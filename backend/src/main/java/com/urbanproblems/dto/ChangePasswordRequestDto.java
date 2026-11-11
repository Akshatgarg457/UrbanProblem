package com.urbanproblems.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequestDto(
		@NotBlank(message = "Current password is required") String currentPassword,
		@NotBlank(message = "New password is required")
		@Size(min = 8, max = 128, message = "New password must be between 8 and 128 characters") String newPassword) {
}
