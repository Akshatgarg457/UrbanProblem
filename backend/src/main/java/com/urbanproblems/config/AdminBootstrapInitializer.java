package com.urbanproblems.config;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.urbanproblems.domain.entity.AdminUser;
import com.urbanproblems.domain.enums.UserRole;
import com.urbanproblems.domain.enums.UserStatus;
import com.urbanproblems.repository.AdminUserRepository;
import com.urbanproblems.repository.CitizenRepository;
import com.urbanproblems.repository.WorkerRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Bootstraps the first system admin, or performs an explicitly requested one-time replacement.
 * The replacement mode is intended for local provisioning and must not remain enabled afterward.
 */
@Slf4j
@Component
@Profile("!test")
@Order(Ordered.LOWEST_PRECEDENCE)
public class AdminBootstrapInitializer implements ApplicationRunner {

	private static final int MIN_PASSWORD_LENGTH = 12;
	private static final int MAX_PASSWORD_LENGTH = 128;
	private static final int MAX_NAME_LENGTH = 50;

	private final AdminUserRepository adminUserRepository;
	private final CitizenRepository citizenRepository;
	private final WorkerRepository workerRepository;
	private final PasswordEncoder passwordEncoder;
	private final TransactionTemplate transactionTemplate;
	private final ConfigurableApplicationContext applicationContext;
	private final String email;
	private final String password;
	private final String firstName;
	private final String lastName;
	private final boolean replaceExisting;
	private final boolean exitAfter;

	public AdminBootstrapInitializer(
			AdminUserRepository adminUserRepository,
			CitizenRepository citizenRepository,
			WorkerRepository workerRepository,
			PasswordEncoder passwordEncoder,
			PlatformTransactionManager transactionManager,
			ConfigurableApplicationContext applicationContext,
			@Value("${app.bootstrap.system-admin.email:}") String email,
			@Value("${app.bootstrap.system-admin.password:}") String password,
			@Value("${app.bootstrap.system-admin.first-name:System}") String firstName,
			@Value("${app.bootstrap.system-admin.last-name:Administrator}") String lastName,
			@Value("${app.bootstrap.system-admin.replace-existing:false}") boolean replaceExisting,
			@Value("${app.bootstrap.exit-after:false}") boolean exitAfter) {
		this.adminUserRepository = adminUserRepository;
		this.citizenRepository = citizenRepository;
		this.workerRepository = workerRepository;
		this.passwordEncoder = passwordEncoder;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
		this.applicationContext = applicationContext;
		this.email = email;
		this.password = password;
		this.firstName = firstName;
		this.lastName = lastName;
		this.replaceExisting = replaceExisting;
		this.exitAfter = exitAfter;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (!replaceExisting && adminUserRepository.existsByRole(UserRole.SYSTEM_ADMIN)) {
			return;
		}

		if (missingProvisioningValues()) {
			if (replaceExisting || exitAfter) {
				throw new IllegalStateException("System-admin provisioning requires an email and password.");
			}
			log.warn("No SYSTEM_ADMIN exists and BOOTSTRAP_SYSTEM_ADMIN_EMAIL/PASSWORD are not set. "
					+ "Set them once to create the first system administrator.");
			return;
		}

		transactionTemplate.executeWithoutResult(status -> provisionSystemAdmin());
		log.info("System administrator provisioning completed.");

		if (exitAfter) {
			SpringApplication.exit(applicationContext);
		}
	}

	private boolean missingProvisioningValues() {
		return email == null || email.isBlank() || password == null || password.isBlank();
	}

	private void provisionSystemAdmin() {
		String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
		if (password.length() < MIN_PASSWORD_LENGTH || password.length() > MAX_PASSWORD_LENGTH) {
			throw new IllegalStateException("The system administrator password must be 12-128 characters.");
		}
		if (firstName == null || firstName.isBlank() || firstName.length() > MAX_NAME_LENGTH
				|| lastName == null || lastName.length() > MAX_NAME_LENGTH) {
			throw new IllegalStateException("Administrator names must be non-empty and at most 50 characters.");
		}
		if (citizenRepository.existsByEmail(normalizedEmail) || workerRepository.existsByEmail(normalizedEmail)) {
			throw new IllegalStateException("The bootstrap email already belongs to a non-admin account.");
		}

		AdminUser target = adminUserRepository.findByEmail(normalizedEmail).orElse(null);
		if (!replaceExisting && target != null) {
			throw new IllegalStateException("The bootstrap email already belongs to an administrator.");
		}

		if (replaceExisting) {
			for (AdminUser admin : adminUserRepository.findAll()) {
				if (target == null || !admin.getId().equals(target.getId())) {
					admin.setStatus(UserStatus.INACTIVE);
					adminUserRepository.save(admin);
				}
			}
		}

		if (target == null) {
			target = AdminUser.builder().email(normalizedEmail).build();
		}
		target.setEmail(normalizedEmail);
		target.setFirstName(firstName.trim());
		target.setLastName(lastName.trim());
		target.setPasswordHash(passwordEncoder.encode(password));
		target.setRole(UserRole.SYSTEM_ADMIN);
		target.setDepartment(null);
		target.setStatus(UserStatus.ACTIVE);
		adminUserRepository.save(target);
	}
}
