package com.urbanproblems.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.urbanproblems.domain.entity.Complaint;
import com.urbanproblems.domain.entity.RecurrenceReport;
import com.urbanproblems.domain.enums.ComplaintCategory;
import com.urbanproblems.domain.enums.ComplaintStatus;
import com.urbanproblems.dto.RecurrenceReportResponseDto;
import com.urbanproblems.exception.ResourceNotFoundException;
import com.urbanproblems.repository.ComplaintRepository;
import com.urbanproblems.repository.RecurrenceReportRepository;
import com.urbanproblems.security.AccessRules;
import com.urbanproblems.security.UserPrincipal;

/**
 * Deterministic, explainable recurrence detection. It is NOT a prediction.
 *
 * Rule: a complaint is recurring with respect to another if both are in the same category, the other
 * is RESOLVED, it was created within the lookback window, and the two are within
 * {@value #PROXIMITY_RADIUS_KM} km (Haversine). If either complaint lacks coordinates, a normalized
 * location text match is used instead.
 */
@Service
@Transactional
public class RecurrenceService {

	static final double PROXIMITY_RADIUS_KM = 0.5;
	static final int MIN_HISTORICAL_THRESHOLD = 2;
	static final long LOOKBACK_DAYS = 365;

	private final RecurrenceReportRepository recurrenceReportRepository;
	private final ComplaintRepository complaintRepository;
	private final ComplaintService complaintService;

	public RecurrenceService(RecurrenceReportRepository recurrenceReportRepository,
			ComplaintRepository complaintRepository,
			ComplaintService complaintService) {
		this.recurrenceReportRepository = recurrenceReportRepository;
		this.complaintRepository = complaintRepository;
		this.complaintService = complaintService;
	}

	/** Returns the existing report if one was already computed, otherwise computes and stores it. */
	public RecurrenceReportResponseDto analyzeRecurrence(Long complaintId, UserPrincipal user) {
		Complaint complaint = complaintService.findComplaint(complaintId);
		AccessRules.requireDepartmentScope(user, complaint.getDepartment());

		return recurrenceReportRepository.findByCurrentComplaintId(complaintId)
				.map(RecurrenceReportResponseDto::from)
				.orElseGet(() -> {
					List<Complaint> historical = findHistoricalComplaints(complaint);
					RecurrenceReport report = RecurrenceReport.builder()
							.currentComplaint(complaint)
							.category(complaint.getCategory())
							.location(complaint.getLocation())
							.latitude(complaint.getLatitude())
							.longitude(complaint.getLongitude())
							.proximityRadiusKm(PROXIMITY_RADIUS_KM)
							.historicalIssueCount(historical.size())
							.relatedComplaintIds(historical.stream().map(Complaint::getId).toList())
							.analysisNotes(buildNotes(complaint, historical))
							.build();
					return RecurrenceReportResponseDto.from(recurrenceReportRepository.save(report));
				});
	}

	/** System admins see every report. Department admins see only their department's reports. */
	@Transactional(readOnly = true)
	public Page<RecurrenceReportResponseDto> getReports(UserPrincipal user, Pageable pageable) {
		if (AccessRules.SYSTEM_ADMIN.equals(user.getRole())) {
			return recurrenceReportRepository.findAll(pageable).map(RecurrenceReportResponseDto::from);
		}
		if (user.getDepartmentId() == null) {
			throw new AccessDeniedException("Access denied");
		}
		return recurrenceReportRepository.findByCurrentComplaintDepartmentId(user.getDepartmentId(), pageable)
				.map(RecurrenceReportResponseDto::from);
	}

	@Transactional(readOnly = true)
	public RecurrenceReportResponseDto getReport(Long reportId, UserPrincipal user) {
		RecurrenceReport report = recurrenceReportRepository.findById(reportId)
				.orElseThrow(() -> new ResourceNotFoundException("Recurrence report not found"));
		AccessRules.requireDepartmentScope(user, report.getCurrentComplaint().getDepartment());
		return RecurrenceReportResponseDto.from(report);
	}

