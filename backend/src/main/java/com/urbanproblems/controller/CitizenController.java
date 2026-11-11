package com.urbanproblems.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.urbanproblems.dto.CitizenProfileUpdateRequestDto;
import com.urbanproblems.dto.CitizenResponseDto;
import com.urbanproblems.security.AccessRules;
import com.urbanproblems.service.CitizenService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/citizens")
public class CitizenController {

	private final CitizenService citizenService;

	public CitizenController(CitizenService citizenService) {
		this.citizenService = citizenService;
	}

	@GetMapping("/me")
	@PreAuthorize("hasRole('CITIZEN')")
	public ResponseEntity<CitizenResponseDto> getMyProfile() {
		Long citizenId = AccessRules.currentPrincipal().getId();
		return ResponseEntity.ok(citizenService.getProfile(citizenId));
	}

	@PutMapping("/me")
	@PreAuthorize("hasRole('CITIZEN')")
	public ResponseEntity<CitizenResponseDto> updateMyProfile(
			@Valid @RequestBody CitizenProfileUpdateRequestDto requestDto) {
		Long citizenId = AccessRules.currentPrincipal().getId();
		return ResponseEntity.ok(citizenService.updateProfile(citizenId, requestDto));
	}
}

