package com.urbanproblems.dto;

import java.time.Instant;
import java.util.List;

import com.urbanproblems.domain.entity.RecurrenceReport;
import com.urbanproblems.domain.enums.ComplaintCategory;

import lombok.Builder;
import lombok.Getter;

/** Public view of a recurrence report. Never exposes the complaint entity graph. */
@Getter
@Builder
public class RecurrenceReportResponseDto {

	private Long id;
	private Long complaintId;
	private String complaintPublicId;
	private Long departmentId;
	private ComplaintCategory category;
	private String location;
	private Double latitude;
	private Double longitude;
	private Double proximityRadiusKm;
	private Integer historicalIssueCount;
	private List<Long> relatedComplaintIds;
	private String analysisNotes;
	private Instant createdAt;
	private Instant updatedAt;

	public static RecurrenceReportResponseDto from(RecurrenceReport report) {
		return RecurrenceReportResponseDto.builder()
				.id(report.getId())
				.complaintId(report.getCurrentComplaint().getId())
				.complaintPublicId(report.getCurrentComplaint().getPublicId())
				.departmentId(report.getCurrentComplaint().getDepartment().getId())
				.category(report.getCategory())
				.location(report.getLocation())
				.latitude(report.getLatitude())
				.longitude(report.getLongitude())
				.proximityRadiusKm(report.getProximityRadiusKm())
				.historicalIssueCount(report.getHistoricalIssueCount())
				.relatedComplaintIds(report.getRelatedComplaintIds())
				.analysisNotes(report.getAnalysisNotes())
				.createdAt(report.getCreatedAt())
				.updatedAt(report.getUpdatedAt())
				.build();
	}
}
