package com.urbanproblems.dto;

import java.time.Instant;

public record NotificationResponseDto(
		Long id,
		Long recipientId,
		String recipientRole,
		String title,
		String message,
		String complaintPublicId,
		boolean read,
		Instant createdAt) {
}
