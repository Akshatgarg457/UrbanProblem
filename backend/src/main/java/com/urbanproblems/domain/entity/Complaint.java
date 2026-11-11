package com.urbanproblems.domain.entity;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.urbanproblems.domain.enums.ComplaintCategory;
import com.urbanproblems.domain.enums.ComplaintStatus;

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
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "complaints", uniqueConstraints = {
		@UniqueConstraint(columnNames = "public_id", name = "uk_complaint_public_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Complaint {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@NotBlank(message = "Public ID is required")
	@Column(nullable = false, unique = true, length = 36)
	@Builder.Default
	private String publicId = UUID.randomUUID().toString();

	@NotBlank(message = "Title is required")
	@Size(min = 5, max = 200, message = "Title must be between 5 and 200 characters")
	@Column(nullable = false)
	private String title;

	@NotBlank(message = "Description is required")
	@Size(min = 10, max = 2000, message = "Description must be between 10 and 2000 characters")
	@Column(nullable = false, columnDefinition = "TEXT")
	private String description;

	@NotNull(message = "Category is required")
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ComplaintCategory category;

	@NotBlank(message = "Location/Address is required")
	@Size(min = 5, max = 500, message = "Location must be between 5 and 500 characters")
	@Column(nullable = false)
	private String location;

	@JdbcTypeCode(SqlTypes.DECIMAL)
	@Column(precision = 10, scale = 8)
	private Double latitude;

	@JdbcTypeCode(SqlTypes.DECIMAL)
	@Column(precision = 11, scale = 8)
	private Double longitude;

	@NotNull(message = "Citizen is required")
	@ManyToOne
	@JoinColumn(name = "citizen_id", nullable = false, foreignKey = @ForeignKey(name = "fk_complaint_citizen"))
	private Citizen citizen;

	@NotNull(message = "Department is required")
	@ManyToOne
	@JoinColumn(name = "department_id", nullable = false, foreignKey = @ForeignKey(name = "fk_complaint_department"))
	private Department department;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	@Builder.Default
	private ComplaintStatus status = ComplaintStatus.SUBMITTED;

	@Column(nullable = false, updatable = false)
	@Builder.Default
	private Instant createdAt = Instant.now();

	@Column(nullable = false)
	@Builder.Default
	private Instant updatedAt = Instant.now();

	@JsonIgnore
	@OneToMany(mappedBy = "complaint", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private List<Assignment> assignments = new ArrayList<>();

	@JsonIgnore
	@OneToMany(mappedBy = "complaint", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private List<Resolution> resolutions = new ArrayList<>();

	@JsonIgnore
	@OneToMany(mappedBy = "complaint", cascade = CascadeType.ALL, orphanRemoval = true)
	@Builder.Default
	private List<ComplaintStatusHistory> statusHistory = new ArrayList<>();

	public boolean canBeAssigned() {
		return this.status == ComplaintStatus.SUBMITTED;
	}

	@PreUpdate
	void touch() {
		this.updatedAt = Instant.now();
	}
}
