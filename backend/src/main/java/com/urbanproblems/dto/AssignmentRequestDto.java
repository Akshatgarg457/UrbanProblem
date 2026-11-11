package com.urbanproblems.dto;

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
public class AssignmentRequestDto {

	@NotNull(message = "Worker ID is required")
	private Long workerId;

	@Size(max = 500, message = "Assignment notes must not exceed 500 characters")
	private String assignmentNotes;
}
