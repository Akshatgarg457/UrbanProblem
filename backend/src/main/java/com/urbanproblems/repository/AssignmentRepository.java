package com.urbanproblems.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.urbanproblems.domain.entity.Assignment;
import com.urbanproblems.domain.enums.AssignmentStatus;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
	Optional<Assignment> findByComplaintId(Long complaintId);

	boolean existsByComplaintIdAndAssignedWorkerId(Long complaintId, Long workerId);

	Page<Assignment> findByAssignedWorkerId(Long workerId, Pageable pageable);

	Page<Assignment> findByAssignedWorkerIdAndStatus(Long workerId, AssignmentStatus status, Pageable pageable);

	List<Assignment> findByAssignedWorkerIdAndStatus(Long workerId, AssignmentStatus status);

	Page<Assignment> findByStatus(AssignmentStatus status, Pageable pageable);

	long countByAssignedWorkerIdAndStatus(Long workerId, AssignmentStatus status);

	long countByAssignedWorkerIdAndStatusIn(Long workerId, List<AssignmentStatus> statuses);
}
