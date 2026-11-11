package com.urbanproblems.domain.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.urbanproblems.domain.enums.DepartmentStatus;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "departments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Department {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank(message = "Department name is required")
	@Size(min = 1, max = 100, message = "Department name must be between 1 and 100 characters")
	@Column(unique = true, nullable = false)
	private String name;

	@Size(max = 500, message = "Description must not exceed 500 characters")
	@Column(length = 500)
	private String description;

	@NotBlank(message = "Department head email is required")
	@Column(nullable = false)
	private String headEmail;

	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	@Builder.Default
	private DepartmentStatus status = DepartmentStatus.ACTIVE;

	@Column(nullable = false, updatable = false)
	@Builder.Default
	private Instant createdAt = Instant.now();

	@Column(nullable = false)
	@Builder.Default
	private Instant updatedAt = Instant.now();

	@JsonIgnore
	@OneToMany(mappedBy = "department", cascade = CascadeType.ALL, orphanRemoval = false)
	@Builder.Default
	private List<Worker> workers = new ArrayList<>();

	@JsonIgnore
	@OneToMany(mappedBy = "department", cascade = CascadeType.ALL, orphanRemoval = false)
	@Builder.Default
	private List<Complaint> complaints = new ArrayList<>();

	@jakarta.persistence.PreUpdate
	void touch() {
		this.updatedAt = java.time.Instant.now();
	}
}
