package com.urbanproblems.domain.enums;

public enum DepartmentStatus {
	ACTIVE("ACTIVE"),
	INACTIVE("INACTIVE");

	private final String value;

	DepartmentStatus(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}
}
