package com.urbanproblems.dto;

import java.time.Instant;

import com.urbanproblems.domain.enums.ComplaintStatus;

public record ComplaintHistoryResponseDto(
		Long id,
		ComplaintStatus fromStatus,
		ComplaintStatus toStatus,
		String actorRole,
		String note,
		Instant changedAt) {
}
