package com.urbanproblems.domain.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "resolutions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Resolution {

	public enum ReviewStatus {
		PENDING, APPROVED, REJECTED
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotNull(message = "Assignment is required")
	@OneToOne
	@JoinColumn(name = "assignment_id", nullable = false, unique = true, foreignKey = @ForeignKey(name = "fk_resolution_assignment"))
	private Assignment assignment;

	@NotNull(message = "Complaint is required")
	@ManyToOne
	@JoinColumn(name = "complaint_id", nullable = false, foreignKey = @ForeignKey(name = "fk_resolution_complaint"))
	private Complaint complaint;

	@NotNull(message = "Completion notes are required")
	@Column(nullable = false, columnDefinition = "TEXT")
	private String completionNotes;

	@Column(columnDefinition = "TEXT")
	private String adminNotes;

	@Column(length = 1000)
	private String rejectionReason;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	@Builder.Default
	private ReviewStatus reviewStatus = ReviewStatus.PENDING;

	@Column(nullable = false, updatable = false)
	@Builder.Default
	private Instant submittedAt = Instant.now();

	@Column
	private Instant reviewedAt;

	public boolean isPending() {
		return reviewStatus == ReviewStatus.PENDING;
	}

	public boolean isApproved() {
		return reviewStatus == ReviewStatus.APPROVED;
	}

	public boolean isRejected() {
		return reviewStatus == ReviewStatus.REJECTED;
	}
}
