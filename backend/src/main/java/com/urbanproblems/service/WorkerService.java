package com.urbanproblems.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.urbanproblems.domain.entity.Department;
import com.urbanproblems.domain.entity.Worker;
import com.urbanproblems.domain.enums.AssignmentStatus;
import com.urbanproblems.domain.enums.DepartmentStatus;
import com.urbanproblems.domain.enums.UserStatus;
import com.urbanproblems.dto.WorkerProfileResponseDto;
import com.urbanproblems.dto.WorkerProfileUpdateRequestDto;
import com.urbanproblems.dto.WorkerRegistrationDto;
import com.urbanproblems.dto.WorkerResponseDto;
import com.urbanproblems.exception.ResourceNotFoundException;
import com.urbanproblems.repository.AdminUserRepository;
import com.urbanproblems.repository.AssignmentRepository;
import com.urbanproblems.repository.DepartmentRepository;
import com.urbanproblems.repository.WorkerRepository;
import com.urbanproblems.security.AccessRules;
import com.urbanproblems.security.UserPrincipal;

@Service
@Transactional
public class WorkerService {

	private final WorkerRepository workerRepository;
	private final DepartmentRepository departmentRepository;
	private final AssignmentRepository assignmentRepository;
	private final AdminUserRepository adminUserRepository;
	private final PasswordEncoder passwordEncoder;

	public WorkerService(WorkerRepository workerRepository,
			DepartmentRepository departmentRepository,
			AssignmentRepository assignmentRepository,
			AdminUserRepository adminUserRepository,
			PasswordEncoder passwordEncoder) {
		this.workerRepository = workerRepository;
		this.departmentRepository = departmentRepository;
		this.assignmentRepository = assignmentRepository;
		this.adminUserRepository = adminUserRepository;
		this.passwordEncoder = passwordEncoder;
	}

	/** Administrator-provisioned worker: active immediately. */
	public WorkerResponseDto registerWorker(WorkerRegistrationDto dto) {
		Worker worker = createWorker(dto, UserStatus.ACTIVE);
		return mapToResponseDto(worker);
	}

	/**
	 * Public worker application. The account is created INACTIVE so it cannot authenticate until a
	 * system administrator approves it.
	 */
	public WorkerResponseDto applyAsWorker(WorkerRegistrationDto dto) {
		Worker worker = createWorker(dto, UserStatus.INACTIVE);
		return mapToResponseDto(worker);
	}

	private Worker createWorker(WorkerRegistrationDto dto, UserStatus status) {
		String email = dto.getEmail().trim().toLowerCase();
		if (workerRepository.existsByEmail(email) || adminUserRepository.existsByEmail(email)) {
			throw new IllegalArgumentException("Email already registered");
		}
		Department department = departmentRepository.findById(dto.getDepartmentId())
				.orElseThrow(() -> new ResourceNotFoundException("Department not found"));
		if (department.getStatus() != DepartmentStatus.ACTIVE) {
			throw new IllegalStateException("That department is not currently accepting worker applications.");
		}

		return workerRepository.save(Worker.builder()
				.email(email)
				.firstName(dto.getFirstName())
				.lastName(dto.getLastName())
				.phoneNumber(dto.getPhoneNumber())
				.passwordHash(passwordEncoder.encode(dto.getPassword()))
				.securityQuestion(dto.getSecurityQuestion().trim())
				.securityAnswerHash(passwordEncoder.encode(normalizeAnswer(dto.getSecurityAnswer())))
				.department(department)
				.status(status)
				.build());
	}

	@Transactional(readOnly = true)
	public Page<WorkerResponseDto> getPendingApplications(Pageable pageable) {
		return workerRepository.findByStatus(UserStatus.INACTIVE, pageable).map(this::mapToResponseDto);
	}

	public WorkerResponseDto approveWorker(Long id) {
		Worker worker = findWorker(id);
		if (worker.getStatus() != UserStatus.INACTIVE) {
			throw new IllegalStateException("Only a pending worker application can be approved.");
		}
		worker.setStatus(UserStatus.ACTIVE);
		return mapToResponseDto(workerRepository.save(worker));
	}

	public WorkerResponseDto rejectWorker(Long id) {
		Worker worker = findWorker(id);
		if (worker.getStatus() == UserStatus.ACTIVE) {
			throw new IllegalStateException("Only a pending worker application can be rejected.");
		}
		// SUSPENDED distinguishes a rejected application from one still awaiting review (INACTIVE).
		worker.setStatus(UserStatus.SUSPENDED);
		return mapToResponseDto(workerRepository.save(worker));
	}

	/** A worker may read their own record. Department admins may read workers in their department. */
	@Transactional(readOnly = true)
	public WorkerResponseDto getWorker(Long id, UserPrincipal user) {
		Worker worker = findWorker(id);
		if (AccessRules.WORKER.equals(user.getRole())) {
			if (!worker.getId().equals(user.getId())) {
				throw new AccessDeniedException("Access denied");
			}
		} else {
			AccessRules.requireDepartmentScope(user, worker.getDepartment());
		}
		return mapToResponseDto(worker);
	}

