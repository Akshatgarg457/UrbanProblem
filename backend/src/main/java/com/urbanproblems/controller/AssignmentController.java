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

import com.urbanproblems.dto.AssignmentRequestDto;
import com.urbanproblems.dto.AssignmentResponseDto;
import com.urbanproblems.security.AccessRules;
import com.urbanproblems.service.AssignmentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {

	private final AssignmentService assignmentService;

	public AssignmentController(AssignmentService assignmentService) {
		this.assignmentService = assignmentService;
	}

	@PostMapping("/complaints/{complaintId}/assign")
	@PreAuthorize("hasAnyRole('DEPARTMENT_ADMIN', 'SYSTEM_ADMIN')")
	public ResponseEntity<AssignmentResponseDto> assignComplaint(
			@PathVariable Long complaintId,
			@Valid @RequestBody AssignmentRequestDto requestDto) {
		return ResponseEntity.status(HttpStatus.CREATED).body(
				assignmentService.assignComplaintToWorker(complaintId, requestDto, AccessRules.currentPrincipal()));
	}

	/** The assigned worker starts work on their own task. */
	@PostMapping("/{assignmentId}/start")
	@PreAuthorize("hasRole('WORKER')")
	public ResponseEntity<AssignmentResponseDto> startWork(@PathVariable Long assignmentId) {
		return ResponseEntity.ok(assignmentService.startWork(assignmentId, AccessRules.currentPrincipal()));
	}

	@GetMapping("/my-assignments")
	@PreAuthorize("hasRole('WORKER')")
	public ResponseEntity<Page<AssignmentResponseDto>> getMyAssignments(Pageable pageable) {
		return ResponseEntity.ok(assignmentService.getWorkerAssignments(
				AccessRules.currentPrincipal().getId(), pageable));
	}

	@GetMapping("/my-assignments/active")
	@PreAuthorize("hasRole('WORKER')")
	public ResponseEntity<Page<AssignmentResponseDto>> getMyActiveAssignments(Pageable pageable) {
		return ResponseEntity.ok(assignmentService.getWorkerActiveAssignments(
				AccessRules.currentPrincipal().getId(), pageable));
	}

	@GetMapping("/{assignmentId}")
	@PreAuthorize("hasAnyRole('WORKER', 'DEPARTMENT_ADMIN')")
	public ResponseEntity<AssignmentResponseDto> getAssignment(@PathVariable Long assignmentId) {
		return ResponseEntity.ok(assignmentService.getAssignment(assignmentId, AccessRules.currentPrincipal()));
	}
}
