package com.urbanproblems.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;

import com.urbanproblems.domain.entity.AdminUser;
import com.urbanproblems.domain.entity.Citizen;
import com.urbanproblems.domain.entity.Worker;
import com.urbanproblems.domain.enums.UserStatus;
import com.urbanproblems.repository.AdminUserRepository;
import com.urbanproblems.repository.CitizenRepository;
import com.urbanproblems.repository.WorkerRepository;
import com.urbanproblems.security.UserPrincipal;

/**
 * Resolves an email plus optional workspace to a citizen, worker, or administrator. A person may
 * have both a citizen and worker account with the same email, but each workspace selects its own
 * independent account and password.
 */
@Service
@Transactional
public class CustomUserDetailsService implements UserDetailsService {

	private final CitizenRepository citizenRepository;
	private final WorkerRepository workerRepository;
	private final AdminUserRepository adminUserRepository;

	public CustomUserDetailsService(CitizenRepository citizenRepository, WorkerRepository workerRepository,
			AdminUserRepository adminUserRepository) {
		this.citizenRepository = citizenRepository;
		this.workerRepository = workerRepository;
		this.adminUserRepository = adminUserRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		int separator = username.lastIndexOf('|');
		if (separator > 0) {
			String workspace = username.substring(0, separator).toUpperCase(Locale.ROOT);
			String email = username.substring(separator + 1);
			return switch (workspace) {
				case "CITIZEN" -> findCitizen(email);
				case "WORKER" -> findWorker(email);
				case "ADMIN" -> findAdmin(email);
				default -> throw new UsernameNotFoundException("User not found");
			};
		}

		String email = username;
		Citizen citizen = citizenRepository.findByEmail(email).orElse(null);
		if (citizen != null) return principal(citizen);

		Worker worker = workerRepository.findByEmail(email).orElse(null);
		if (worker != null) return principal(worker);

		AdminUser admin = adminUserRepository.findByEmail(email).orElse(null);
		if (admin != null) return principal(admin);

		throw new UsernameNotFoundException("User not found");
	}

	private UserPrincipal findCitizen(String email) {
		return citizenRepository.findByEmail(email).map(this::principal)
				.orElseThrow(() -> new UsernameNotFoundException("User not found"));
	}

	private UserPrincipal findWorker(String email) {
		return workerRepository.findByEmail(email).map(this::principal)
				.orElseThrow(() -> new UsernameNotFoundException("User not found"));
	}

	private UserPrincipal findAdmin(String email) {
		return adminUserRepository.findByEmail(email).map(this::principal)
				.orElseThrow(() -> new UsernameNotFoundException("User not found"));
	}

	private UserPrincipal principal(Citizen citizen) {
		return new UserPrincipal(citizen.getId(), citizen.getEmail(), citizen.getPasswordHash(),
				"CITIZEN", "CITIZEN", null, citizen.getStatus() == UserStatus.ACTIVE);
	}

	private UserPrincipal principal(Worker worker) {
		return new UserPrincipal(worker.getId(), worker.getEmail(), worker.getPasswordHash(),
				"WORKER", "WORKER", worker.getDepartment().getId(), worker.getStatus() == UserStatus.ACTIVE);
	}

	private UserPrincipal principal(AdminUser admin) {
		Long departmentId = admin.getDepartment() == null ? null : admin.getDepartment().getId();
		return new UserPrincipal(admin.getId(), admin.getEmail(), admin.getPasswordHash(),
				admin.getRole().name(), null, departmentId, admin.getStatus() == UserStatus.ACTIVE);
	}
}
