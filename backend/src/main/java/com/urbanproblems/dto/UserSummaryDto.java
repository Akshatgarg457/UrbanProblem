package com.urbanproblems.dto;

import java.time.Instant;

public record UserSummaryDto(
		Long id,
		String email,
		String firstName,
		String lastName,
		String phoneNumber,
		String role,
		Long departmentId,
		String departmentName,
		boolean active,
		Instant createdAt) {
}

