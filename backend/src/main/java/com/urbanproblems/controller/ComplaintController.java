package com.urbanproblems.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.urbanproblems.domain.enums.ComplaintCategory;
import com.urbanproblems.domain.enums.ComplaintStatus;
import com.urbanproblems.dto.ComplaintHistoryResponseDto;
import com.urbanproblems.dto.ComplaintRequestDto;
import com.urbanproblems.dto.ComplaintResponseDto;
import com.urbanproblems.security.AccessRules;
import com.urbanproblems.service.ComplaintService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {

	private final ComplaintService complaintService;

	public ComplaintController(ComplaintService complaintService) {
		this.complaintService = complaintService;
	}

	@PostMapping
	@PreAuthorize("hasRole('CITIZEN')")
	public ResponseEntity<ComplaintResponseDto> createComplaint(@Valid @RequestBody ComplaintRequestDto requestDto) {
		ComplaintResponseDto response = complaintService.createComplaint(
				AccessRules.currentPrincipal().getId(), requestDto);
		return ResponseEntity.status(HttpStatus.CREATED).body(response);
	}

	/** Visible to the citizen who filed it, the assigned worker, the department admin, or a system admin. */
	@GetMapping("/{publicId}")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<ComplaintResponseDto> getComplaintByPublicId(@PathVariable String publicId) {
		return ResponseEntity.ok(complaintService.getComplaintByPublicId(publicId, AccessRules.currentPrincipal()));
	}

	/** Read-only status history; same visibility rules as the complaint itself. */
	@GetMapping("/{publicId}/history")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<java.util.List<ComplaintHistoryResponseDto>> getComplaintHistory(
			@PathVariable String publicId) {
		return ResponseEntity.ok(complaintService.getComplaintHistory(publicId, AccessRules.currentPrincipal()));
	}

	@GetMapping("/internal/{id}")
	@PreAuthorize("hasAnyRole('DEPARTMENT_ADMIN', 'SYSTEM_ADMIN')")
	public ResponseEntity<ComplaintResponseDto> getComplaintById(@PathVariable Long id) {
		return ResponseEntity.ok(complaintService.getComplaintById(id, AccessRules.currentPrincipal()));
	}

	@GetMapping("/my-complaints")
	@PreAuthorize("hasRole('CITIZEN')")
	public ResponseEntity<Page<ComplaintResponseDto>> getMyComplaints(Pageable pageable) {
		return ResponseEntity.ok(complaintService.getComplaintsByCitizen(
				AccessRules.currentPrincipal().getId(), pageable));
	}

	@GetMapping("/department/{departmentId}")
	@PreAuthorize("hasAnyRole('DEPARTMENT_ADMIN', 'SYSTEM_ADMIN')")
	public ResponseEntity<Page<ComplaintResponseDto>> getComplaintsByDepartment(
			@PathVariable Long departmentId, Pageable pageable) {
		return ResponseEntity.ok(complaintService.getComplaintsByDepartment(
				departmentId, AccessRules.currentPrincipal(), pageable));
	}

	@GetMapping
	@PreAuthorize("hasRole('SYSTEM_ADMIN')")
	public ResponseEntity<Page<ComplaintResponseDto>> getAllComplaints(
			@RequestParam(required = false) ComplaintStatus status,
			@RequestParam(required = false) ComplaintCategory category,
			Pageable pageable) {
		if (status != null) {
			return ResponseEntity.ok(complaintService.getComplaintsByStatus(status, pageable));
		}
		if (category != null) {
			return ResponseEntity.ok(complaintService.getComplaintsByCategory(category, pageable));
		}
		return ResponseEntity.ok(complaintService.getComplaintsByStatus(ComplaintStatus.SUBMITTED, pageable));
	}
}
