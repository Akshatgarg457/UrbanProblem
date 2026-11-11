package com.urbanproblems.domain.enums;

import java.util.EnumSet;
import java.util.Set;

/**
 * Complaint lifecycle. Transitions are enforced centrally by {@link #canTransitionTo(ComplaintStatus)}.
 *
 * SUBMITTED -> ASSIGNED -> IN_PROGRESS -> PENDING_VERIFICATION -> APPROVED -> RESOLVED
 * PENDING_VERIFICATION -> IN_PROGRESS is the rework loop (resolution rejected).
 */
public enum ComplaintStatus {
	SUBMITTED("SUBMITTED"),
	ASSIGNED("ASSIGNED"),
	IN_PROGRESS("IN_PROGRESS"),
	PENDING_VERIFICATION("PENDING_VERIFICATION"),
	APPROVED("APPROVED"),
	REJECTED("REJECTED"),
	RESOLVED("RESOLVED"),
	CANCELLED("CANCELLED");

	private final String value;

	ComplaintStatus(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public boolean canTransitionTo(ComplaintStatus next) {
		return allowedNext().contains(next);
	}

	private Set<ComplaintStatus> allowedNext() {
		return switch (this) {
		case SUBMITTED -> EnumSet.of(ASSIGNED, CANCELLED);
		case ASSIGNED -> EnumSet.of(IN_PROGRESS, CANCELLED);
		case IN_PROGRESS -> EnumSet.of(PENDING_VERIFICATION);
		case PENDING_VERIFICATION -> EnumSet.of(APPROVED, IN_PROGRESS);
		case APPROVED -> EnumSet.of(RESOLVED);
		case REJECTED, RESOLVED, CANCELLED -> EnumSet.noneOf(ComplaintStatus.class);
		};
	}
}
