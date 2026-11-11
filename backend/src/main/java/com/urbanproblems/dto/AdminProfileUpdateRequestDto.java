package com.urbanproblems.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminProfileUpdateRequestDto(
		@NotBlank(message = "First name is required")
		@Size(min = 1, max = 50, message = "First name must be between 1 and 50 characters")
		String firstName,

		@NotBlank(message = "Last name is required")
		@Size(min = 1, max = 50, message = "Last name must be between 1 and 50 characters")
		String lastName) {
}
