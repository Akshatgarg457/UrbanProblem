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
import org.springframework.web.bind.annotation.RestController;

import com.urbanproblems.dto.ResolutionResponseDto;
import com.urbanproblems.dto.ResolutionReviewDto;
import com.urbanproblems.dto.ResolutionSubmitDto;
import com.urbanproblems.security.AccessRules;
import com.urbanproblems.service.ResolutionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/resolutions")
public class ResolutionController {

	private final ResolutionService resolutionService;

	public ResolutionController(ResolutionService resolutionService) {
		this.resolutionService = resolutionService;
	}

	@PostMapping("/assignments/{assignmentId}/submit")
	@PreAuthorize("hasRole('WORKER')")
	public ResponseEntity<ResolutionResponseDto> submitResolution(
			@PathVariable Long assignmentId,
			@Valid @RequestBody ResolutionSubmitDto submitDto) {
		return ResponseEntity.status(HttpStatus.CREATED).body(
				resolutionService.submitResolution(assignmentId, submitDto, AccessRules.currentPrincipal()));
	}

	@PostMapping("/{resolutionId}/approve")
	@PreAuthorize("hasAnyRole('DEPARTMENT_ADMIN', 'SYSTEM_ADMIN')")
	public ResponseEntity<ResolutionResponseDto> approveResolution(
			@PathVariable Long resolutionId,
			@Valid @RequestBody ResolutionReviewDto reviewDto) {
		return ResponseEntity.ok(resolutionService.approveResolution(
				resolutionId, reviewDto, AccessRules.currentPrincipal()));
	}

	@PostMapping("/{resolutionId}/reject")
	@PreAuthorize("hasAnyRole('DEPARTMENT_ADMIN', 'SYSTEM_ADMIN')")
	public ResponseEntity<ResolutionResponseDto> rejectResolution(
			@PathVariable Long resolutionId,
			@Valid @RequestBody ResolutionReviewDto reviewDto) {
		return ResponseEntity.ok(resolutionService.rejectResolution(
				resolutionId, reviewDto, AccessRules.currentPrincipal()));
	}

	@GetMapping("/{resolutionId}")
	@PreAuthorize("hasAnyRole('WORKER', 'DEPARTMENT_ADMIN', 'SYSTEM_ADMIN')")
	public ResponseEntity<ResolutionResponseDto> getResolution(@PathVariable Long resolutionId) {
		return ResponseEntity.ok(resolutionService.getResolution(resolutionId, AccessRules.currentPrincipal()));
	}

	@GetMapping("/assignments/{assignmentId}/latest")
	@PreAuthorize("hasAnyRole('WORKER', 'DEPARTMENT_ADMIN', 'SYSTEM_ADMIN')")
	public ResponseEntity<ResolutionResponseDto> getLatestForAssignment(@PathVariable Long assignmentId) {
		return ResponseEntity.ok(
				resolutionService.getLatestForAssignment(assignmentId, AccessRules.currentPrincipal()));
	}

	@GetMapping("/pending")
	@PreAuthorize("hasAnyRole('DEPARTMENT_ADMIN', 'SYSTEM_ADMIN')")
	public ResponseEntity<Page<ResolutionResponseDto>> getPendingResolutions(Pageable pageable) {
		return ResponseEntity.ok(resolutionService.getPendingResolutions(AccessRules.currentPrincipal(), pageable));
	}

	@GetMapping("/complaints/{complaintId}")
	@PreAuthorize("hasAnyRole('CITIZEN', 'DEPARTMENT_ADMIN', 'WORKER', 'SYSTEM_ADMIN')")
	public ResponseEntity<Page<ResolutionResponseDto>> getComplaintResolutions(
			@PathVariable Long complaintId, Pageable pageable) {
		return ResponseEntity.ok(resolutionService.getResolutionsByComplaint(
				complaintId, AccessRules.currentPrincipal(), pageable));
	}
}
