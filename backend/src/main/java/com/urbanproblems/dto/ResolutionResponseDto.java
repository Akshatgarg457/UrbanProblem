package com.urbanproblems.dto;

import java.time.Instant;

import com.urbanproblems.domain.entity.Resolution.ReviewStatus;

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
public class ResolutionResponseDto {

	private Long id;
	private Long assignmentId;
	private Long complaintId;
	private String complaintPublicId;
	private String completionNotes;
	private String adminNotes;
	private String rejectionReason;
	private ReviewStatus reviewStatus;
	private Instant submittedAt;
	private Instant reviewedAt;
}