	/** Self-service profile read: the caller's own worker record only. */
	@Transactional(readOnly = true)
	public WorkerProfileResponseDto getMyProfile(Long workerId) {
		return mapToProfileDto(findWorker(workerId));
	}

	/** Self-service profile update: name, phone and saved location. */
	public WorkerProfileResponseDto updateMyProfile(Long workerId, WorkerProfileUpdateRequestDto dto) {
		Worker worker = findWorker(workerId);
		worker.setFirstName(dto.firstName().trim());
		worker.setLastName(dto.lastName().trim());
		worker.setPhoneNumber(dto.phoneNumber().trim());
		if ((dto.latitude() == null) != (dto.longitude() == null)) {
			throw new IllegalArgumentException("Latitude and longitude must be provided together.");
		}
		worker.setAddress(blankToNull(dto.address()));
		worker.setCity(blankToNull(dto.city()));
		worker.setState(blankToNull(dto.state()));
		worker.setPostalCode(blankToNull(dto.postalCode()));
		worker.setLatitude(dto.latitude());
		worker.setLongitude(dto.longitude());
		return mapToProfileDto(workerRepository.save(worker));
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	private static String normalizeAnswer(String answer) {
		return answer.trim().toLowerCase(java.util.Locale.ROOT);
	}

	@Transactional(readOnly = true)
	public Page<WorkerResponseDto> getWorkersByDepartment(Long departmentId, boolean activeOnly, UserPrincipal user,
			Pageable pageable) {
		Department department = departmentRepository.findById(departmentId)
				.orElseThrow(() -> new ResourceNotFoundException("Department not found"));
		AccessRules.requireDepartmentScope(user, department);
		Page<Worker> page = activeOnly
				? workerRepository.findByDepartmentIdAndStatus(departmentId, UserStatus.ACTIVE, pageable)
				: workerRepository.findByDepartmentId(departmentId, pageable);
		return page.map(this::mapToResponseDto);
	}

	public WorkerResponseDto updateWorkerStatus(Long id, UserStatus status) {
		Worker worker = findWorker(id);
		worker.setStatus(status);
		return mapToResponseDto(workerRepository.save(worker));
	}

	/** Blocked while the worker still holds open assignments. */
	public WorkerResponseDto changeWorkerDepartment(Long workerId, Long newDepartmentId) {
		Worker worker = findWorker(workerId);
		Department newDepartment = departmentRepository.findById(newDepartmentId)
				.orElseThrow(() -> new ResourceNotFoundException("Department not found"));
		long open = assignmentRepository.countByAssignedWorkerIdAndStatusIn(workerId,
				java.util.List.of(AssignmentStatus.ASSIGNED, AssignmentStatus.IN_PROGRESS,
						AssignmentStatus.SUBMITTED_FOR_REVIEW));
		if (open > 0) {
			throw new IllegalStateException("Cannot transfer a worker with open assignments.");
		}
		worker.setDepartment(newDepartment);
		return mapToResponseDto(workerRepository.save(worker));
	}

	private Worker findWorker(Long id) {
		return workerRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Worker not found"));
	}

	WorkerResponseDto mapToResponseDto(Worker worker) {
		return WorkerResponseDto.builder()
				.id(worker.getId())
				.email(worker.getEmail())
				.firstName(worker.getFirstName())
				.lastName(worker.getLastName())
				.phoneNumber(worker.getPhoneNumber())
				.departmentId(worker.getDepartment().getId())
				.departmentName(worker.getDepartment().getName())
				.status(worker.getStatus())
				.activeAssignmentCount(assignmentRepository.countByAssignedWorkerIdAndStatus(
						worker.getId(), AssignmentStatus.ASSIGNED))
				.createdAt(worker.getCreatedAt())
				.updatedAt(worker.getUpdatedAt())
				.build();
	}

	private WorkerProfileResponseDto mapToProfileDto(Worker worker) {
		return WorkerProfileResponseDto.builder()
				.id(worker.getId())
				.email(worker.getEmail())
				.firstName(worker.getFirstName())
				.lastName(worker.getLastName())
				.phoneNumber(worker.getPhoneNumber())
				.address(worker.getAddress())
				.city(worker.getCity())
				.state(worker.getState())
				.postalCode(worker.getPostalCode())
				.latitude(worker.getLatitude())
				.longitude(worker.getLongitude())
				.departmentId(worker.getDepartment().getId())
				.departmentName(worker.getDepartment().getName())
				.status(worker.getStatus())
				.createdAt(worker.getCreatedAt())
				.updatedAt(worker.getUpdatedAt())
				.build();
	}
}
