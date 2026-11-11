package com.urbanproblems.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.urbanproblems.domain.entity.AdminUser;
import com.urbanproblems.domain.entity.Citizen;
import com.urbanproblems.domain.entity.Complaint;
import com.urbanproblems.domain.entity.Notification;
import com.urbanproblems.domain.entity.Worker;
import com.urbanproblems.domain.enums.ComplaintStatus;
import com.urbanproblems.domain.enums.UserRole;
import com.urbanproblems.dto.NotificationResponseDto;
import com.urbanproblems.exception.ResourceNotFoundException;
import com.urbanproblems.repository.AdminUserRepository;
import com.urbanproblems.repository.NotificationRepository;
import com.urbanproblems.security.AccessRules;
import com.urbanproblems.security.UserPrincipal;

import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
public class NotificationService {

	private final NotificationRepository notificationRepository;
	private final AdminUserRepository adminUserRepository;

	public NotificationService(NotificationRepository notificationRepository,
			AdminUserRepository adminUserRepository) {
		this.notificationRepository = notificationRepository;
		this.adminUserRepository = adminUserRepository;
	}

	// ---- In-app notifications ----

	/**
	 * Creates a single in-app notification. Never throws: a notification failure must not break
	 * the caller's main transaction.
	 */
	public void createNotification(Long recipientId, String recipientRole, String title, String message,
			String complaintPublicId) {
		try {
			notificationRepository.save(Notification.builder()
					.recipientId(recipientId)
					.recipientRole(recipientRole)
					.title(title)
					.message(message)
					.complaintPublicId(complaintPublicId)
					.build());
		} catch (Exception e) {
			log.warn("Failed to create notification for {} {}: {}", recipientRole, recipientId, e.getMessage());
		}
	}

	@Transactional(readOnly = true)
	public Page<NotificationResponseDto> getMyNotifications(UserPrincipal principal, Pageable pageable) {
		return notificationRepository
				.findByRecipientIdAndRecipientRoleOrderByCreatedAtDesc(principal.getId(), principal.getRole(), pageable)
				.map(this::mapToDto);
	}

	@Transactional(readOnly = true)
	public long getUnreadCount(UserPrincipal principal) {
		return notificationRepository.countByRecipientIdAndRecipientRoleAndReadFalse(
				principal.getId(), principal.getRole());
	}

