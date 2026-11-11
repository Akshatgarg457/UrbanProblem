package com.urbanproblems.dto;

import java.time.Instant;

public record AdminResponseDto(
		Long id,
		String email,
		String firstName,
		String lastName,
		String role,
		Long departmentId,
		String status,
		Instant createdAt) {
}
