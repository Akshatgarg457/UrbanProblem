package com.urbanproblems.dto;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiResponse<T> {

	private boolean success;
	private T data;
	private String message;
	private String error;
	@Builder.Default
	private Instant timestamp = Instant.now();

	public static <T> ApiResponse<T> success(T data, String message) {
		return ApiResponse.<T>builder()
				.success(true)
				.data(data)
				.message(message)
				.build();
	}

	public static <T> ApiResponse<T> success(T data) {
		return ApiResponse.<T>builder()
				.success(true)
				.data(data)
				.build();
	}

	public static <T> ApiResponse<T> error(String error, String message) {
		return ApiResponse.<T>builder()
				.success(false)
				.error(error)
				.message(message)
				.build();
	}

	public static <T> ApiResponse<T> error(String message) {
		return ApiResponse.<T>builder()
				.success(false)
				.error("ERROR")
				.message(message)
				.build();
	}
}
