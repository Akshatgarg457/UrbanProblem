package com.urbanproblems.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "complaint_evidence")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplaintEvidence {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "complaint_id", nullable = false)
	private Complaint complaint;

	@Column(nullable = false)
	private String fileName;

	@Column(nullable = false)
	private String fileType;

	@Column(nullable = false)
	private Long fileSize;

	@Column(nullable = false)
	private String filePath;

	/** Uploader role, e.g. CITIZEN or WORKER. */
	@Column(nullable = false)
	private String uploadedBy;

	/** Uploader id within the table implied by uploadedBy. Nullable for rows created before this column. */
	@Column(name = "uploaded_by_id")
	private Long uploadedById;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@PrePersist
	protected void onCreate() {
		this.createdAt = Instant.now();
	}
}