	@Transactional(readOnly = true)
	public RecurrenceReportResponseDto getReportForComplaint(Long complaintId, UserPrincipal user) {
		Complaint complaint = complaintService.findComplaint(complaintId);
		AccessRules.requireDepartmentScope(user, complaint.getDepartment());
		return recurrenceReportRepository.findByCurrentComplaintId(complaintId)
				.map(RecurrenceReportResponseDto::from)
				.orElseThrow(() -> new ResourceNotFoundException("No recurrence report for complaint"));
	}

	/** System-wide view only. */
	@Transactional(readOnly = true)
	public List<RecurrenceReportResponseDto> getHighRiskAreas(int minHistoricalCount, UserPrincipal user) {
		if (!AccessRules.SYSTEM_ADMIN.equals(user.getRole())) {
			throw new AccessDeniedException("Access denied");
		}
		return recurrenceReportRepository.findAll().stream()
				.filter(r -> r.getHistoricalIssueCount() != null && r.getHistoricalIssueCount() >= minHistoricalCount)
				.sorted((a, b) -> Integer.compare(b.getHistoricalIssueCount(), a.getHistoricalIssueCount()))
				.map(RecurrenceReportResponseDto::from)
				.toList();
	}

	/** Count of complaints in a category, scoped to the caller's department unless they are a system admin. */
	@Transactional(readOnly = true)
	public long getCategoryCount(ComplaintCategory category, UserPrincipal user) {
		if (AccessRules.SYSTEM_ADMIN.equals(user.getRole())) {
			return complaintRepository.countByCategory(category);
		}
		if (user.getDepartmentId() == null) {
			throw new AccessDeniedException("Access denied");
		}
		return complaintRepository.countByCategoryAndDepartmentId(category, user.getDepartmentId());
	}

	private List<Complaint> findHistoricalComplaints(Complaint current) {
		Instant cutoff = Instant.now().minus(LOOKBACK_DAYS, ChronoUnit.DAYS);
		return complaintRepository
				.findByCategoryAndStatusAndCreatedAtGreaterThanEqual(current.getCategory(), ComplaintStatus.RESOLVED, cutoff)
				.stream()
				.filter(c -> !c.getId().equals(current.getId()))
				.filter(c -> isNearby(current, c))
				.toList();
	}

	static boolean isNearby(Complaint a, Complaint b) {
		if (hasCoordinates(a) && hasCoordinates(b)) {
			return haversineKm(a.getLatitude(), a.getLongitude(), b.getLatitude(), b.getLongitude())
					<= PROXIMITY_RADIUS_KM;
		}
		return normalize(a.getLocation()).equals(normalize(b.getLocation()));
	}

	static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
		final double earthRadiusKm = 6371.0;
		double dLat = Math.toRadians(lat2 - lat1);
		double dLon = Math.toRadians(lon2 - lon1);
		double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
				+ Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
				* Math.sin(dLon / 2) * Math.sin(dLon / 2);
		return 2 * earthRadiusKm * Math.asin(Math.min(1.0, Math.sqrt(h)));
	}

	private static boolean hasCoordinates(Complaint c) {
		return c.getLatitude() != null && c.getLongitude() != null;
	}

	private static String normalize(String location) {
		return location == null ? "" : location.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
	}

	private static String buildNotes(Complaint current, List<Complaint> historical) {
		StringBuilder notes = new StringBuilder("Rule: same category, RESOLVED, within ")
				.append(LOOKBACK_DAYS).append(" days, within ").append(PROXIMITY_RADIUS_KM)
				.append(" km (or same normalized location when coordinates are missing). ");
		if (historical.isEmpty()) {
			notes.append("No similar resolved complaints found; this appears isolated.");
		} else if (historical.size() < MIN_HISTORICAL_THRESHOLD) {
			notes.append(historical.size()).append(" similar resolved complaint(s) found; history is limited.");
		} else {
			notes.append("RECURRENCE DETECTED: ").append(historical.size())
					.append(" similar resolved complaints found. Consider a preventive fix.");
		}
		return notes.append(" Category: ").append(current.getCategory().getCode())
				.append(", Location: ").append(current.getLocation()).toString();
	}
}
