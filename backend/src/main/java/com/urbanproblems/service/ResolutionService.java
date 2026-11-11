package com.urbanproblems.service;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.urbanproblems.domain.entity.Assignment;
import com.urbanproblems.domain.entity.Complaint;
import com.urbanproblems.domain.entity.Resolution;
import com.urbanproblems.domain.entity.Resolution.ReviewStatus;
import com.urbanproblems.domain.enums.AssignmentStatus;
import com.urbanproblems.domain.enums.ComplaintStatus;
import com.urbanproblems.dto.ResolutionResponseDto;
import com.urbanproblems.dto.ResolutionReviewDto;
import com.urbanproblems.dto.ResolutionSubmitDto;
import com.urbanproblems.exception.ResourceNotFoundException;
import com.urbanproblems.repository.AssignmentRepository;
import com.urbanproblems.repository.ComplaintEvidenceRepository;
import com.urbanproblems.repository.ResolutionRepository;
import com.urbanproblems.security.AccessRules;
import com.urbanproblems.security.UserPrincipal;

@Service
@Transactional
public class ResolutionService {

	private final ResolutionRepository resolutionRepository;
	private final AssignmentRepository assignmentRepository;
	private final ComplaintEvidenceRepository evidenceRepository;
	private final ComplaintService complaintService;
	private final ValidationService validationService;
	private final NotificationService notificationService;

	public ResolutionService(ResolutionRepository resolutionRepository,
			AssignmentRepository assignmentRepository,
			ComplaintEvidenceRepository evidenceRepository,
			ComplaintService complaintService,
			ValidationService validationService,
			NotificationService notificationService) {
		this.resolutionRepository = resolutionRepository;
		this.assignmentRepository = assignmentRepository;
		this.evidenceRepository = evidenceRepository;
		this.complaintService = complaintService;
		this.validationService = validationService;
		this.notificationService = notificationService;
	}

