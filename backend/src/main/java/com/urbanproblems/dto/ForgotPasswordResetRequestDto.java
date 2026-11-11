package com.urbanproblems.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
public class ForgotPasswordResetRequestDto {

	@NotBlank(message = "Username or email is required")
	@Size(max = 255, message = "Identifier must be at most 255 characters")
	private String identifier;

	@NotBlank(message = "Security answer is required")
	@Size(max = 128, message = "Security answer must be at most 128 characters")
	private String answer;

	@NotBlank(message = "New password is required")
	@Size(min = 8, max = 128, message = "New password must be between 8 and 128 characters")
	private String newPassword;
}
