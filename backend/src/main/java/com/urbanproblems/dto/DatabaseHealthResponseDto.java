package com.urbanproblems.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DatabaseHealthResponseDto(
		String status,
		String database,
		String message) {

	public static DatabaseHealthResponseDto connected(String database, String message) {
		return new DatabaseHealthResponseDto("CONNECTED", database, message);
	}

	public static DatabaseHealthResponseDto disconnected(String message) {
		return new DatabaseHealthResponseDto("DISCONNECTED", null, message);
	}

	public static DatabaseHealthResponseDto notConfigured(String message) {
		return new DatabaseHealthResponseDto("NOT_CONFIGURED", null, message);
	}
}