	/**
	 * Assigned worker submits completed work. Requires at least one piece of evidence uploaded by that
	 * worker on this complaint. After a rejection the same record is reused for rework, and the
	 * rejection reason stays in complaint status history.
	 */
	public ResolutionResponseDto submitResolution(Long assignmentId, ResolutionSubmitDto submitDto,
			UserPrincipal worker) {
		Assignment assignment = assignmentRepository.findById(assignmentId)
				.orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));

		if (!assignment.getAssignedWorker().getId().equals(worker.getId())
				|| !AccessRules.WORKER.equals(worker.getRole())) {
			throw new AccessDeniedException("Access denied");
		}

		validationService.validateResolutionSubmission(assignment);

		Long complaintId = assignment.getComplaint().getId();
		boolean hasEvidence = evidenceRepository.existsByComplaintIdAndUploadedByAndUploadedById(
				complaintId, AccessRules.WORKER, worker.getId());
		if (!hasEvidence) {
			throw new IllegalStateException("Upload at least one piece of evidence before submitting.");
		}

		Resolution resolution = assignment.getResolution();
		if (resolution == null) {
			resolution = Resolution.builder()
					.assignment(assignment)
					.complaint(assignment.getComplaint())
					.completionNotes(submitDto.getCompletionNotes())
					.reviewStatus(ReviewStatus.PENDING)
					.build();
		} else {
			resolution.setCompletionNotes(submitDto.getCompletionNotes());
			resolution.setReviewStatus(ReviewStatus.PENDING);
			resolution.setReviewedAt(null);
			resolution.setRejectionReason(null);
			resolution.setAdminNotes(null);
			resolution.setSubmittedAt(Instant.now());
		}
		resolution = resolutionRepository.save(resolution);

		assignment.setStatus(AssignmentStatus.SUBMITTED_FOR_REVIEW);
		assignmentRepository.save(assignment);

		complaintService.updateComplaintStatus(complaintId, ComplaintStatus.PENDING_VERIFICATION,
				"Resolution submitted for review", "WORKER");

		notificationService.notifyDepartmentAdminsResolutionSubmitted(assignment.getComplaint());

		return mapToResponseDto(resolution);
	}

	/** Only the department admin of the complaint's department may approve. */
	public ResolutionResponseDto approveResolution(Long resolutionId, ResolutionReviewDto reviewDto,
			UserPrincipal reviewer) {
		if (reviewDto.getReviewStatus() != ReviewStatus.APPROVED) {
			throw new IllegalArgumentException("reviewStatus must be APPROVED for this action.");
		}
		Resolution resolution = findResolutionForReview(resolutionId, reviewer);

		Assignment assignment = resolution.getAssignment();
		resolution.setReviewStatus(ReviewStatus.APPROVED);
		resolution.setAdminNotes(reviewDto.getAdminNotes());
		resolution.setReviewedAt(Instant.now());
		resolution = resolutionRepository.save(resolution);

		assignment.setStatus(AssignmentStatus.APPROVED);
		assignmentRepository.save(assignment);

		Long complaintId = resolution.getComplaint().getId();
		complaintService.updateComplaintStatus(complaintId, ComplaintStatus.APPROVED,
				"Resolution approved", reviewer.getRole());
		complaintService.updateComplaintStatus(complaintId, ComplaintStatus.RESOLVED,
				"Resolution approved", reviewer.getRole());

		notificationService.notifyResolutionApproved(resolution.getComplaint(), assignment.getAssignedWorker());

		return mapToResponseDto(resolution);
	}

	/** Rejection requires a reason and sends the complaint back to IN_PROGRESS for rework. */
	public ResolutionResponseDto rejectResolution(Long resolutionId, ResolutionReviewDto reviewDto,
			UserPrincipal reviewer) {
		if (reviewDto.getReviewStatus() != ReviewStatus.REJECTED) {
			throw new IllegalArgumentException("reviewStatus must be REJECTED for this action.");
		}
		if (reviewDto.getRejectionReason() == null || reviewDto.getRejectionReason().isBlank()) {
			throw new IllegalArgumentException("A rejection reason is required.");
		}
		Resolution resolution = findResolutionForReview(resolutionId, reviewer);

		Assignment assignment = resolution.getAssignment();
		resolution.setReviewStatus(ReviewStatus.REJECTED);
		resolution.setRejectionReason(reviewDto.getRejectionReason());
		resolution.setAdminNotes(reviewDto.getAdminNotes());
		resolution.setReviewedAt(Instant.now());
		resolution = resolutionRepository.save(resolution);

		assignment.setStatus(AssignmentStatus.IN_PROGRESS);
		assignmentRepository.save(assignment);

		complaintService.updateComplaintStatus(resolution.getComplaint().getId(), ComplaintStatus.IN_PROGRESS,
				"Resolution rejected: " + reviewDto.getRejectionReason(), reviewer.getRole());

		notificationService.notifyResolutionRejected(
				resolution.getComplaint(), assignment.getAssignedWorker(), reviewDto.getRejectionReason());

		return mapToResponseDto(resolution);
	}

	/** Resolution visible to the assigned worker or to the complaint's department admin. */
	public ResolutionResponseDto getResolution(Long resolutionId, UserPrincipal user) {
		Resolution resolution = resolutionRepository.findById(resolutionId)
				.orElseThrow(() -> new ResourceNotFoundException("Resolution not found"));
		AccessRules.requireComplaintVisible(user, resolution.getComplaint(), assignmentRepository);
		return mapToResponseDto(resolution);
	}

	/** Latest submission for one assignment, for worker feedback and rework context. */
	@Transactional(readOnly = true)
	public ResolutionResponseDto getLatestForAssignment(Long assignmentId, UserPrincipal user) {
		Assignment assignment = assignmentRepository.findById(assignmentId)
				.orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));
		if (AccessRules.WORKER.equals(user.getRole())) {
			if (!assignment.getAssignedWorker().getId().equals(user.getId())) {
				throw new AccessDeniedException("Access denied");
			}
		} else {
			AccessRules.requireDepartmentScope(user, assignment.getComplaint().getDepartment());
		}
		return resolutionRepository.findByAssignmentId(assignmentId)
				.map(this::mapToResponseDto)
				.orElseThrow(() -> new ResourceNotFoundException("No resolution has been submitted yet"));
	}

	/** Pending reviews for the caller's department. System admins see all departments. */
	public Page<ResolutionResponseDto> getPendingResolutions(UserPrincipal admin, Pageable pageable) {
		if (AccessRules.SYSTEM_ADMIN.equals(admin.getRole())) {
			return resolutionRepository.findByReviewStatus(ReviewStatus.PENDING, pageable)
					.map(this::mapToResponseDto);
		}
		if (admin.getDepartmentId() == null) {
			throw new AccessDeniedException("Access denied");
		}
		return resolutionRepository.findByReviewStatusAndComplaintDepartmentId(
				ReviewStatus.PENDING, admin.getDepartmentId(), pageable).map(this::mapToResponseDto);
	}

	/** Approved resolutions for a complaint, visible to anyone allowed to see the complaint. */
	public Page<ResolutionResponseDto> getResolutionsByComplaint(Long complaintId, UserPrincipal user,
			Pageable pageable) {
		Complaint complaint = complaintService.findComplaint(complaintId);
		AccessRules.requireComplaintVisible(user, complaint, assignmentRepository);
		return resolutionRepository.findByComplaintIdAndReviewStatus(complaintId, ReviewStatus.APPROVED, pageable)
				.map(this::mapToResponseDto);
	}

	private Resolution findResolutionForReview(Long resolutionId, UserPrincipal reviewer) {
		Resolution resolution = resolutionRepository.findById(resolutionId)
				.orElseThrow(() -> new ResourceNotFoundException("Resolution not found"));
		AccessRules.requireDepartmentAdmin(reviewer, resolution.getComplaint().getDepartment());
		validationService.validateResolutionReview(resolution, reviewer.getRole(), reviewer.getId());
		return resolution;
	}

	ResolutionResponseDto mapToResponseDto(Resolution resolution) {
		return ResolutionResponseDto.builder()
				.id(resolution.getId())
				.assignmentId(resolution.getAssignment().getId())
				.complaintId(resolution.getComplaint().getId())
				.complaintPublicId(resolution.getComplaint().getPublicId())
				.completionNotes(resolution.getCompletionNotes())
				.adminNotes(resolution.getAdminNotes())
				.rejectionReason(resolution.getRejectionReason())
				.reviewStatus(resolution.getReviewStatus())
				.submittedAt(resolution.getSubmittedAt())
				.reviewedAt(resolution.getReviewedAt())
				.build();
	}
}
