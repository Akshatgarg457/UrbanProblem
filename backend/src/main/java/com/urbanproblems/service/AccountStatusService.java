package com.urbanproblems.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.urbanproblems.domain.enums.UserStatus;
import com.urbanproblems.repository.AdminUserRepository;
import com.urbanproblems.repository.CitizenRepository;
import com.urbanproblems.repository.WorkerRepository;

/**
 * Re-checks account status on every authenticated request. A JWT is only a snapshot of the
 * account at sign-in time, so a worker deactivated after signing in must lose access immediately.
 */
@Service
@Transactional(readOnly = true)
public class AccountStatusService {

	private final CitizenRepository citizenRepository;
	private final WorkerRepository workerRepository;
	private final AdminUserRepository adminUserRepository;

	public AccountStatusService(CitizenRepository citizenRepository, WorkerRepository workerRepository,
			AdminUserRepository adminUserRepository) {
		this.citizenRepository = citizenRepository;
		this.workerRepository = workerRepository;
		this.adminUserRepository = adminUserRepository;
	}

	public boolean isActive(String role, Long id) {
		if (role == null || id == null) {
			return false;
		}
		return switch (role) {
			case "CITIZEN" -> citizenRepository.findById(id)
					.map(c -> c.getStatus() == UserStatus.ACTIVE).orElse(false);
			case "WORKER" -> workerRepository.findById(id)
					.map(w -> w.getStatus() == UserStatus.ACTIVE).orElse(false);
			case "DEPARTMENT_ADMIN", "SYSTEM_ADMIN" -> adminUserRepository.findById(id)
					.map(a -> a.getStatus() == UserStatus.ACTIVE).orElse(false);
			default -> false;
		};
	}
}
