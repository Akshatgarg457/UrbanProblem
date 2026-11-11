package com.urbanproblems.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.urbanproblems.domain.entity.ComplaintEvidence;

import java.util.List;

@Repository
public interface ComplaintEvidenceRepository extends JpaRepository<ComplaintEvidence, Long> {

	List<ComplaintEvidence> findByComplaintId(Long complaintId);

	long countByComplaintId(Long complaintId);

	boolean existsByComplaintIdAndUploadedByAndUploadedById(Long complaintId, String uploadedBy, Long uploadedById);
}
