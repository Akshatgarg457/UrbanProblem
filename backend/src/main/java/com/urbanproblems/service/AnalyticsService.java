package com.urbanproblems.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.urbanproblems.domain.entity.Complaint;
import com.urbanproblems.domain.entity.Department;
import com.urbanproblems.domain.enums.ComplaintCategory;
import com.urbanproblems.domain.enums.ComplaintStatus;
import com.urbanproblems.repository.AssignmentRepository;
import com.urbanproblems.repository.ComplaintRepository;
import com.urbanproblems.repository.DepartmentRepository;
import com.urbanproblems.repository.ResolutionRepository;
import com.urbanproblems.repository.WorkerRepository;
import com.urbanproblems.security.AccessRules;
import com.urbanproblems.security.UserPrincipal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {

	private final ComplaintRepository complaintRepository;
	private final AssignmentRepository assignmentRepository;
	private final ResolutionRepository resolutionRepository;
	private final DepartmentRepository departmentRepository;
	private final WorkerRepository workerRepository;

	public AnalyticsService(ComplaintRepository complaintRepository,
			AssignmentRepository assignmentRepository,
			ResolutionRepository resolutionRepository,
			DepartmentRepository departmentRepository,
			WorkerRepository workerRepository) {
		this.complaintRepository = complaintRepository;
		this.assignmentRepository = assignmentRepository;
		this.resolutionRepository = resolutionRepository;
		this.departmentRepository = departmentRepository;
		this.workerRepository = workerRepository;
	}

	public SystemMetrics getSystemMetrics() {
		SystemMetrics metrics = new SystemMetrics();

		// Overall counts
		metrics.totalComplaints = complaintRepository.count();
		metrics.totalDepartments = departmentRepository.count();
		metrics.totalWorkers = workerRepository.count();

		// Status breakdown
		metrics.submittedComplaints = complaintRepository.countByStatus(ComplaintStatus.SUBMITTED);
		metrics.assignedComplaints = complaintRepository.countByStatus(ComplaintStatus.ASSIGNED);
		metrics.inProgressComplaints = complaintRepository.countByStatus(ComplaintStatus.IN_PROGRESS);
		metrics.pendingVerificationComplaints = complaintRepository
				.countByStatus(ComplaintStatus.PENDING_VERIFICATION);
		metrics.resolvedComplaints = complaintRepository.countByStatus(ComplaintStatus.RESOLVED);
		metrics.rejectedComplaints = complaintRepository.countByStatus(ComplaintStatus.REJECTED);

		// Pending reviews
		metrics.pendingResolutions = resolutionRepository
				.countByReviewStatus(com.urbanproblems.domain.entity.Resolution.ReviewStatus.PENDING);

		// Active assignments
		metrics.totalAssignments = assignmentRepository.count();

		// Resolution rate
		if (metrics.totalComplaints > 0) {
			metrics.resolutionRate = (double) metrics.resolvedComplaints / metrics.totalComplaints * 100;
		}

		return metrics;
	}

	public Map<ComplaintCategory, Long> getComplaintsByCategory() {
		Map<ComplaintCategory, Long> categoryStats = new HashMap<>();
		for (ComplaintCategory category : ComplaintCategory.values()) {
			long count = complaintRepository.countByCategory(category);
			categoryStats.put(category, count);
		}
		return categoryStats;
	}

	public Map<String, Long> getComplaintsByDepartment() {
		Map<String, Long> departmentStats = new HashMap<>();
		departmentRepository.findAll().forEach(dept ->
				departmentStats.put(dept.getName(), complaintRepository.countByDepartmentId(dept.getId())));
		return departmentStats;
	}

	public Map<String, Long> getComplaintsByStatus() {
		Map<String, Long> statusStats = new HashMap<>();
		statusStats.put("SUBMITTED", complaintRepository.countByStatus(ComplaintStatus.SUBMITTED));
		statusStats.put("ASSIGNED", complaintRepository.countByStatus(ComplaintStatus.ASSIGNED));
		statusStats.put("IN_PROGRESS", complaintRepository.countByStatus(ComplaintStatus.IN_PROGRESS));
		statusStats.put("PENDING_VERIFICATION",
				complaintRepository.countByStatus(ComplaintStatus.PENDING_VERIFICATION));
		statusStats.put("RESOLVED", complaintRepository.countByStatus(ComplaintStatus.RESOLVED));
		statusStats.put("REJECTED", complaintRepository.countByStatus(ComplaintStatus.REJECTED));
		return statusStats;
	}

	public AverageResolutionTime getAverageResolutionTime() {
		List<Complaint> resolvedComplaints = complaintRepository.findCreatedComplaintsCreatedSince(
				Instant.now().minus(90, ChronoUnit.DAYS),
				ComplaintStatus.RESOLVED);

		if (resolvedComplaints.isEmpty()) {
			return new AverageResolutionTime(0, 0);
		}

		double totalDays = resolvedComplaints.stream()
				.mapToLong(complaint -> ChronoUnit.DAYS.between(complaint.getCreatedAt(), complaint.getUpdatedAt()))
				.average()
				.orElse(0);

		return new AverageResolutionTime(
				(long) totalDays,
				resolvedComplaints.size());
	}

	public Map<Long, DepartmentMetrics> getDepartmentMetrics() {
		Map<Long, DepartmentMetrics> departmentMetrics = new HashMap<>();

		departmentRepository.findAll().forEach(dept -> {
			DepartmentMetrics metrics = new DepartmentMetrics();
			metrics.departmentId = dept.getId();
			metrics.departmentName = dept.getName();

			long totalComplaints = complaintRepository.countByDepartmentId(dept.getId());
			long resolvedComplaints = complaintRepository.countByDepartmentIdAndStatus(
					dept.getId(), ComplaintStatus.RESOLVED);
			long totalWorkers = workerRepository.countByDepartmentId(dept.getId());

			metrics.totalComplaints = totalComplaints;
			metrics.resolvedComplaints = resolvedComplaints;
			metrics.workerCount = totalWorkers;

			if (totalComplaints > 0) {
				metrics.resolutionRate = (double) resolvedComplaints / totalComplaints * 100;
			}

			departmentMetrics.put(dept.getId(), metrics);
		});

		return departmentMetrics;
	}

	/** Department dashboard summary. Department admin of that department, or a system admin. */
	public DepartmentSummary getDepartmentSummary(Long departmentId, UserPrincipal user) {
		Department department = departmentRepository.findById(departmentId)
				.orElseThrow(() -> new com.urbanproblems.exception.ResourceNotFoundException("Department not found"));
		AccessRules.requireDepartmentScope(user, department);

		DepartmentSummary summary = new DepartmentSummary();
		summary.departmentId = department.getId();
		summary.departmentName = department.getName();
		summary.totalComplaints = complaintRepository.countByDepartmentId(departmentId);
		summary.submittedComplaints = complaintRepository.countByDepartmentIdAndStatus(
				departmentId, ComplaintStatus.SUBMITTED);
		summary.assignedComplaints = complaintRepository.countByDepartmentIdAndStatus(
				departmentId, ComplaintStatus.ASSIGNED);
		summary.inProgressComplaints = complaintRepository.countByDepartmentIdAndStatus(
				departmentId, ComplaintStatus.IN_PROGRESS);
		summary.pendingVerificationComplaints = complaintRepository.countByDepartmentIdAndStatus(
				departmentId, ComplaintStatus.PENDING_VERIFICATION);
		summary.resolvedComplaints = complaintRepository.countByDepartmentIdAndStatus(
				departmentId, ComplaintStatus.RESOLVED);
		summary.rejectedComplaints = complaintRepository.countByDepartmentIdAndStatus(
				departmentId, ComplaintStatus.REJECTED);

		summary.recentComplaints = complaintRepository.findByDepartmentId(departmentId,
						PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt")))
				.map(c -> new RecentComplaint(c.getPublicId(), c.getTitle(), c.getStatus(), c.getCreatedAt()))
				.toList();

		return summary;
	}

	@Getter
	@Setter
	public static class SystemMetrics {
		private long totalComplaints;
		private long totalDepartments;
		private long totalWorkers;
		private long submittedComplaints;
		private long assignedComplaints;
		private long inProgressComplaints;
		private long pendingVerificationComplaints;
		private long resolvedComplaints;
		private long rejectedComplaints;
		private long pendingResolutions;
		private long totalAssignments;
		private double resolutionRate;
	}

	@Getter
	@Setter
	public static class AverageResolutionTime {
		private long averageDays;
		private int sampleSize;

		public AverageResolutionTime(long averageDays, int sampleSize) {
			this.averageDays = averageDays;
			this.sampleSize = sampleSize;
		}
	}

	@Getter
	@Setter
	public static class DepartmentMetrics {
		private Long departmentId;
		private String departmentName;
		private long totalComplaints;
		private long resolvedComplaints;
		private long workerCount;
		private double resolutionRate;
	}

	@Getter
	@Setter
	public static class DepartmentSummary {
		private Long departmentId;
		private String departmentName;
		private long totalComplaints;
		private long submittedComplaints;
		private long assignedComplaints;
		private long inProgressComplaints;
		private long pendingVerificationComplaints;
		private long resolvedComplaints;
		private long rejectedComplaints;
		private List<RecentComplaint> recentComplaints;
	}

	@Getter
	@Setter
	@AllArgsConstructor
	public static class RecentComplaint {
		private String publicId;
		private String title;
		private ComplaintStatus status;
		private Instant createdAt;
	}
}
