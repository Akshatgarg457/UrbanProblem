package com.urbanproblems.dto;

import java.time.Instant;

import com.urbanproblems.domain.enums.AssignmentStatus;

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
public class AssignmentResponseDto {

	private Long id;
	private Long complaintId;
	private String complaintPublicId;
	private String complaintTitle;
	private Long workerId;
	private String workerName;
	private String assignmentNotes;
	private AssignmentStatus status;
	private Instant createdAt;
	private Instant updatedAt;
}
