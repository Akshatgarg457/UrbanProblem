package com.urbanproblems.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.urbanproblems.domain.entity.Assignment;
import com.urbanproblems.domain.entity.Complaint;
import com.urbanproblems.domain.entity.Worker;
import com.urbanproblems.domain.enums.AssignmentStatus;
import com.urbanproblems.domain.enums.ComplaintStatus;
import com.urbanproblems.dto.AssignmentRequestDto;
import com.urbanproblems.dto.AssignmentResponseDto;
import com.urbanproblems.exception.ResourceNotFoundException;
import com.urbanproblems.repository.AssignmentRepository;
import com.urbanproblems.repository.WorkerRepository;
import com.urbanproblems.security.AccessRules;
import com.urbanproblems.security.UserPrincipal;

@Service
@Transactional
public class AssignmentService {

	private final AssignmentRepository assignmentRepository;
	private final WorkerRepository workerRepository;
	private final ComplaintService complaintService;
	private final ValidationService validationService;
	private final NotificationService notificationService;

	public AssignmentService(AssignmentRepository assignmentRepository,
			WorkerRepository workerRepository,
			ComplaintService complaintService,
			ValidationService validationService,
			NotificationService notificationService) {
		this.assignmentRepository = assignmentRepository;
		this.workerRepository = workerRepository;
		this.complaintService = complaintService;
		this.validationService = validationService;
		this.notificationService = notificationService;
	}

	/** Only the department admin of the complaint's department may assign it. */
	public AssignmentResponseDto assignComplaintToWorker(Long complaintId, AssignmentRequestDto requestDto,
			UserPrincipal admin) {
		Complaint complaint = complaintService.findComplaint(complaintId);
		AccessRules.requireDepartmentAdmin(admin, complaint.getDepartment());

		Worker worker = workerRepository.findById(requestDto.getWorkerId())
				.orElseThrow(() -> new ResourceNotFoundException("Worker not found"));

		validationService.validateWorkerAssignment(worker, complaint);

		Assignment assignment = assignmentRepository.save(Assignment.builder()
				.complaint(complaint)
				.assignedWorker(worker)
				.assignmentNotes(requestDto.getAssignmentNotes())
				.status(AssignmentStatus.ASSIGNED)
				.build());

		complaintService.updateComplaintStatus(
				complaintId,
				ComplaintStatus.ASSIGNED,
				"Assigned to " + worker.getFullName(),
				admin.getRole());

		notificationService.notifyWorkerTaskAssigned(complaint, worker);

		return mapToResponseDto(assignment);
	}

	/** Assigned worker moves the task from ASSIGNED to IN_PROGRESS. */
	public AssignmentResponseDto startWork(Long assignmentId, UserPrincipal worker) {
		Assignment assignment = findAssignment(assignmentId);
		requireAssignedWorker(assignment, worker);

		if (assignment.getStatus() != AssignmentStatus.ASSIGNED) {
			throw new IllegalStateException("Work can only be started from ASSIGNED. Current: "
					+ assignment.getStatus());
		}

		assignment.setStatus(AssignmentStatus.IN_PROGRESS);
		assignmentRepository.save(assignment);

		complaintService.updateComplaintStatus(
				assignment.getComplaint().getId(),
				ComplaintStatus.IN_PROGRESS,
				"Work in progress",
				"WORKER");

		notificationService.notifyDepartmentAdminsWorkStarted(assignment.getComplaint());

		return mapToResponseDto(assignment);
	}

	/** Visible to the assigned worker and to the complaint's department admin. */
	public Page<AssignmentResponseDto> getWorkerAssignments(Long workerId, Pageable pageable) {
		return assignmentRepository.findByAssignedWorkerId(workerId, pageable).map(this::mapToResponseDto);
	}

	public Page<AssignmentResponseDto> getWorkerActiveAssignments(Long workerId, Pageable pageable) {
		return assignmentRepository.findByAssignedWorkerIdAndStatus(workerId, AssignmentStatus.ASSIGNED, pageable)
				.map(this::mapToResponseDto);
	}

	public AssignmentResponseDto getAssignment(Long assignmentId, UserPrincipal user) {
		Assignment assignment = findAssignment(assignmentId);
		if (AccessRules.WORKER.equals(user.getRole())) {
			requireAssignedWorker(assignment, user);
		} else {
			AccessRules.requireDepartmentScope(user, assignment.getComplaint().getDepartment());
		}
		return mapToResponseDto(assignment);
	}

	public long getWorkerActiveAssignmentCount(Long workerId) {
		return assignmentRepository.countByAssignedWorkerIdAndStatus(workerId, AssignmentStatus.ASSIGNED);
	}

	Assignment findAssignment(Long assignmentId) {
		return assignmentRepository.findById(assignmentId)
				.orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));
	}

	private void requireAssignedWorker(Assignment assignment, UserPrincipal user) {
		if (!AccessRules.WORKER.equals(user.getRole())
				|| !assignment.getAssignedWorker().getId().equals(user.getId())) {
			throw new AccessDeniedException("Access denied");
		}
	}

	AssignmentResponseDto mapToResponseDto(Assignment assignment) {
		return AssignmentResponseDto.builder()
				.id(assignment.getId())
				.complaintId(assignment.getComplaint().getId())
				.complaintPublicId(assignment.getComplaint().getPublicId())
				.complaintTitle(assignment.getComplaint().getTitle())
				.workerId(assignment.getAssignedWorker().getId())
				.workerName(assignment.getAssignedWorker().getFullName())
				.assignmentNotes(assignment.getAssignmentNotes())
				.status(assignment.getStatus())
				.createdAt(assignment.getCreatedAt())
				.updatedAt(assignment.getUpdatedAt())
				.build();
	}
}
