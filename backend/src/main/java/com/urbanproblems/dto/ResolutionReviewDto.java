package com.urbanproblems.dto;

import com.urbanproblems.domain.entity.Resolution.ReviewStatus;

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
public class ResolutionReviewDto {

	@NotNull(message = "Review status (APPROVED or REJECTED) is required")
	private ReviewStatus reviewStatus;

	@Size(max = 1000, message = "Admin notes must not exceed 1000 characters")
	private String adminNotes;

	@Size(max = 1000, message = "Rejection reason must not exceed 1000 characters")
	private String rejectionReason;
}
