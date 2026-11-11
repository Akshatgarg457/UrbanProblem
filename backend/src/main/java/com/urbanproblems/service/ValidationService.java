package com.urbanproblems.service;

import org.springframework.stereotype.Service;

import com.urbanproblems.domain.entity.Assignment;
import com.urbanproblems.domain.entity.Complaint;
import com.urbanproblems.domain.entity.Department;
import com.urbanproblems.domain.entity.Resolution;
import com.urbanproblems.domain.entity.Worker;
import com.urbanproblems.domain.enums.AssignmentStatus;
import com.urbanproblems.domain.enums.ComplaintStatus;
import com.urbanproblems.domain.enums.UserStatus;
import com.urbanproblems.repository.AssignmentRepository;

@Service
public class ValidationService {

	private final AssignmentRepository assignmentRepository;

	public ValidationService(AssignmentRepository assignmentRepository) {
		this.assignmentRepository = assignmentRepository;
	}

	/**
	 * Validates that a worker can be assigned to a complaint
	 */
	public void validateWorkerAssignment(Worker worker, Complaint complaint) {
		// Worker must be active
		if (worker.getStatus() != UserStatus.ACTIVE) {
			throw new IllegalStateException("Worker is not active. Cannot assign task.");
		}

		// Worker must belong to the complaint's department
		if (!worker.getDepartment().getId().equals(complaint.getDepartment().getId())) {
			throw new IllegalStateException("Worker does not belong to the complaint's department.");
		}

		// Complaint must be eligible for assignment
		if (!complaint.canBeAssigned()) {
			throw new IllegalStateException(
					"Complaint cannot be assigned in current status: " + complaint.getStatus());
		}

		// Check if complaint already has active assignment
		assignmentRepository.findByComplaintId(complaint.getId()).ifPresent(existing -> {
			throw new IllegalStateException("Complaint already has an active assignment.");
		});
	}

	/**
	 * A worker may submit only while work is in progress. A new submission is allowed only after a
	 * rejection (rework); otherwise an existing resolution blocks it.
	 */
	public void validateResolutionSubmission(Assignment assignment) {
		if (assignment.getStatus() != AssignmentStatus.IN_PROGRESS) {
			throw new IllegalStateException(
					"Cannot submit resolution for assignment in status: " + assignment.getStatus());
		}
		Resolution existing = assignment.getResolution();
		if (existing != null && existing.getReviewStatus() != Resolution.ReviewStatus.REJECTED) {
			throw new IllegalStateException("Resolution already submitted for this assignment.");
		}
	}

	/**
	 * A resolution must be pending and cannot be reviewed by the worker who submitted it.
	 * Identity is compared by role as well as id, because user ids are only unique per table.
	 */
	public void validateResolutionReview(Resolution resolution, String reviewerRole, Long reviewerId) {
		if (!resolution.isPending()) {
			throw new IllegalStateException(
					"Resolution is not pending review. Current status: " + resolution.getReviewStatus());
		}
		Worker submittedBy = resolution.getAssignment().getAssignedWorker();
		if ("WORKER".equals(reviewerRole) && submittedBy != null && submittedBy.getId().equals(reviewerId)) {
			throw new IllegalStateException("A worker cannot review their own submitted resolution.");
		}
	}

	/**
	 * Validates that a department can be deactivated
	 */
	public void validateDepartmentDeactivation(Department department, long activeComplaintCount) {
		if (activeComplaintCount > 0) {
			throw new IllegalStateException(
					"Cannot deactivate department with active complaints. " +
							"Transfer or resolve " + activeComplaintCount + " complaint(s) first.");
		}
	}

	/**
	 * Validates that a worker can change departments
	 */
	public void validateWorkerDepartmentTransfer(Worker worker, Department newDepartment) {
		// Check for active assignments
		long activeAssignmentCount = assignmentRepository
				.countByAssignedWorkerIdAndStatus(worker.getId(), AssignmentStatus.ASSIGNED);

		if (activeAssignmentCount > 0) {
			throw new IllegalStateException(
					"Cannot transfer worker with active assignments. " +
							"Complete or reassign " + activeAssignmentCount + " task(s) first.");
		}
	}

	/**
	 * Validates worker is from same department as complaint for assignment
	 */
	public void validateDepartmentMatch(Worker worker, Complaint complaint) {
		if (!worker.getDepartment().getId().equals(complaint.getDepartment().getId())) {
			throw new IllegalStateException(
					"Worker's department (" + worker.getDepartment().getName() + ") does not match " +
							"complaint's department (" + complaint.getDepartment().getName() + ").");
		}
	}

	/**
	 * Checks a requested complaint status change against the lifecycle in {@link ComplaintStatus}.
	 */
	public void validateStatusTransition(Complaint complaint, ComplaintStatus newStatus) {
		ComplaintStatus currentStatus = complaint.getStatus();
		if (!currentStatus.canTransitionTo(newStatus)) {
			throw new IllegalStateException(
					"Invalid status transition from " + currentStatus + " to " + newStatus);
		}
	}

	/**
	 * Validates email format (basic)
	 */
	public void validateEmail(String email) {
		if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
			throw new IllegalArgumentException("Invalid email format: " + email);
		}
	}

	/**
	 * Validates password meets requirements
	 */
	public void validatePassword(String password) {
		if (password.length() < 8 || password.length() > 128) {
			throw new IllegalArgumentException("Password must be 8-128 characters");
		}
	}

	/**
	 * Validates phone number format
	 */
	public void validatePhoneNumber(String phoneNumber) {
		if (!phoneNumber.matches("^[0-9\\-\\+\\s()]{10,20}$")) {
			throw new IllegalArgumentException("Invalid phone number format: " + phoneNumber);
		}
	}

	/**
	 * Validates location string
	 */
	public void validateLocation(String location) {
		if (location == null || location.trim().isEmpty()) {
			throw new IllegalArgumentException("Location cannot be empty");
		}
		if (location.length() < 5) {
			throw new IllegalArgumentException("Location must be at least 5 characters");
		}
		if (location.length() > 500) {
			throw new IllegalArgumentException("Location must not exceed 500 characters");
		}
	}

	/**
	 * Validates coordinates if provided
	 */
	public void validateCoordinates(Double latitude, Double longitude) {
		if (latitude != null) {
			if (latitude < -90 || latitude > 90) {
				throw new IllegalArgumentException("Latitude must be between -90 and 90");
			}
		}
		if (longitude != null) {
			if (longitude < -180 || longitude > 180) {
				throw new IllegalArgumentException("Longitude must be between -180 and 180");
			}
		}
	}
}
