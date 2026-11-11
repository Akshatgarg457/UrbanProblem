package com.urbanproblems.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.urbanproblems.domain.entity.AdminUser;
import com.urbanproblems.domain.entity.Citizen;
import com.urbanproblems.domain.entity.Worker;
import com.urbanproblems.dto.ChangePasswordRequestDto;
import com.urbanproblems.exception.ResourceNotFoundException;
import com.urbanproblems.repository.AdminUserRepository;
import com.urbanproblems.repository.CitizenRepository;
import com.urbanproblems.repository.WorkerRepository;
import com.urbanproblems.security.UserPrincipal;

@Service
@Transactional
public class AccountService {

	private final CitizenRepository citizenRepository;
	private final WorkerRepository workerRepository;
	private final AdminUserRepository adminUserRepository;
	private final PasswordEncoder passwordEncoder;

	public AccountService(CitizenRepository citizenRepository, WorkerRepository workerRepository,
			AdminUserRepository adminUserRepository, PasswordEncoder passwordEncoder) {
		this.citizenRepository = citizenRepository;
		this.workerRepository = workerRepository;
		this.adminUserRepository = adminUserRepository;
		this.passwordEncoder = passwordEncoder;
	}

	public void changePassword(UserPrincipal principal, ChangePasswordRequestDto request) {
		String newPassword = request.newPassword();
		switch (principal.getRole()) {
		case "CITIZEN" -> {
			Citizen citizen = citizenRepository.findById(principal.getId())
					.orElseThrow(() -> ResourceNotFoundException.forResource("Citizen", principal.getId().toString()));
			updatePassword(citizen.getPasswordHash(), request.currentPassword(), newPassword,
					citizen::setPasswordHash, () -> citizenRepository.save(citizen));
		}
		case "WORKER" -> {
			Worker worker = workerRepository.findById(principal.getId())
					.orElseThrow(() -> ResourceNotFoundException.forResource("Worker", principal.getId().toString()));
			updatePassword(worker.getPasswordHash(), request.currentPassword(), newPassword,
					worker::setPasswordHash, () -> workerRepository.save(worker));
		}
		case "DEPARTMENT_ADMIN", "SYSTEM_ADMIN" -> {
			AdminUser admin = adminUserRepository.findById(principal.getId())
					.orElseThrow(() -> ResourceNotFoundException.forResource("Administrator", principal.getId().toString()));
			updatePassword(admin.getPasswordHash(), request.currentPassword(), newPassword,
					admin::setPasswordHash, () -> adminUserRepository.save(admin));
		}
		default -> throw new IllegalStateException("Unsupported account role.");
		}
	}

	private void updatePassword(String currentHash, String currentPassword, String newPassword,
			java.util.function.Consumer<String> setPasswordHash, Runnable save) {
		if (!passwordEncoder.matches(currentPassword, currentHash)) {
			throw new IllegalArgumentException("Current password is incorrect.");
		}
		if (passwordEncoder.matches(newPassword, currentHash)) {
			throw new IllegalArgumentException("New password must be different from the current password.");
		}
		setPasswordHash.accept(passwordEncoder.encode(newPassword));
		save.run();
	}
}
