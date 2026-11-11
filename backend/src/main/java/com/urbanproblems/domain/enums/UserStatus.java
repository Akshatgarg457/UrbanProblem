package com.urbanproblems.domain.enums;

public enum UserStatus {
	ACTIVE("ACTIVE"),
	INACTIVE("INACTIVE"),
	SUSPENDED("SUSPENDED");

	private final String value;

	UserStatus(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}
}
