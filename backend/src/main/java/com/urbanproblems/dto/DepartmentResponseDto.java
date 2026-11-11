package com.urbanproblems.dto;

import java.time.Instant;

import com.urbanproblems.domain.enums.DepartmentStatus;

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
public class DepartmentResponseDto {

	private Long id;
	private String name;
	private String description;
	private String headEmail;
	private DepartmentStatus status;
	private long workerCount;
	private long complaintCount;
	private Instant createdAt;
	private Instant updatedAt;
}
