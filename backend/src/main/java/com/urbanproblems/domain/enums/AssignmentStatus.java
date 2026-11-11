package com.urbanproblems.domain.enums;

public enum AssignmentStatus {
	ASSIGNED("ASSIGNED"),
	IN_PROGRESS("IN_PROGRESS"),
	SUBMITTED_FOR_REVIEW("SUBMITTED_FOR_REVIEW"),
	APPROVED("APPROVED"),
	REJECTED("REJECTED"),
	COMPLETED("COMPLETED"),
	CANCELLED("CANCELLED");

	private final String value;

	AssignmentStatus(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}
}
