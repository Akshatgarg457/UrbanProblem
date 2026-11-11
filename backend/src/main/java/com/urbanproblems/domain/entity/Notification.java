package com.urbanproblems.domain.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * In-app notification. Recipients are keyed by (recipientId, recipientRole) because ids are only
 * unique within a user table — the same identity model {@code AccessRules} uses.
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotNull(message = "Recipient is required")
	@Column(nullable = false)
	private Long recipientId;

	@NotBlank(message = "Recipient role is required")
	@Column(nullable = false, length = 32)
	private String recipientRole;

	@NotBlank(message = "Title is required")
	@Size(max = 200, message = "Title must not exceed 200 characters")
	@Column(nullable = false, length = 200)
	private String title;

	@NotBlank(message = "Message is required")
	@Size(max = 1000, message = "Message must not exceed 1000 characters")
	@Column(nullable = false, length = 1000)
	private String message;

	@Size(max = 36, message = "Complaint public id must not exceed 36 characters")
	@Column(length = 36)
	private String complaintPublicId;

	@Column(name = "is_read", nullable = false)
	@Builder.Default
	private boolean read = false;

	@Column(nullable = false, updatable = false)
	@Builder.Default
	private Instant createdAt = Instant.now();
}
