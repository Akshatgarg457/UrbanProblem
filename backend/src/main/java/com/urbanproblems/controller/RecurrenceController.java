package com.urbanproblems.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.urbanproblems.domain.enums.ComplaintCategory;
import com.urbanproblems.dto.RecurrenceReportResponseDto;
import com.urbanproblems.security.AccessRules;
import com.urbanproblems.service.RecurrenceService;

/** Recurrence analysis is internal to departments. Citizens and workers do not receive it. */
@RestController
@RequestMapping("/api/recurrence")
public class RecurrenceController {

	private final RecurrenceService recurrenceService;

	public RecurrenceController(RecurrenceService recurrenceService) {
		this.recurrenceService = recurrenceService;
	}

	@PostMapping("/complaints/{complaintId}/analyze")
	@PreAuthorize("hasAnyRole('DEPARTMENT_ADMIN', 'SYSTEM_ADMIN')")
	public ResponseEntity<RecurrenceReportResponseDto> analyzeComplaintRecurrence(@PathVariable Long complaintId) {
		return ResponseEntity.ok(recurrenceService.analyzeRecurrence(complaintId, AccessRules.currentPrincipal()));
	}

	@GetMapping("/reports")
	@PreAuthorize("hasAnyRole('DEPARTMENT_ADMIN', 'SYSTEM_ADMIN')")
	public ResponseEntity<Page<RecurrenceReportResponseDto>> getAllRecurrenceReports(Pageable pageable) {
		return ResponseEntity.ok(recurrenceService.getReports(AccessRules.currentPrincipal(), pageable));
	}

	@GetMapping("/reports/{reportId}")
	@PreAuthorize("hasAnyRole('DEPARTMENT_ADMIN', 'SYSTEM_ADMIN')")
	public ResponseEntity<RecurrenceReportResponseDto> getRecurrenceReport(@PathVariable Long reportId) {
		return ResponseEntity.ok(recurrenceService.getReport(reportId, AccessRules.currentPrincipal()));
	}

	@GetMapping("/complaints/{complaintId}/report")
	@PreAuthorize("hasAnyRole('DEPARTMENT_ADMIN', 'SYSTEM_ADMIN')")
	public ResponseEntity<RecurrenceReportResponseDto> getComplaintRecurrenceReport(@PathVariable Long complaintId) {
		return ResponseEntity.ok(recurrenceService.getReportForComplaint(complaintId, AccessRules.currentPrincipal()));
	}

	@GetMapping("/high-risk-areas")
	@PreAuthorize("hasRole('SYSTEM_ADMIN')")
	public ResponseEntity<List<RecurrenceReportResponseDto>> getHighRiskAreas(
			@RequestParam(defaultValue = "3") int minHistoricalCount) {
		return ResponseEntity.ok(recurrenceService.getHighRiskAreas(
				minHistoricalCount, AccessRules.currentPrincipal()));
	}

	@GetMapping("/category/{category}/count")
	@PreAuthorize("hasAnyRole('DEPARTMENT_ADMIN', 'SYSTEM_ADMIN')")
	public ResponseEntity<Long> getCategoryRecurrenceCount(@PathVariable ComplaintCategory category) {
		return ResponseEntity.ok(recurrenceService.getCategoryCount(category, AccessRules.currentPrincipal()));
	}
}
