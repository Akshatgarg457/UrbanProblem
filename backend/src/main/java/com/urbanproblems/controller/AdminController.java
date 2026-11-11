package com.urbanproblems.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.urbanproblems.dto.AdminProfileResponseDto;
import com.urbanproblems.dto.AdminProfileUpdateRequestDto;
import com.urbanproblems.dto.AdminRegistrationDto;
import com.urbanproblems.dto.AdminResponseDto;
import com.urbanproblems.dto.UserSummaryDto;
import com.urbanproblems.security.AccessRules;
import com.urbanproblems.service.AdminService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

	private final AdminService adminService;

	public AdminController(AdminService adminService) {
		this.adminService = adminService;
	}

	/** Creates a department administrator. Department admins cannot create accounts or change their own role. */
	@PostMapping("/department-admins")
	@PreAuthorize("hasRole('SYSTEM_ADMIN')")
	public ResponseEntity<AdminResponseDto> createDepartmentAdmin(@Valid @RequestBody AdminRegistrationDto dto) {
		return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createDepartmentAdmin(dto));
	}

	/** Own profile for both department admins and system admins. */
	@GetMapping("/me")
	@PreAuthorize("hasAnyRole('DEPARTMENT_ADMIN', 'SYSTEM_ADMIN')")
	public ResponseEntity<AdminProfileResponseDto> getMyProfile() {
		return ResponseEntity.ok(adminService.getMyProfile(AccessRules.currentPrincipal()));
	}

	@PutMapping("/me")
	@PreAuthorize("hasAnyRole('DEPARTMENT_ADMIN', 'SYSTEM_ADMIN')")
	public ResponseEntity<AdminProfileResponseDto> updateMyProfile(
			@Valid @RequestBody AdminProfileUpdateRequestDto dto) {
		return ResponseEntity.ok(adminService.updateMyProfile(AccessRules.currentPrincipal(), dto));
	}

	@GetMapping("/users")
	@PreAuthorize("hasRole('SYSTEM_ADMIN')")
	public ResponseEntity<java.util.List<UserSummaryDto>> getAllUsers(@RequestParam(required = false) String role) {
		return ResponseEntity.ok(adminService.getAllUsers(role));
	}
}
