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
public class ForgotPasswordQuestionRequestDto {

	@NotBlank(message = "Username or email is required")
	@Size(max = 255, message = "Identifier must be at most 255 characters")
	private String identifier;
}
