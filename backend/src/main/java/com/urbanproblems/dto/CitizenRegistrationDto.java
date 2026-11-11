package com.urbanproblems.dto;

import jakarta.validation.constraints.Email;
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
public class CitizenRegistrationDto {

	@NotBlank(message = "Email is required")
	@Email(message = "Email must be valid")
	private String email;

	@NotBlank(message = "First name is required")
	@Size(min = 1, max = 50, message = "First name must be between 1 and 50 characters")
	private String firstName;

	@NotBlank(message = "Last name is required")
	@Size(min = 1, max = 50, message = "Last name must be between 1 and 50 characters")
	private String lastName;

	@NotBlank(message = "Phone number is required")
	@Size(min = 10, max = 20, message = "Phone number must be between 10 and 20 characters")
	private String phoneNumber;

	@NotBlank(message = "Password is required")
	@Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
	private String password;

	@NotBlank(message = "Security question is required")
	@Size(max = 255, message = "Security question must be at most 255 characters")
	@Builder.Default
	private String securityQuestion = "What city were you born in?";

	@NotBlank(message = "Security answer is required")
	@Size(min = 2, max = 128, message = "Security answer must be between 2 and 128 characters")
	@Builder.Default
	private String securityAnswer = "unknown";
}
