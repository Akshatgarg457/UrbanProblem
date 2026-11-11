package com.urbanproblems.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.urbanproblems.domain.entity.Resolution;
import com.urbanproblems.domain.entity.Resolution.ReviewStatus;

@Repository
public interface ResolutionRepository extends JpaRepository<Resolution, Long> {
	Optional<Resolution> findByAssignmentId(Long assignmentId);

	Page<Resolution> findByReviewStatus(ReviewStatus status, Pageable pageable);

	Page<Resolution> findByReviewStatusAndComplaintDepartmentId(ReviewStatus status, Long departmentId,
			Pageable pageable);

	Page<Resolution> findByComplaintIdAndReviewStatus(Long complaintId, ReviewStatus status, Pageable pageable);

	long countByReviewStatus(ReviewStatus status);
}
