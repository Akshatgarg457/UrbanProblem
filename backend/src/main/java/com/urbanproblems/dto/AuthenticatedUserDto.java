package com.urbanproblems.dto;

import java.time.Instant;

public record AuthenticatedUserDto(
		Long id,
		String email,
		String firstName,
		String lastName,
		String role,
		Long departmentId,
		String departmentName,
		boolean active,
		Instant createdAt) {
}
