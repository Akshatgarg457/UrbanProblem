package com.urbanproblems.domain.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.urbanproblems.domain.enums.UserStatus;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "workers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Worker {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank(message = "Email is required")
	@Email(message = "Email must be valid")
	@Column(unique = true, nullable = false)
	private String email;

	@NotBlank(message = "First name is required")
	@Size(min = 1, max = 50, message = "First name must be between 1 and 50 characters")
	@Column(nullable = false)
	private String firstName;

	@NotBlank(message = "Last name is required")
	@Size(min = 1, max = 50, message = "Last name must be between 1 and 50 characters")
	@Column(nullable = false)
	private String lastName;

	@NotBlank(message = "Phone number is required")
	@Size(min = 10, max = 20, message = "Phone number must be between 10 and 20 characters")
	@Column(nullable = false)
	private String phoneNumber;

	@JsonIgnore
	@NotBlank(message = "Password hash is required")
	@Column(nullable = false, name = "password_hash")
	private String passwordHash;

	@NotNull(message = "Department is required")
	@ManyToOne
	@JoinColumn(name = "department_id", nullable = false, foreignKey = @ForeignKey(name = "fk_worker_department"))
	private Department department;

	/** Saved base location, picked on the profile map or entered manually. */
	@Size(max = 255)
	@Column(name = "address")
	private String address;

	@Size(max = 100)
	@Column(name = "city")
	private String city;

	@Size(max = 100)
	@Column(name = "state")
	private String state;

	@Size(max = 20)
	@Column(name = "postal_code")
	private String postalCode;

	@Column(name = "latitude")
	private Double latitude;

	@Column(name = "longitude")
	private Double longitude;

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

	@JsonIgnore
	@OneToMany(mappedBy = "assignedWorker", cascade = CascadeType.ALL, orphanRemoval = false)
	@Builder.Default
	private List<Assignment> assignments = new ArrayList<>();

	@PreUpdate
	void touch() {
		this.updatedAt = Instant.now();
	}

	public String getFullName() {
		return firstName + " " + lastName;
	}
}
