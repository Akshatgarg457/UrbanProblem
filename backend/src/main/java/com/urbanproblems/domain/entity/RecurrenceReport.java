package com.urbanproblems.domain.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.urbanproblems.domain.enums.ComplaintCategory;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "recurrence_reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecurrenceReport {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotNull(message = "Current complaint is required")
	@ManyToOne
	@JoinColumn(name = "current_complaint_id", nullable = false, foreignKey = @ForeignKey(name = "fk_recurrence_current_complaint"))
	private Complaint currentComplaint;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ComplaintCategory category;

	@Column(nullable = false)
	private String location;

	@JdbcTypeCode(SqlTypes.DECIMAL)
	@Column(precision = 10, scale = 8)
	private Double latitude;

	@JdbcTypeCode(SqlTypes.DECIMAL)
	@Column(precision = 11, scale = 8)
	private Double longitude;

	@JdbcTypeCode(SqlTypes.DECIMAL)
	@Column(precision = 5, scale = 2)
	private Double proximityRadiusKm;

	private Integer historicalIssueCount;

	@ElementCollection
	@Column(name = "related_complaint_id")
	@Builder.Default
	private List<Long> relatedComplaintIds = new ArrayList<>();

	@Column(length = 1000)
	private String analysisNotes;

	@Column(nullable = false, updatable = false)
	@Builder.Default
	private Instant createdAt = Instant.now();

	@Column(nullable = false)
	@Builder.Default
	private Instant updatedAt = Instant.now();
}
