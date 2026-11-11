package com.urbanproblems.domain.entity;

import java.time.Instant;

import com.urbanproblems.domain.enums.ComplaintStatus;

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
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "complaint_status_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplaintStatusHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotNull(message = "Complaint is required")
	@ManyToOne
	@JoinColumn(name = "complaint_id", nullable = false, foreignKey = @ForeignKey(name = "fk_status_history_complaint"))
	private Complaint complaint;

	/** Null only for the initial SUBMITTED record. */
	@Enumerated(EnumType.STRING)
	@Column(nullable = true)
	private ComplaintStatus previousStatus;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ComplaintStatus newStatus;

	@Column(length = 500)
	private String reason;

	@Column(nullable = false, updatable = false)
	@Builder.Default
	private Instant changedAt = Instant.now();

	@Column(length = 100)
	private String changedByRole;
}
