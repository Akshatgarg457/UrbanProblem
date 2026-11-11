package com.urbanproblems.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.urbanproblems.domain.entity.RecurrenceReport;

@Repository
public interface RecurrenceReportRepository extends JpaRepository<RecurrenceReport, Long> {
	Optional<RecurrenceReport> findByCurrentComplaintId(Long complaintId);

	Page<RecurrenceReport> findByCurrentComplaintDepartmentId(Long departmentId, Pageable pageable);
}
