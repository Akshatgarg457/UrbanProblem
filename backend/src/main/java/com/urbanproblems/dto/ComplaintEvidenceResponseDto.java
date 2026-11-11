package com.urbanproblems.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplaintEvidenceResponseDto {
	private Long id;
	private Long complaintId;
	private String fileName;
	private String fileType;
	private Long fileSize;
	private String uploadedBy;
	private Instant createdAt;
	private String downloadUrl;
}
