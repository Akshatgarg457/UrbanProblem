package com.urbanproblems.dto;

import java.time.Instant;

import com.urbanproblems.domain.enums.ComplaintCategory;
import com.urbanproblems.domain.enums.ComplaintStatus;

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
public class ComplaintResponseDto {

	private Long id;
	private String publicId;
	private String title;
	private String description;
	private ComplaintCategory category;
	private String location;
	private Double latitude;
	private Double longitude;
	private Long citizenId;
	private String citizenName;
	private Long departmentId;
	private String departmentName;
	private ComplaintStatus status;
	private Instant createdAt;
	private Instant updatedAt;
}
