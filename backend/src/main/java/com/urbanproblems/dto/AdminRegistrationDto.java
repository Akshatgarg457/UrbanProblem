package com.urbanproblems.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class AdminRegistrationDto {

	@NotBlank(message = "Email is required")
	@Email(message = "Email must be valid")
	private String email;

	@NotBlank(message = "First name is required")
	@Size(max = 50)
	private String firstName;

	@NotBlank(message = "Last name is required")
	@Size(max = 50)
	private String lastName;

	@NotBlank(message = "Password is required")
	@Size(min = 12, max = 128, message = "Password must be between 12 and 128 characters")
	private String password;

	@NotNull(message = "Department ID is required")
	private Long departmentId;
}