	/** Marks one notification read. The update is scoped to the caller's own (id, role) identity. */
	public NotificationResponseDto markRead(Long id, UserPrincipal principal) {
		int updated = notificationRepository.markAsRead(id, principal.getId(), principal.getRole());
		if (updated == 0) {
			throw new ResourceNotFoundException("Notification not found");
		}
		return mapToDto(notificationRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Notification not found")));
	}

	/** Marks all of the caller's notifications read. Returns the number updated. */
	public int markAllRead(UserPrincipal principal) {
		return notificationRepository.markAllAsRead(principal.getId(), principal.getRole());
	}

	private NotificationResponseDto mapToDto(Notification notification) {
		return new NotificationResponseDto(notification.getId(), notification.getRecipientId(),
				notification.getRecipientRole(), notification.getTitle(), notification.getMessage(),
				notification.getComplaintPublicId(), notification.isRead(), notification.getCreatedAt());
	}

	// ---- Business-event notifications (all defensive, never rethrow) ----

	/**
	 * Complaint submitted: notify every DEPARTMENT_ADMIN of the complaint's department. If the
	 * department has no admin accounts, fall back to all SYSTEM_ADMINs.
	 */
	public void notifyDepartmentAdminsNewComplaint(Complaint complaint) {
		try {
			List<AdminUser> admins = adminUserRepository.findByDepartmentIdAndRole(
					complaint.getDepartment().getId(), UserRole.DEPARTMENT_ADMIN);
			String role = AccessRules.DEPARTMENT_ADMIN;
			if (admins.isEmpty()) {
				admins = adminUserRepository.findByRole(UserRole.SYSTEM_ADMIN);
				role = AccessRules.SYSTEM_ADMIN;
			}
			for (AdminUser admin : admins) {
				createNotification(admin.getId(), role, "New complaint submitted",
						"A new complaint \"" + complaint.getTitle() + "\" (" + complaint.getPublicId()
								+ ") was submitted to your department.",
						complaint.getPublicId());
			}
		} catch (Exception e) {
			log.warn("Failed to notify admins about new complaint {}: {}", complaint.getPublicId(), e.getMessage());
		}
	}

	/** Complaint submitted: confirm receipt to the citizen who filed it. */
	public void notifyCitizenComplaintSubmitted(Complaint complaint) {
		try {
			Citizen citizen = complaint.getCitizen();
			createNotification(citizen.getId(), AccessRules.CITIZEN, "Complaint submitted",
					"Your complaint \"" + complaint.getTitle() + "\" (" + complaint.getPublicId()
							+ ") was submitted and is awaiting department review.",
					complaint.getPublicId());
		} catch (Exception e) {
			log.warn("Failed to notify citizen about submitted complaint {}: {}", complaint.getPublicId(),
					e.getMessage());
		}
	}

	/**
	 * Citizen status update for lifecycle transitions. Approved is skipped because it is immediately
	 * followed by Resolved in the same review action.
	 */
	public void notifyCitizenStatusChanged(Complaint complaint, ComplaintStatus status) {
		String title;
		String message;
		switch (status) {
		case ASSIGNED -> {
			title = "Complaint assigned";
			message = "A field worker has been assigned to your complaint \"" + complaint.getTitle() + "\".";
		}
		case IN_PROGRESS -> {
			title = "Work started";
			message = "Work has started on your complaint \"" + complaint.getTitle() + "\".";
		}
		case PENDING_VERIFICATION -> {
			title = "Work submitted for review";
			message = "Work on your complaint \"" + complaint.getTitle()
					+ "\" is complete and awaiting department approval.";
		}
		case RESOLVED -> {
			title = "Complaint resolved";
			message = "Your complaint \"" + complaint.getTitle() + "\" has been resolved.";
		}
		case REJECTED -> {
			title = "Complaint rejected";
			message = "Your complaint \"" + complaint.getTitle() + "\" was rejected. Check the status history for details.";
		}
		default -> {
			return;
		}
		}
		createNotification(complaint.getCitizen().getId(), AccessRules.CITIZEN, title, message,
				complaint.getPublicId());
	}

	/** Complaint assigned to a worker: notify the worker. */
	public void notifyWorkerTaskAssigned(Complaint complaint, Worker worker) {
		try {
			createNotification(worker.getId(), AccessRules.WORKER, "New task assigned",
					"You have been assigned the complaint \"" + complaint.getTitle() + "\" ("
							+ complaint.getPublicId() + ").",
					complaint.getPublicId());
		} catch (Exception e) {
			log.warn("Failed to notify worker {} about assignment {}: {}", worker.getId(),
					complaint.getPublicId(), e.getMessage());
		}
	}

	/** Worker started work (IN_PROGRESS): notify the complaint's department admins. */
	public void notifyDepartmentAdminsWorkStarted(Complaint complaint) {
		notifyDepartmentAdmins(complaint, "Work started",
				"Work has started on complaint \"" + complaint.getTitle() + "\" (" + complaint.getPublicId() + ").");
	}

	/** Resolution submitted for review: notify the complaint's department admins. */
	public void notifyDepartmentAdminsResolutionSubmitted(Complaint complaint) {
		notifyDepartmentAdmins(complaint, "Resolution ready for review",
				"A resolution was submitted for complaint \"" + complaint.getTitle() + "\" ("
						+ complaint.getPublicId() + ").");
	}

	/** Resolution approved: notify the worker. The citizen is notified by the RESOLVED status change. */
	public void notifyResolutionApproved(Complaint complaint, Worker worker) {
		try {
			createNotification(worker.getId(), AccessRules.WORKER, "Resolution approved",
					"Your resolution for complaint \"" + complaint.getTitle() + "\" (" + complaint.getPublicId()
							+ ") was approved.",
					complaint.getPublicId());
		} catch (Exception e) {
			log.warn("Failed to notify about approved resolution {}: {}", complaint.getPublicId(), e.getMessage());
		}
	}

	/** Resolution rejected (rework): notify the worker. */
	public void notifyResolutionRejected(Complaint complaint, Worker worker, String reason) {
		try {
			createNotification(worker.getId(), AccessRules.WORKER, "Resolution rejected",
					"Your resolution for complaint \"" + complaint.getTitle() + "\" (" + complaint.getPublicId()
							+ ") was rejected. Reason: " + reason,
					complaint.getPublicId());
		} catch (Exception e) {
			log.warn("Failed to notify worker {} about rejected resolution {}: {}", worker.getId(),
					complaint.getPublicId(), e.getMessage());
		}
	}

	private void notifyDepartmentAdmins(Complaint complaint, String title, String message) {
		try {
			List<AdminUser> admins = adminUserRepository.findByDepartmentIdAndRole(
					complaint.getDepartment().getId(), UserRole.DEPARTMENT_ADMIN);
			for (AdminUser admin : admins) {
				createNotification(admin.getId(), AccessRules.DEPARTMENT_ADMIN, title, message,
						complaint.getPublicId());
			}
		} catch (Exception e) {
			log.warn("Failed to notify department admins for complaint {}: {}", complaint.getPublicId(), e.getMessage());
		}
	}
}
