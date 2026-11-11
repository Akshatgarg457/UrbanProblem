package com.urbanproblems.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.urbanproblems.domain.enums.ComplaintCategory;
import com.urbanproblems.security.AccessRules;
import com.urbanproblems.service.AnalyticsService;
import com.urbanproblems.service.AnalyticsService.AverageResolutionTime;
import com.urbanproblems.service.AnalyticsService.DepartmentMetrics;
import com.urbanproblems.service.AnalyticsService.DepartmentSummary;
import com.urbanproblems.service.AnalyticsService.SystemMetrics;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

	private final AnalyticsService analyticsService;

	public AnalyticsController(AnalyticsService analyticsService) {
		this.analyticsService = analyticsService;
	}

	@GetMapping("/system-metrics")
	@PreAuthorize("hasRole('SYSTEM_ADMIN')")
	public ResponseEntity<SystemMetrics> getSystemMetrics() {
		SystemMetrics metrics = analyticsService.getSystemMetrics();
		return ResponseEntity.ok(metrics);
	}

	@GetMapping("/complaints-by-category")
	@PreAuthorize("hasRole('SYSTEM_ADMIN')")
	public ResponseEntity<Map<ComplaintCategory, Long>> getComplaintsByCategory() {
		Map<ComplaintCategory, Long> stats = analyticsService.getComplaintsByCategory();
		return ResponseEntity.ok(stats);
	}

	@GetMapping("/complaints-by-department")
	@PreAuthorize("hasRole('SYSTEM_ADMIN')")
	public ResponseEntity<Map<String, Long>> getComplaintsByDepartment() {
		Map<String, Long> stats = analyticsService.getComplaintsByDepartment();
		return ResponseEntity.ok(stats);
	}

	@GetMapping("/complaints-by-status")
	@PreAuthorize("hasRole('SYSTEM_ADMIN')")
	public ResponseEntity<Map<String, Long>> getComplaintsByStatus() {
		Map<String, Long> stats = analyticsService.getComplaintsByStatus();
		return ResponseEntity.ok(stats);
	}

	@GetMapping("/average-resolution-time")
	@PreAuthorize("hasRole('SYSTEM_ADMIN')")
	public ResponseEntity<AverageResolutionTime> getAverageResolutionTime() {
		AverageResolutionTime avgTime = analyticsService.getAverageResolutionTime();
		return ResponseEntity.ok(avgTime);
	}

	@GetMapping("/department-metrics")
	@PreAuthorize("hasRole('SYSTEM_ADMIN')")
	public ResponseEntity<Map<Long, DepartmentMetrics>> getDepartmentMetrics() {
		Map<Long, DepartmentMetrics> metrics = analyticsService.getDepartmentMetrics();
		return ResponseEntity.ok(metrics);
	}

	@GetMapping("/department/{departmentId}/summary")
	@PreAuthorize("hasAnyRole('DEPARTMENT_ADMIN', 'SYSTEM_ADMIN')")
	public ResponseEntity<DepartmentSummary> getDepartmentSummary(@PathVariable Long departmentId) {
		return ResponseEntity.ok(
				analyticsService.getDepartmentSummary(departmentId, AccessRules.currentPrincipal()));
	}
}
