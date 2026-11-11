package com.urbanproblems.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.urbanproblems.domain.entity.Citizen;
import com.urbanproblems.domain.entity.Complaint;
import com.urbanproblems.domain.entity.ComplaintStatusHistory;
import com.urbanproblems.domain.entity.Department;
import com.urbanproblems.domain.enums.ComplaintCategory;
import com.urbanproblems.domain.enums.ComplaintStatus;
import com.urbanproblems.dto.ComplaintHistoryResponseDto;
import com.urbanproblems.dto.ComplaintRequestDto;
import com.urbanproblems.dto.ComplaintResponseDto;
import com.urbanproblems.exception.ResourceNotFoundException;
import com.urbanproblems.repository.AssignmentRepository;
import com.urbanproblems.repository.CitizenRepository;
import com.urbanproblems.repository.ComplaintRepository;
import com.urbanproblems.repository.ComplaintStatusHistoryRepository;
import com.urbanproblems.repository.DepartmentRepository;
import com.urbanproblems.security.AccessRules;
import com.urbanproblems.security.UserPrincipal;

@Service
@Transactional
public class ComplaintService {

	private static final java.util.List<ComplaintStatus> OPEN_STATUSES = java.util.List.of(
			ComplaintStatus.SUBMITTED,
			ComplaintStatus.ASSIGNED,
			ComplaintStatus.IN_PROGRESS,
			ComplaintStatus.PENDING_VERIFICATION,
			ComplaintStatus.APPROVED);

	private final ComplaintRepository complaintRepository;
	private final CitizenRepository citizenRepository;
	private final DepartmentRepository departmentRepository;
	private final ComplaintStatusHistoryRepository statusHistoryRepository;
	private final AssignmentRepository assignmentRepository;
	private final NotificationService notificationService;

	public ComplaintService(ComplaintRepository complaintRepository,
			CitizenRepository citizenRepository,
			DepartmentRepository departmentRepository,
			ComplaintStatusHistoryRepository statusHistoryRepository,
			AssignmentRepository assignmentRepository,
			NotificationService notificationService) {
		this.complaintRepository = complaintRepository;
		this.citizenRepository = citizenRepository;
		this.departmentRepository = departmentRepository;
		this.statusHistoryRepository = statusHistoryRepository;
		this.assignmentRepository = assignmentRepository;
		this.notificationService = notificationService;
	}

	public ComplaintResponseDto createComplaint(Long citizenId, ComplaintRequestDto requestDto) {
		Citizen citizen = citizenRepository.findById(citizenId)
				.orElseThrow(() -> new ResourceNotFoundException("Citizen not found"));

		if ((requestDto.getLatitude() == null) != (requestDto.getLongitude() == null)) {
			throw new IllegalArgumentException("Provide both latitude and longitude, or neither.");
		}
		if (requestDto.getLatitude() != null && (requestDto.getLatitude() < -90 || requestDto.getLatitude() > 90
				|| requestDto.getLongitude() < -180 || requestDto.getLongitude() > 180)) {
			throw new IllegalArgumentException("Coordinates are outside the valid range.");
		}

		String title = requestDto.getTitle().trim();
		String location = requestDto.getLocation().trim();
		if (complaintRepository.existsByCitizenIdAndTitleIgnoreCaseAndLocationIgnoreCaseAndCreatedAtGreaterThanEqualAndStatusIn(
				citizenId, title, location, java.time.Instant.now().minusSeconds(600), OPEN_STATUSES)) {
			throw new IllegalStateException(
					"You already submitted a complaint with the same title and location in the last 10 minutes.");
		}

		Department department = resolveDepartmentForCategory(requestDto.getCategory());

		Complaint complaint = Complaint.builder()
				.title(title)
				.description(requestDto.getDescription())
				.category(requestDto.getCategory())
				.location(location)
				.latitude(requestDto.getLatitude())
				.longitude(requestDto.getLongitude())
				.citizen(citizen)
				.department(department)
				.status(ComplaintStatus.SUBMITTED)
				.build();

		complaint = complaintRepository.save(complaint);

		statusHistoryRepository.save(ComplaintStatusHistory.builder()
				.complaint(complaint)
				.previousStatus(null)
				.newStatus(ComplaintStatus.SUBMITTED)
				.reason("Initial submission")
				.changedByRole("CITIZEN")
				.build());

		notificationService.notifyDepartmentAdminsNewComplaint(complaint);
		notificationService.notifyCitizenComplaintSubmitted(complaint);

		return mapToResponseDto(complaint);
	}

	/** Returns the complaint only if the caller is allowed to see it. */
	@Transactional(readOnly = true)
	public ComplaintResponseDto getComplaintByPublicId(String publicId, UserPrincipal user) {
		Complaint complaint = complaintRepository.findByPublicId(publicId)
				.orElseThrow(() -> new ResourceNotFoundException("Complaint not found"));
		AccessRules.requireComplaintVisible(user, complaint, assignmentRepository);
		return mapToResponseDto(complaint);
	}

