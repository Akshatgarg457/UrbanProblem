package com.urbanproblems.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.urbanproblems.domain.enums.UserStatus;
import com.urbanproblems.dto.WorkerProfileResponseDto;
import com.urbanproblems.dto.WorkerProfileUpdateRequestDto;
import com.urbanproblems.dto.WorkerRegistrationDto;
import com.urbanproblems.dto.WorkerResponseDto;
import com.urbanproblems.security.AccessRules;
import com.urbanproblems.service.WorkerService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/workers")
public class WorkerController {

	private final WorkerService workerService;

	public WorkerController(WorkerService workerService) {
		this.workerService = workerService;
	}

	@GetMapping("/me")
	@PreAuthorize("hasRole('WORKER')")
	public ResponseEntity<WorkerProfileResponseDto> getMyProfile() {
		return ResponseEntity.ok(workerService.getMyProfile(AccessRules.currentPrincipal().getId()));
	}

	@PutMapping("/me")
	@PreAuthorize("hasRole('WORKER')")
	public ResponseEntity<WorkerProfileResponseDto> updateMyProfile(
			@Valid @RequestBody WorkerProfileUpdateRequestDto requestDto) {
		return ResponseEntity.ok(workerService.updateMyProfile(AccessRules.currentPrincipal().getId(), requestDto));
	}

	@PostMapping
	@PreAuthorize("hasRole('SYSTEM_ADMIN')")
	public ResponseEntity<WorkerResponseDto> registerWorker(
			@Valid @RequestBody WorkerRegistrationDto registrationDto) {
		return ResponseEntity.status(HttpStatus.CREATED).body(workerService.registerWorker(registrationDto));
	}

	/** Worker applications awaiting review. INACTIVE workers cannot sign in until approved. */
	@GetMapping("/pending")
	@PreAuthorize("hasRole('SYSTEM_ADMIN')")
	public ResponseEntity<Page<WorkerResponseDto>> getPendingApplications(Pageable pageable) {
		return ResponseEntity.ok(workerService.getPendingApplications(pageable));
	}

	@PostMapping("/{workerId}/approve")
	@PreAuthorize("hasRole('SYSTEM_ADMIN')")
	public ResponseEntity<WorkerResponseDto> approveWorker(@PathVariable Long workerId) {
		return ResponseEntity.ok(workerService.approveWorker(workerId));
	}

	@PostMapping("/{workerId}/reject")
	@PreAuthorize("hasRole('SYSTEM_ADMIN')")
	public ResponseEntity<WorkerResponseDto> rejectWorker(@PathVariable Long workerId) {
		return ResponseEntity.ok(workerService.rejectWorker(workerId));
	}

	@GetMapping("/{workerId}")
	@PreAuthorize("hasAnyRole('WORKER', 'DEPARTMENT_ADMIN', 'SYSTEM_ADMIN')")
	public ResponseEntity<WorkerResponseDto> getWorker(@PathVariable Long workerId) {
		return ResponseEntity.ok(workerService.getWorker(workerId, AccessRules.currentPrincipal()));
	}

	@GetMapping("/department/{departmentId}")
	@PreAuthorize("hasAnyRole('DEPARTMENT_ADMIN', 'SYSTEM_ADMIN')")
	public ResponseEntity<Page<WorkerResponseDto>> getWorkersByDepartment(
			@PathVariable Long departmentId,
			@RequestParam(required = false, defaultValue = "false") boolean activeOnly,
			Pageable pageable) {
		return ResponseEntity.ok(workerService.getWorkersByDepartment(
				departmentId, activeOnly, AccessRules.currentPrincipal(), pageable));
	}

	@PutMapping("/{workerId}/status")
	@PreAuthorize("hasRole('SYSTEM_ADMIN')")
	public ResponseEntity<WorkerResponseDto> updateWorkerStatus(
			@PathVariable Long workerId,
			@RequestParam UserStatus status) {
		return ResponseEntity.ok(workerService.updateWorkerStatus(workerId, status));
	}

	@PutMapping("/{workerId}/department")
	@PreAuthorize("hasRole('SYSTEM_ADMIN')")
	public ResponseEntity<WorkerResponseDto> changeWorkerDepartment(
			@PathVariable Long workerId,
			@RequestParam Long newDepartmentId) {
		return ResponseEntity.ok(workerService.changeWorkerDepartment(workerId, newDepartmentId));
	}
}
