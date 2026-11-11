package com.urbanproblems.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
public class AuthRequestDto {

	/** Email address, or the username (email local part) of the account. */
	@NotBlank(message = "Username or email is required")
	@Size(max = 254, message = "Username or email must be 254 characters or fewer")
	private String email;

	@NotBlank(message = "Password is required")
	private String password;

	@Pattern(regexp = "(?i)CITIZEN|WORKER|ADMIN", message = "Workspace must be CITIZEN, WORKER, or ADMIN")
	private String workspace;
}
