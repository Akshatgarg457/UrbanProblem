package com.urbanproblems.domain.entity;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnore;

import com.urbanproblems.domain.enums.AssignmentStatus;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "assignments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Assignment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotNull(message = "Complaint is required")
	@ManyToOne
	@JoinColumn(name = "complaint_id", nullable = false, foreignKey = @ForeignKey(name = "fk_assignment_complaint"))
	private Complaint complaint;

	@NotNull(message = "Assigned worker is required")
	@ManyToOne
	@JoinColumn(name = "worker_id", nullable = false, foreignKey = @ForeignKey(name = "fk_assignment_worker"))
	private Worker assignedWorker;

	@Column(length = 500)
	private String assignmentNotes;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	@Builder.Default
	private AssignmentStatus status = AssignmentStatus.ASSIGNED;

	@Column(nullable = false, updatable = false)
	@Builder.Default
	private Instant createdAt = Instant.now();

	@Column(nullable = false)
	@Builder.Default
	private Instant updatedAt = Instant.now();

	@JsonIgnore
	@OneToOne(mappedBy = "assignment", cascade = CascadeType.ALL, orphanRemoval = true)
	private Resolution resolution;

	@PreUpdate
	void touch() {
		this.updatedAt = Instant.now();
	}
}
