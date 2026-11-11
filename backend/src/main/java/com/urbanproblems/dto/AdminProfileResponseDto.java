package com.urbanproblems.dto;

import java.time.Instant;

public record AdminProfileResponseDto(
		Long id,
		String email,
		String firstName,
		String lastName,
		String role,
		Long departmentId,
		String departmentName,
		String status,
		Instant createdAt) {
}