	/** Status-change history, newest first. Read access follows {@link AccessRules#requireComplaintVisible}. */
	@Transactional(readOnly = true)
	public java.util.List<ComplaintHistoryResponseDto> getComplaintHistory(String publicId, UserPrincipal user) {
		Complaint complaint = complaintRepository.findByPublicId(publicId)
				.orElseThrow(() -> new ResourceNotFoundException("Complaint not found"));
		AccessRules.requireComplaintVisible(user, complaint, assignmentRepository);
		return statusHistoryRepository.findByComplaintIdOrderByChangedAtDesc(complaint.getId()).stream()
				.map(h -> new ComplaintHistoryResponseDto(h.getId(), h.getPreviousStatus(), h.getNewStatus(),
						h.getChangedByRole(), h.getReason(), h.getChangedAt()))
				.toList();
	}

	/** Returns the complaint only if the caller is allowed to see it. */
	@Transactional(readOnly = true)
	public ComplaintResponseDto getComplaintById(Long id, UserPrincipal user) {
		Complaint complaint = findComplaint(id);
		AccessRules.requireComplaintVisible(user, complaint, assignmentRepository);
		return mapToResponseDto(complaint);
	}

	@Transactional(readOnly = true)
	public Page<ComplaintResponseDto> getComplaintsByCitizen(Long citizenId, Pageable pageable) {
		return complaintRepository.findByCitizenId(citizenId, pageable).map(this::mapToResponseDto);
	}

	/** Department admins may only list their own department. System admins may list any department. */
	@Transactional(readOnly = true)
	public Page<ComplaintResponseDto> getComplaintsByDepartment(Long departmentId, UserPrincipal user,
			Pageable pageable) {
		Department department = departmentRepository.findById(departmentId)
				.orElseThrow(() -> new ResourceNotFoundException("Department not found"));
		AccessRules.requireDepartmentScope(user, department);
		return complaintRepository.findByDepartmentId(departmentId, pageable).map(this::mapToResponseDto);
	}

	@Transactional(readOnly = true)
	public Page<ComplaintResponseDto> getComplaintsByStatus(ComplaintStatus status, Pageable pageable) {
		return complaintRepository.findByStatus(status, pageable).map(this::mapToResponseDto);
	}

	@Transactional(readOnly = true)
	public Page<ComplaintResponseDto> getComplaintsByCategory(ComplaintCategory category, Pageable pageable) {
		return complaintRepository.findByCategory(category, pageable).map(this::mapToResponseDto);
	}

	/**
	 * Single entry point for every complaint status change. Enforces the lifecycle in
	 * {@link ComplaintStatus#canTransitionTo(ComplaintStatus)} and records history.
	 */
	public void updateComplaintStatus(Long complaintId, ComplaintStatus newStatus, String reason,
			String changedByRole) {
		Complaint complaint = findComplaint(complaintId);

		ComplaintStatus previousStatus = complaint.getStatus();
		if (!previousStatus.canTransitionTo(newStatus)) {
			throw new IllegalStateException(
					"Invalid status transition from " + previousStatus + " to " + newStatus);
		}

		complaint.setStatus(newStatus);
		complaintRepository.save(complaint);

		statusHistoryRepository.save(ComplaintStatusHistory.builder()
				.complaint(complaint)
				.previousStatus(previousStatus)
				.newStatus(newStatus)
				.reason(reason)
				.changedByRole(changedByRole)
				.build());

		notificationService.notifyCitizenStatusChanged(complaint, newStatus);
	}

	/** Loads a complaint entity for internal use. Callers must apply their own access checks. */
	@Transactional(readOnly = true)
	public Complaint findComplaint(Long complaintId) {
		return complaintRepository.findById(complaintId)
				.orElseThrow(() -> new ResourceNotFoundException("Complaint not found"));
	}

	private Department resolveDepartmentForCategory(ComplaintCategory category) {
		return departmentRepository.findByName(category.getDepartmentName())
				.orElseThrow(() -> new IllegalStateException("No department configured for category: " + category));
	}

	ComplaintResponseDto mapToResponseDto(Complaint complaint) {
		return ComplaintResponseDto.builder()
				.id(complaint.getId())
				.publicId(complaint.getPublicId())
				.title(complaint.getTitle())
				.description(complaint.getDescription())
				.category(complaint.getCategory())
				.location(complaint.getLocation())
				.latitude(complaint.getLatitude())
				.longitude(complaint.getLongitude())
				.citizenId(complaint.getCitizen().getId())
				.citizenName(complaint.getCitizen().getFullName())
				.departmentId(complaint.getDepartment().getId())
				.departmentName(complaint.getDepartment().getName())
				.status(complaint.getStatus())
				.createdAt(complaint.getCreatedAt())
				.updatedAt(complaint.getUpdatedAt())
				.build();
	}
}
