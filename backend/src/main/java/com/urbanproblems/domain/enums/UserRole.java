package com.urbanproblems.domain.enums;

public enum UserRole {
	CITIZEN("CITIZEN"),
	WORKER("WORKER"),
	DEPARTMENT_ADMIN("DEPARTMENT_ADMIN"),
	SYSTEM_ADMIN("SYSTEM_ADMIN");

	private final String value;

	UserRole(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}
}
