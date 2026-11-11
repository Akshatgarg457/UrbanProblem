package com.urbanproblems.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

import com.urbanproblems.domain.entity.AdminUser;
import com.urbanproblems.domain.enums.UserRole;
import com.urbanproblems.domain.enums.UserStatus;
import com.urbanproblems.repository.AdminUserRepository;
import com.urbanproblems.repository.CitizenRepository;
import com.urbanproblems.repository.WorkerRepository;

class AdminBootstrapInitializerTest {

	@Test
	void replacementActivatesNamedSystemAdminAndDeactivatesOtherAdmins() {
		AdminUser goku = AdminUser.builder()
				.id(1L)
				.email("akshat120@gmail.com")
				.firstName("Old")
				.lastName("Admin")
				.passwordHash("old-hash")
				.role(UserRole.DEPARTMENT_ADMIN)
				.status(UserStatus.ACTIVE)
				.build();
		AdminUser previousAdmin = AdminUser.builder()
				.id(2L)
				.email("previous@example.org")
				.firstName("Previous")
				.lastName("Admin")
				.passwordHash("old-hash")
				.role(UserRole.SYSTEM_ADMIN)
				.status(UserStatus.ACTIVE)
				.build();

		AdminUserRepository admins = mock(AdminUserRepository.class);
		CitizenRepository citizens = mock(CitizenRepository.class);
		WorkerRepository workers = mock(WorkerRepository.class);
		PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
		when(transactions.getTransaction(any(TransactionDefinition.class))).thenReturn(new SimpleTransactionStatus());
		when(citizens.existsByEmail("akshat120@gmail.com")).thenReturn(false);
		when(workers.existsByEmail("akshat120@gmail.com")).thenReturn(false);
		when(admins.findByEmail("akshat120@gmail.com")).thenReturn(Optional.of(goku));
		when(admins.findAll()).thenReturn(List.of(goku, previousAdmin));

		String password = "local-provisioning-test-password";
		BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
		AdminBootstrapInitializer initializer = new AdminBootstrapInitializer(
				admins, citizens, workers, encoder, transactions, mock(ConfigurableApplicationContext.class),
				"  AKSHAT120@gmail.com ", password, "Goku", "Admin", true, false);

		initializer.run(new org.springframework.boot.DefaultApplicationArguments(new String[0]));

		assertEquals(UserRole.SYSTEM_ADMIN, goku.getRole());
		assertEquals(UserStatus.ACTIVE, goku.getStatus());
		assertEquals("Goku", goku.getFirstName());
		assertNull(goku.getDepartment());
		assertTrue(encoder.matches(password, goku.getPasswordHash()));
		assertEquals(UserStatus.INACTIVE, previousAdmin.getStatus());
	}
}
