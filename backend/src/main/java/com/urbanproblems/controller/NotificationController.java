package com.urbanproblems.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.urbanproblems.dto.NotificationResponseDto;
import com.urbanproblems.dto.NotificationUnreadCountDto;
import com.urbanproblems.security.AccessRules;
import com.urbanproblems.service.NotificationService;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

	private final NotificationService notificationService;

	public NotificationController(NotificationService notificationService) {
		this.notificationService = notificationService;
	}

	/** Any authenticated role; a user only ever sees notifications addressed to their own (id, role). */
	@GetMapping("/my")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<Page<NotificationResponseDto>> getMyNotifications(Pageable pageable) {
		return ResponseEntity.ok(notificationService.getMyNotifications(AccessRules.currentPrincipal(), pageable));
	}

	@GetMapping("/my/unread-count")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<NotificationUnreadCountDto> getUnreadCount() {
		return ResponseEntity.ok(
				new NotificationUnreadCountDto(notificationService.getUnreadCount(AccessRules.currentPrincipal())));
	}

	@PutMapping("/{id}/read")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<NotificationResponseDto> markRead(@PathVariable Long id) {
		return ResponseEntity.ok(notificationService.markRead(id, AccessRules.currentPrincipal()));
	}

	@PutMapping("/read-all")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<NotificationUnreadCountDto> markAllRead() {
		notificationService.markAllRead(AccessRules.currentPrincipal());
		return ResponseEntity.ok(
				new NotificationUnreadCountDto(notificationService.getUnreadCount(AccessRules.currentPrincipal())));
	}
}
