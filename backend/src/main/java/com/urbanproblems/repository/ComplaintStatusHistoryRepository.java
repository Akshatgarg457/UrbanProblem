package com.urbanproblems.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.urbanproblems.domain.entity.ComplaintStatusHistory;

@Repository
public interface ComplaintStatusHistoryRepository extends JpaRepository<ComplaintStatusHistory, Long> {
	List<ComplaintStatusHistory> findByComplaintIdOrderByChangedAtDesc(Long complaintId);
}
