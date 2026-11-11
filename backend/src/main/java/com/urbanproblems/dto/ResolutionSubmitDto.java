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
public class ResolutionSubmitDto {

	@NotBlank(message = "Completion notes are required")
	@Size(min = 10, max = 2000, message = "Completion notes must be between 10 and 2000 characters")
	private String completionNotes;
}
