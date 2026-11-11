package com.urbanproblems.repository;
import com.urbanproblems.domain.entity.RecurrenceReport; import org.springframework.data.domain.*; import java.util.Optional;
public interface RecurrenceReportRepository { Optional<RecurrenceReport> findByCurrentComplaintId(Long id); RecurrenceReport save(RecurrenceReport r); Optional<RecurrenceReport> findById(Long id);
  Page<RecurrenceReport> findAll(Pageable p); java.util.List<RecurrenceReport> findAll(); Page<RecurrenceReport> findByCurrentComplaintDepartmentId(Long d, Pageable p); }
