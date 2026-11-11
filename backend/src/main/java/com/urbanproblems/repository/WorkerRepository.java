package com.urbanproblems.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.urbanproblems.domain.entity.Worker;
import com.urbanproblems.domain.enums.UserStatus;

@Repository
public interface WorkerRepository extends JpaRepository<Worker, Long> {
	Optional<Worker> findByEmail(String email);

	/** Candidate accounts for a username (email local part) lookup. */
	List<Worker> findByEmailStartingWithIgnoreCase(String prefix);

	boolean existsByEmail(String email);

	Page<Worker> findByDepartmentId(Long departmentId, Pageable pageable);

	Page<Worker> findByDepartmentIdAndStatus(Long departmentId, UserStatus status, Pageable pageable);

	Page<Worker> findByStatus(UserStatus status, Pageable pageable);

	long countByDepartmentId(Long departmentId);
}
