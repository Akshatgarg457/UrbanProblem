package com.urbanproblems.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.urbanproblems.domain.entity.AdminUser;
import com.urbanproblems.domain.entity.Department;
import com.urbanproblems.domain.enums.UserRole;
import com.urbanproblems.domain.enums.UserStatus;
import com.urbanproblems.dto.AdminProfileResponseDto;
import com.urbanproblems.dto.AdminProfileUpdateRequestDto;
import com.urbanproblems.dto.AdminRegistrationDto;
import com.urbanproblems.dto.AdminResponseDto;
import com.urbanproblems.exception.ResourceNotFoundException;
import com.urbanproblems.repository.AdminUserRepository;
import com.urbanproblems.repository.CitizenRepository;
import com.urbanproblems.repository.DepartmentRepository;
import com.urbanproblems.repository.WorkerRepository;

/** System-admin operations on administrator accounts. Callers must be SYSTEM_ADMIN (enforced in controller). */
@Service
@Transactional
public class AdminService {

	private final AdminUserRepository adminUserRepository;
	private final DepartmentRepository departmentRepository;
	private final CitizenRepository citizenRepository;
	private final WorkerRepository workerRepository;
	private final PasswordEncoder passwordEncoder;

	public AdminService(AdminUserRepository adminUserRepository, DepartmentRepository departmentRepository,
			CitizenRepository citizenRepository, WorkerRepository workerRepository,
			PasswordEncoder passwordEncoder) {
		this.adminUserRepository = adminUserRepository;
		this.departmentRepository = departmentRepository;
		this.citizenRepository = citizenRepository;
		this.workerRepository = workerRepository;
		this.passwordEncoder = passwordEncoder;
	}

	public AdminResponseDto createDepartmentAdmin(AdminRegistrationDto dto) {
		requireEmailFree(dto.getEmail());
		Department department = departmentRepository.findById(dto.getDepartmentId())
				.orElseThrow(() -> new ResourceNotFoundException("Department not found"));

		AdminUser admin = adminUserRepository.save(AdminUser.builder()
				.email(dto.getEmail().trim().toLowerCase())
				.firstName(dto.getFirstName())
				.lastName(dto.getLastName())
				.passwordHash(passwordEncoder.encode(dto.getPassword()))
				.role(UserRole.DEPARTMENT_ADMIN)
				.department(department)
				.status(UserStatus.ACTIVE)
				.build());
		return map(admin);
	}

	/** Administrator emails cannot overlap with another administrator, citizen, or worker account. */
	void requireEmailFree(String email) {
		String normalized = email.trim().toLowerCase();
		if (citizenRepository.existsByEmail(normalized) || workerRepository.existsByEmail(normalized)
				|| adminUserRepository.existsByEmail(normalized)) {
			throw new IllegalArgumentException("Email already registered");
		}
	}

	/** Self-service profile read for DEPARTMENT_ADMIN and SYSTEM_ADMIN, resolved from the principal. */
	@Transactional(readOnly = true)
	public AdminProfileResponseDto getMyProfile(com.urbanproblems.security.UserPrincipal principal) {
		requireAdminRole(principal);
		return mapProfile(findAdmin(principal.getId()));
	}

	/** Self-service profile update: name only. Role and department are never editable here. */
	public AdminProfileResponseDto updateMyProfile(com.urbanproblems.security.UserPrincipal principal,
			AdminProfileUpdateRequestDto dto) {
		requireAdminRole(principal);
		AdminUser admin = findAdmin(principal.getId());
		admin.setFirstName(dto.firstName().trim());
		admin.setLastName(dto.lastName().trim());
		return mapProfile(adminUserRepository.save(admin));
	}

	private void requireAdminRole(com.urbanproblems.security.UserPrincipal principal) {
		if (!UserRole.DEPARTMENT_ADMIN.name().equals(principal.getRole())
				&& !UserRole.SYSTEM_ADMIN.name().equals(principal.getRole())) {
			throw new org.springframework.security.access.AccessDeniedException("Access denied");
		}
	}

	private AdminUser findAdmin(Long id) {
		return adminUserRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Administrator not found"));
	}

	private static AdminProfileResponseDto mapProfile(AdminUser admin) {
		return new AdminProfileResponseDto(admin.getId(), admin.getEmail(), admin.getFirstName(), admin.getLastName(),
				admin.getRole().name(),
				admin.getDepartment() == null ? null : admin.getDepartment().getId(),
				admin.getDepartment() == null ? null : admin.getDepartment().getName(),
				admin.getStatus().name(), admin.getCreatedAt());
	}

	@Transactional(readOnly = true)
	public java.util.List<com.urbanproblems.dto.UserSummaryDto> getAllUsers(String roleFilter) {
		java.util.List<com.urbanproblems.dto.UserSummaryDto> users = new java.util.ArrayList<>();

		if (roleFilter == null || roleFilter.isBlank() || "SYSTEM_ADMIN".equalsIgnoreCase(roleFilter) || "DEPARTMENT_ADMIN".equalsIgnoreCase(roleFilter)) {
			adminUserRepository.findAll().forEach(a -> {
				if (roleFilter == null || roleFilter.isBlank() || a.getRole().name().equalsIgnoreCase(roleFilter)) {
					users.add(new com.urbanproblems.dto.UserSummaryDto(
							a.getId(), a.getEmail(), a.getFirstName(), a.getLastName(),
							null, a.getRole().name(),
							a.getDepartment() != null ? a.getDepartment().getId() : null,
							a.getDepartment() != null ? a.getDepartment().getName() : null,
							a.getStatus() == UserStatus.ACTIVE, a.getCreatedAt()));
				}
			});
		}

		if (roleFilter == null || roleFilter.isBlank() || "WORKER".equalsIgnoreCase(roleFilter)) {
			workerRepository.findAll().forEach(w -> {
				users.add(new com.urbanproblems.dto.UserSummaryDto(
						w.getId(), w.getEmail(), w.getFirstName(), w.getLastName(),
						w.getPhoneNumber(), "WORKER",
						w.getDepartment() != null ? w.getDepartment().getId() : null,
						w.getDepartment() != null ? w.getDepartment().getName() : null,
						w.getStatus() == UserStatus.ACTIVE, w.getCreatedAt()));
			});
		}

		if (roleFilter == null || roleFilter.isBlank() || "CITIZEN".equalsIgnoreCase(roleFilter)) {
			citizenRepository.findAll().forEach(c -> {
				users.add(new com.urbanproblems.dto.UserSummaryDto(
						c.getId(), c.getEmail(), c.getFirstName(), c.getLastName(),
						c.getPhoneNumber(), "CITIZEN",
						null, null,
						c.getStatus() == UserStatus.ACTIVE, c.getCreatedAt()));
			});
		}

		return users;
	}

	static AdminResponseDto map(AdminUser admin) {
		return new AdminResponseDto(admin.getId(), admin.getEmail(), admin.getFirstName(), admin.getLastName(),
				admin.getRole().name(),
				admin.getDepartment() == null ? null : admin.getDepartment().getId(),
				admin.getStatus().name(), admin.getCreatedAt());
	}

	static List<AdminResponseDto> mapAll(List<AdminUser> admins) {
		return admins.stream().map(AdminService::map).toList();
	}
}
