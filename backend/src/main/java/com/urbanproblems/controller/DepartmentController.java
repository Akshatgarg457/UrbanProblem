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

import com.urbanproblems.domain.enums.DepartmentStatus;
import com.urbanproblems.dto.DepartmentRequestDto;
import com.urbanproblems.dto.DepartmentResponseDto;
import com.urbanproblems.service.DepartmentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

	private final DepartmentService departmentService;

	public DepartmentController(DepartmentService departmentService) {
		this.departmentService = departmentService;
	}

	@PostMapping
	@PreAuthorize("hasRole('SYSTEM_ADMIN')")
	public ResponseEntity<DepartmentResponseDto> createDepartment(
			@Valid @RequestBody DepartmentRequestDto requestDto) {
		try {
			DepartmentResponseDto response = departmentService.createDepartment(requestDto);
			return ResponseEntity.status(HttpStatus.CREATED).body(response);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
		}
	}

	@GetMapping("/{departmentId}")
	@PreAuthorize("hasAnyRole('DEPARTMENT_ADMIN', 'SYSTEM_ADMIN')")
	public ResponseEntity<DepartmentResponseDto> getDepartment(@PathVariable Long departmentId) {
		return ResponseEntity.ok(departmentService.getDepartment(departmentId,
				com.urbanproblems.security.AccessRules.currentPrincipal()));
	}

	@GetMapping
	@PreAuthorize("hasAnyRole('SYSTEM_ADMIN')")
	public ResponseEntity<Page<DepartmentResponseDto>> getAllDepartments(
			@RequestParam(required = false, defaultValue = "false") boolean activeOnly,
			Pageable pageable) {
		Page<DepartmentResponseDto> response;
		if (activeOnly) {
			response = departmentService.getActiveDepartments(pageable);
		} else {
			response = departmentService.getAllDepartments(pageable);
		}
		return ResponseEntity.ok(response);
	}

	@PutMapping("/{departmentId}")
	@PreAuthorize("hasRole('SYSTEM_ADMIN')")
	public ResponseEntity<DepartmentResponseDto> updateDepartment(
			@PathVariable Long departmentId,
			@Valid @RequestBody DepartmentRequestDto requestDto) {
		try {
			DepartmentResponseDto response = departmentService.updateDepartment(departmentId, requestDto);
			return ResponseEntity.ok(response);
		} catch (IllegalArgumentException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
	}

	@PutMapping("/{departmentId}/status")
	@PreAuthorize("hasRole('SYSTEM_ADMIN')")
	public ResponseEntity<DepartmentResponseDto> changeDepartmentStatus(
			@PathVariable Long departmentId,
			@RequestParam DepartmentStatus status) {
		try {
			DepartmentResponseDto response = departmentService.changeDepartmentStatus(departmentId, status);
			return ResponseEntity.ok(response);
		} catch (IllegalArgumentException | IllegalStateException e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
		}
	}
}
