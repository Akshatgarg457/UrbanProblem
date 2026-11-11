package com.urbanproblems.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.urbanproblems.domain.entity.Complaint;
import com.urbanproblems.domain.enums.ComplaintCategory;
import com.urbanproblems.domain.enums.ComplaintStatus;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
	Optional<Complaint> findByPublicId(String publicId);

	boolean existsByPublicId(String publicId);

	Page<Complaint> findByCitizenId(Long citizenId, Pageable pageable);

	Page<Complaint> findByDepartmentId(Long departmentId, Pageable pageable);

	Page<Complaint> findByStatus(ComplaintStatus status, Pageable pageable);

	Page<Complaint> findByCategory(ComplaintCategory category, Pageable pageable);

	Page<Complaint> findByDepartmentIdAndStatus(Long departmentId, ComplaintStatus status, Pageable pageable);

	boolean existsByCitizenIdAndTitleIgnoreCaseAndLocationIgnoreCaseAndCreatedAtGreaterThanEqualAndStatusIn(
			Long citizenId, String title, String location, Instant since,
			java.util.Collection<ComplaintStatus> statuses);

	Page<Complaint> findByCategoryAndStatus(ComplaintCategory category, ComplaintStatus status, Pageable pageable);

	List<Complaint> findByCategoryAndStatusAndCreatedAtGreaterThanEqual(ComplaintCategory category,
			ComplaintStatus status, Instant since);

	@Query("SELECT c FROM Complaint c WHERE c.createdAt >= :since AND c.status = :status")
	List<Complaint> findCreatedComplaintsCreatedSince(
			@Param("since") Instant since,
			@Param("status") ComplaintStatus status);

	long countByStatus(ComplaintStatus status);

	long countByCategory(ComplaintCategory category);

	long countByCategoryAndDepartmentId(ComplaintCategory category, Long departmentId);

	long countByDepartmentId(Long departmentId);

	long countByDepartmentIdAndStatus(Long departmentId, ComplaintStatus status);

	long countByDepartmentIdAndStatusIn(Long departmentId, java.util.Collection<ComplaintStatus> statuses);
}
