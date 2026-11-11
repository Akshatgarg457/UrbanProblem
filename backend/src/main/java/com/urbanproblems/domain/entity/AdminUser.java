package com.urbanproblems.domain.entity;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.urbanproblems.domain.enums.UserRole;
import com.urbanproblems.domain.enums.UserStatus;

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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Department and system administrators. Role is DEPARTMENT_ADMIN or SYSTEM_ADMIN.
 * department is required for DEPARTMENT_ADMIN and must be null for SYSTEM_ADMIN.
 */
@Entity
@Table(name = "admin_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUser {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true, nullable = false)
	private String email;

	@Column(nullable = false)
	private String firstName;

	@Column(nullable = false)
	private String lastName;

	@JsonIgnore
	@Column(nullable = false, name = "password_hash")
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private UserRole role;

	@ManyToOne
	@JoinColumn(name = "department_id", foreignKey = @ForeignKey(name = "fk_admin_department"))
	private Department department;

	@Size(max = 255)
	@Column(name = "security_question")
	private String securityQuestion;

	@JsonIgnore
	@Size(max = 255)
	@Column(name = "security_answer_hash")
	private String securityAnswerHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	@Builder.Default
	private UserStatus status = UserStatus.ACTIVE;

	@Column(nullable = false, updatable = false)
	@Builder.Default
	private Instant createdAt = Instant.now();

	@Column(nullable = false)
	@Builder.Default
	private Instant updatedAt = Instant.now();

	@PreUpdate
	void touch() {
		this.updatedAt = Instant.now();
	}
}
