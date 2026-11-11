package com.urbanproblems.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.urbanproblems.domain.entity.AdminUser;
import com.urbanproblems.domain.enums.UserRole;

@Repository
public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {
	Optional<AdminUser> findByEmail(String email);

	/** Candidate accounts for a username (email local part) lookup. */
	List<AdminUser> findByEmailStartingWithIgnoreCase(String prefix);

	boolean existsByEmail(String email);

	boolean existsByRole(UserRole role);

	List<AdminUser> findByRole(UserRole role);

	List<AdminUser> findByDepartmentIdAndRole(Long departmentId, UserRole role);
}
