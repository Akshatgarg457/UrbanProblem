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
public class DepartmentRequestDto {

	@NotBlank(message = "Department name is required")
	@Size(min = 1, max = 100, message = "Department name must be between 1 and 100 characters")
	private String name;

	@Size(max = 500, message = "Description must not exceed 500 characters")
	private String description;

	@NotBlank(message = "Department head email is required")
	@Email(message = "Department head email must be valid")
	private String headEmail;
}
