package com.urbanproblems.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.urbanproblems.domain.entity.AdminUser;
import com.urbanproblems.domain.entity.Citizen;
import com.urbanproblems.domain.entity.Worker;
import com.urbanproblems.dto.ChangePasswordRequestDto;
import com.urbanproblems.repository.AdminUserRepository;
import com.urbanproblems.repository.CitizenRepository;
import com.urbanproblems.repository.WorkerRepository;
import com.urbanproblems.security.UserPrincipal;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

	@Mock
	private CitizenRepository citizenRepository;
	@Mock
	private WorkerRepository workerRepository;
	@Mock
	private AdminUserRepository adminUserRepository;
	@Mock
	private PasswordEncoder passwordEncoder;
	@InjectMocks
	private AccountService accountService;

	@Test
	void changesCitizenPasswordAfterVerifyingCurrentPassword() {
		Citizen citizen = Citizen.builder().id(1L).passwordHash("old-hash").build();
		when(citizenRepository.findById(1L)).thenReturn(Optional.of(citizen));
		when(passwordEncoder.matches("current-password", "old-hash")).thenReturn(true);
		when(passwordEncoder.matches("new-password", "old-hash")).thenReturn(false);
		when(passwordEncoder.encode("new-password")).thenReturn("new-hash");

		accountService.changePassword(principal("CITIZEN"), request());

		assertEquals("new-hash", citizen.getPasswordHash());
		verify(citizenRepository).save(citizen);
	}

	@Test
	void changesWorkerPassword() {
		Worker worker = Worker.builder().id(1L).passwordHash("old-hash").build();
		when(workerRepository.findById(1L)).thenReturn(Optional.of(worker));
		when(passwordEncoder.matches("current-password", "old-hash")).thenReturn(true);
		when(passwordEncoder.matches("new-password", "old-hash")).thenReturn(false);
		when(passwordEncoder.encode("new-password")).thenReturn("new-hash");

		accountService.changePassword(principal("WORKER"), request());

		assertEquals("new-hash", worker.getPasswordHash());
		verify(workerRepository).save(worker);
	}

	@Test
	void changesAdministratorPassword() {
		AdminUser admin = AdminUser.builder().id(1L).passwordHash("old-hash").build();
		when(adminUserRepository.findById(1L)).thenReturn(Optional.of(admin));
		when(passwordEncoder.matches("current-password", "old-hash")).thenReturn(true);
		when(passwordEncoder.matches("new-password", "old-hash")).thenReturn(false);
		when(passwordEncoder.encode("new-password")).thenReturn("new-hash");

		accountService.changePassword(principal("SYSTEM_ADMIN"), request());

		assertEquals("new-hash", admin.getPasswordHash());
		verify(adminUserRepository).save(admin);
	}

	@Test
	void rejectsAnIncorrectCurrentPassword() {
		Citizen citizen = Citizen.builder().id(1L).passwordHash("old-hash").build();
		when(citizenRepository.findById(1L)).thenReturn(Optional.of(citizen));
		when(passwordEncoder.matches("current-password", "old-hash")).thenReturn(false);

		assertThrows(IllegalArgumentException.class,
				() -> accountService.changePassword(principal("CITIZEN"), request()));
	}

	private static UserPrincipal principal(String role) {
		return new UserPrincipal(1L, "user@example.com", "old-hash", role, role, null, true);
	}

	private static ChangePasswordRequestDto request() {
		return new ChangePasswordRequestDto("current-password", "new-password");
	}
}
