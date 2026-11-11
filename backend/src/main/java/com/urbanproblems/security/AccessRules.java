package com.urbanproblems.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.urbanproblems.domain.entity.Complaint;
import com.urbanproblems.domain.entity.Department;
import com.urbanproblems.repository.AssignmentRepository;

/**
 * Central object-level authorization rules. Services call these before returning or mutating any
 * complaint-related data, so access control does not depend on the controller layer.
 *
 * Identity is the pair (role, id). Ids are only unique within a user table, so every comparison
 * must also check the role claim.
 */
public final class AccessRules {

	public static final String CITIZEN = "CITIZEN";
	public static final String WORKER = "WORKER";
	public static final String DEPARTMENT_ADMIN = "DEPARTMENT_ADMIN";
	public static final String SYSTEM_ADMIN = "SYSTEM_ADMIN";

	private AccessRules() {
	}

	public static UserPrincipal currentPrincipal() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
			throw new AccessDeniedException("Authentication required");
		}
		return principal;
	}

	/** Citizen owner, assigned worker, department admin of the complaint's department, or system admin. */
	public static void requireComplaintVisible(UserPrincipal user, Complaint complaint,
			AssignmentRepository assignments) {
		switch (user.getRole()) {
		case SYSTEM_ADMIN -> {
			return;
		}
		case DEPARTMENT_ADMIN -> {
			if (sameDepartment(user, complaint.getDepartment())) {
				return;
			}
		}
		case CITIZEN -> {
			if (complaint.getCitizen() != null && complaint.getCitizen().getId().equals(user.getId())) {
				return;
			}
		}
		case WORKER -> {
			if (assignments.existsByComplaintIdAndAssignedWorkerId(complaint.getId(), user.getId())) {
				return;
			}
		}
		default -> {
		}
		}
		throw new AccessDeniedException("Access denied");
	}

	/** Department admin of the given department, or system admin. */
	public static void requireDepartmentScope(UserPrincipal user, Department department) {
		if (SYSTEM_ADMIN.equals(user.getRole()) || (DEPARTMENT_ADMIN.equals(user.getRole())
				&& sameDepartment(user, department))) {
			return;
		}
		throw new AccessDeniedException("Access denied");
	}

	/** Department admin for the department, or system admin across all departments. */
	public static void requireDepartmentAdmin(UserPrincipal user, Department department) {
		if (SYSTEM_ADMIN.equals(user.getRole())
				|| (DEPARTMENT_ADMIN.equals(user.getRole()) && sameDepartment(user, department))) {
			return;
		}
		throw new AccessDeniedException("Access denied");
	}

	private static boolean sameDepartment(UserPrincipal user, Department department) {
		return user.getDepartmentId() != null && department != null
				&& user.getDepartmentId().equals(department.getId());
	}
}
