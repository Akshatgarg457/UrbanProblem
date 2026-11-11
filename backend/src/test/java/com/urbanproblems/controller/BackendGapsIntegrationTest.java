package com.urbanproblems.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import com.urbanproblems.IntegrationTestBase;
import com.urbanproblems.domain.entity.AdminUser;
import com.urbanproblems.domain.entity.Citizen;
import com.urbanproblems.domain.entity.Department;
import com.urbanproblems.domain.entity.Worker;
import com.urbanproblems.domain.enums.ComplaintCategory;
import com.urbanproblems.domain.enums.ComplaintStatus;
import com.urbanproblems.domain.enums.UserRole;
import com.urbanproblems.domain.enums.UserStatus;
import com.urbanproblems.dto.AssignmentRequestDto;
import com.urbanproblems.dto.AssignmentResponseDto;
import com.urbanproblems.dto.ComplaintRequestDto;
import com.urbanproblems.dto.ComplaintResponseDto;
import com.urbanproblems.dto.ResolutionResponseDto;
import com.urbanproblems.dto.ResolutionReviewDto;
import com.urbanproblems.dto.ResolutionSubmitDto;
import com.urbanproblems.repository.ComplaintRepository;
import com.urbanproblems.repository.NotificationRepository;
import com.urbanproblems.security.UserPrincipal;
import com.urbanproblems.service.AssignmentService;
import com.urbanproblems.service.ComplaintService;
import com.urbanproblems.service.ResolutionService;

/**
 * Integration tests for the backend gaps: worker/admin self-service profiles, complaint status
 * history, department summary analytics, and the in-app notification system.
 */
class BackendGapsIntegrationTest extends IntegrationTestBase {

	@Autowired
	private ComplaintRepository complaintRepository;

	@Autowired
	private NotificationRepository notificationRepository;

	@Autowired
	private ComplaintService complaintService;

	@Autowired
	private AssignmentService assignmentService;

	@Autowired
	private ResolutionService resolutionService;

	private Department water;
	private Department electricity;
	private Citizen citizenA;
	private Citizen citizenB;
	private Worker workerA;
	private AdminUser waterAdmin;
	private AdminUser electricityAdmin;
	private AdminUser systemAdmin;

	private String citizenAToken;
	private String citizenBToken;
	private String workerAToken;
	private String waterAdminToken;
	private String electricityAdminToken;
	private String systemAdminToken;

	@BeforeEach
	void setUp() {
		cleanupAll();

		java.util.Map<String, Department> departments = seedDepartments();
		water = departments.get("Water and Sanitation");
		electricity = departments.get("Electricity");

		citizenA = citizenRepository.save(Citizen.builder()
				.email("gap-citizen-a@test.org").firstName("A").lastName("One").phoneNumber("5550000001")
				.passwordHash(passwordEncoder.encode("Password123!")).status(UserStatus.ACTIVE).build());
		citizenB = citizenRepository.save(Citizen.builder()
				.email("gap-citizen-b@test.org").firstName("B").lastName("Two").phoneNumber("5550000002")
				.passwordHash(passwordEncoder.encode("Password123!")).status(UserStatus.ACTIVE).build());

		workerA = workerRepository.save(Worker.builder()
				.email("gap-worker-a@test.org").firstName("W").lastName("Worker").phoneNumber("5550000100")
				.passwordHash(passwordEncoder.encode("Password123!")).department(water)
				.status(UserStatus.ACTIVE).build());

		waterAdmin = adminUserRepository.save(admin("gap-water-admin@test.org", UserRole.DEPARTMENT_ADMIN, water));
		electricityAdmin = adminUserRepository.save(
				admin("gap-elec-admin@test.org", UserRole.DEPARTMENT_ADMIN, electricity));
		systemAdmin = adminUserRepository.save(admin("gap-sys-admin@test.org", UserRole.SYSTEM_ADMIN, null));

		citizenAToken = token(citizenA.getId(), citizenA.getEmail(), "CITIZEN", null);
		citizenBToken = token(citizenB.getId(), citizenB.getEmail(), "CITIZEN", null);
		workerAToken = token(workerA.getId(), workerA.getEmail(), "WORKER", water.getId());
		waterAdminToken = token(waterAdmin.getId(), waterAdmin.getEmail(), "DEPARTMENT_ADMIN", water.getId());
		electricityAdminToken = token(electricityAdmin.getId(), electricityAdmin.getEmail(),
				"DEPARTMENT_ADMIN", electricity.getId());
		systemAdminToken = token(systemAdmin.getId(), systemAdmin.getEmail(), "SYSTEM_ADMIN", null);
	}

	// ---- A. Worker self-service profile ----

	@Test
	void workerCanGetOwnProfile() throws Exception {
		mockMvc.perform(get("/api/workers/me").header("Authorization", "Bearer " + workerAToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(workerA.getId()))
				.andExpect(jsonPath("$.email").value("gap-worker-a@test.org"))
				.andExpect(jsonPath("$.departmentId").value(water.getId()))
				.andExpect(jsonPath("$.departmentName").value("Water and Sanitation"))
				.andExpect(jsonPath("$.createdAt").isString());
	}

	@Test
	void workerCanUpdateOwnProfileFields() throws Exception {
		String updateJson = """
				{"firstName":"Renamed","lastName":"Worker","phoneNumber":"5559990000"}
				""";
		mockMvc.perform(put("/api/workers/me")
				.header("Authorization", "Bearer " + workerAToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(updateJson))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.firstName").value("Renamed"))
				.andExpect(jsonPath("$.phoneNumber").value("5559990000"));

		Worker reloaded = workerRepository.findById(workerA.getId()).orElseThrow();
		assertEquals("Renamed", reloaded.getFirstName());
		assertEquals("5559990000", reloaded.getPhoneNumber());
	}

	@Test
	void workerCanSaveProfileLocation() throws Exception {
		String updateJson = """
				{
					"firstName":"Renamed","lastName":"Worker","phoneNumber":"5559990000",
					"address":"22 Canal Street","city":"Bhopal","state":"Madhya Pradesh",
					"postalCode":"462016","latitude":23.2334,"longitude":77.4298
				}
				""";
		mockMvc.perform(put("/api/workers/me")
				.header("Authorization", "Bearer " + workerAToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(updateJson))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.address").value("22 Canal Street"))
				.andExpect(jsonPath("$.latitude").value(23.2334))
				.andExpect(jsonPath("$.longitude").value(77.4298));

		mockMvc.perform(get("/api/workers/me").header("Authorization", "Bearer " + workerAToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.city").value("Bhopal"))
				.andExpect(jsonPath("$.postalCode").value("462016"));

		Worker reloaded = workerRepository.findById(workerA.getId()).orElseThrow();
		assertEquals("22 Canal Street", reloaded.getAddress());
		assertEquals(23.2334, reloaded.getLatitude());
	}

	@Test
	void citizenGets403OnWorkerSelfServiceProfile() throws Exception {
		mockMvc.perform(get("/api/workers/me").header("Authorization", "Bearer " + citizenAToken))
				.andExpect(status().isForbidden());

		mockMvc.perform(put("/api/workers/me")
				.header("Authorization", "Bearer " + citizenAToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"firstName\":\"X\",\"lastName\":\"Y\",\"phoneNumber\":\"5559990000\"}"))
				.andExpect(status().isForbidden());
	}

	// ---- B. Admin self-service profile ----

	@Test
	void departmentAdminCanGetOwnProfile() throws Exception {
		mockMvc.perform(get("/api/admin/me").header("Authorization", "Bearer " + waterAdminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(waterAdmin.getId()))
				.andExpect(jsonPath("$.role").value("DEPARTMENT_ADMIN"))
				.andExpect(jsonPath("$.departmentId").value(water.getId()))
				.andExpect(jsonPath("$.departmentName").value("Water and Sanitation"));
	}

	@Test
	void systemAdminCanGetOwnProfile() throws Exception {
		mockMvc.perform(get("/api/admin/me").header("Authorization", "Bearer " + systemAdminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.role").value("SYSTEM_ADMIN"))
				.andExpect(jsonPath("$.departmentId").doesNotExist());
	}

	@Test
	void adminCanUpdateOwnNameButNotRoleOrDepartment() throws Exception {
		mockMvc.perform(put("/api/admin/me")
				.header("Authorization", "Bearer " + waterAdminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"firstName\":\"NewName\",\"lastName\":\"Admin\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.firstName").value("NewName"));

		AdminUser reloaded = adminUserRepository.findById(waterAdmin.getId()).orElseThrow();
		assertEquals("NewName", reloaded.getFirstName());
		assertEquals(UserRole.DEPARTMENT_ADMIN, reloaded.getRole());
		assertEquals(water.getId(), reloaded.getDepartment().getId());
	}

	@Test
	void nonAdminGets403OnAdminSelfServiceProfile() throws Exception {
		mockMvc.perform(get("/api/admin/me").header("Authorization", "Bearer " + workerAToken))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/admin/me").header("Authorization", "Bearer " + citizenAToken))
				.andExpect(status().isForbidden());
	}

	// ---- C. Complaint status history ----

	@Test
	void ownerSeesHistoryWithInitialSubmissionEntry() throws Exception {
		ComplaintResponseDto complaint = createWaterComplaint();

		mockMvc.perform(get("/api/complaints/" + complaint.getPublicId() + "/history")
				.header("Authorization", "Bearer " + citizenAToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].toStatus").value("SUBMITTED"))
				.andExpect(jsonPath("$[0].fromStatus").doesNotExist())
				.andExpect(jsonPath("$[0].actorRole").value("CITIZEN"))
				.andExpect(jsonPath("$[0].changedAt").isString());
	}

	@Test
	void historyRecordsAssignmentAndDeniesUnrelatedCitizen() throws Exception {
		ComplaintResponseDto complaint = createWaterComplaint();
		assignWorker(complaint.getId());

		mockMvc.perform(get("/api/complaints/" + complaint.getPublicId() + "/history")
				.header("Authorization", "Bearer " + citizenAToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].toStatus").value("ASSIGNED"))
				.andExpect(jsonPath("$[0].fromStatus").value("SUBMITTED"))
				.andExpect(jsonPath("$[0].actorRole").value("DEPARTMENT_ADMIN"));

		mockMvc.perform(get("/api/complaints/" + complaint.getPublicId() + "/history")
				.header("Authorization", "Bearer " + citizenBToken))
				.andExpect(status().isForbidden());
	}

	@Test
	void systemAdminCanAssignWorkersAcrossDepartments() throws Exception {
		ComplaintResponseDto complaint = createWaterComplaint();
		mockMvc.perform(post("/api/assignments/complaints/" + complaint.getId() + "/assign")
				.header("Authorization", "Bearer " + systemAdminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"workerId\":" + workerA.getId() + "}"))
				.andExpect(status().isCreated());

		assertEquals(ComplaintStatus.ASSIGNED,
				complaintRepository.findById(complaint.getId()).orElseThrow().getStatus());
		mockMvc.perform(get("/api/complaints/" + complaint.getPublicId() + "/history")
				.header("Authorization", "Bearer " + citizenAToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].actorRole").value("SYSTEM_ADMIN"));
	}

	// ---- D. Department summary analytics ----

	@Test
	void departmentAdminCanGetOwnDepartmentSummary() throws Exception {
		createWaterComplaint();
		createWaterComplaint();

		mockMvc.perform(get("/api/analytics/department/" + water.getId() + "/summary")
				.header("Authorization", "Bearer " + waterAdminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.departmentId").value(water.getId()))
				.andExpect(jsonPath("$.totalComplaints").value(2))
				.andExpect(jsonPath("$.submittedComplaints").value(2))
				.andExpect(jsonPath("$.recentComplaints.length()").value(2));
	}

	@Test
	void otherDepartmentAdminGets403OnSummary() throws Exception {
		createWaterComplaint();

		mockMvc.perform(get("/api/analytics/department/" + water.getId() + "/summary")
				.header("Authorization", "Bearer " + electricityAdminToken))
				.andExpect(status().isForbidden());
	}

	@Test
	void systemAdminCanGetAnyDepartmentSummary() throws Exception {
		mockMvc.perform(get("/api/analytics/department/" + water.getId() + "/summary")
				.header("Authorization", "Bearer " + systemAdminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.departmentId").value(water.getId()));
	}

	// ---- E. Notifications ----

	@Test
	void complaintSubmissionNotifiesDepartmentAdmins() throws Exception {
		ComplaintResponseDto complaint = createWaterComplaint();

		mockMvc.perform(get("/api/notifications/my/unread-count")
				.header("Authorization", "Bearer " + waterAdminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.count").value(1));

		mockMvc.perform(get("/api/notifications/my")
				.header("Authorization", "Bearer " + waterAdminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[0].complaintPublicId").value(complaint.getPublicId()))
				.andExpect(jsonPath("$.content[0].read").value(false));

		// The filing citizen gets a submission receipt; an unrelated citizen gets nothing.
		assertEquals(1, unreadCount(citizenAToken));
		assertEquals(0, unreadCount(citizenBToken));
	}

	@Test
	void complaintSubmissionFallsBackToSystemAdminsWhenDepartmentHasNoAdmin() throws Exception {
		adminUserRepository.delete(electricityAdmin);

		postComplaint(citizenAToken, ComplaintCategory.ELECTRICITY_AND_STREETLIGHTS)
				.andExpect(status().isCreated());

		assertEquals(1, unreadCount(systemAdminToken));
		assertEquals(0, unreadCount(waterAdminToken));
	}

	@Test
	void assignmentAndWorkStartNotifyWorkerAndAdmins() throws Exception {
		ComplaintResponseDto complaint = createWaterComplaint();
		assertEquals(1, unreadCount(waterAdminToken));

		AssignmentResponseDto assignment = assignWorker(complaint.getId());
		assertEquals(1, unreadCount(workerAToken));
		assertEquals(1, unreadCount(waterAdminToken));

		assignmentService.startWork(assignment.getId(), workerPrincipal());
		assertEquals(2, unreadCount(waterAdminToken));
	}

	@Test
	void resolutionLifecycleNotifiesAdminsWorkerAndCitizen() throws Exception {
		ComplaintResponseDto complaint = createWaterComplaint();
		AssignmentResponseDto assignment = assignWorker(complaint.getId());
		assignmentService.startWork(assignment.getId(), workerPrincipal());
		uploadWorkerEvidence(complaint.getId());

		ResolutionResponseDto resolution = resolutionService.submitResolution(assignment.getId(),
				ResolutionSubmitDto.builder().completionNotes("Fixed the leaking pipe completely.").build(),
				workerPrincipal());
		assertEquals(3, unreadCount(waterAdminToken)); // submitted, started, resolution ready

		mockMvc.perform(post("/api/resolutions/" + resolution.getId() + "/approve")
				.header("Authorization", "Bearer " + systemAdminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"reviewStatus":"APPROVED","adminNotes":"Looks good."}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.reviewStatus").value("APPROVED"));
		assertEquals(2, unreadCount(workerAToken)); // assigned + approved
		// submitted, assigned, work started, submitted for review, resolved (exactly once).
		assertEquals(5, unreadCount(citizenAToken));
		long resolvedNotifications = notificationRepository
				.findByRecipientIdAndRecipientRoleOrderByCreatedAtDesc(citizenA.getId(), "CITIZEN",
						org.springframework.data.domain.PageRequest.of(0, 20))
				.getContent().stream()
				.filter(n -> "Complaint resolved".equals(n.getTitle()))
				.count();
		assertEquals(1, resolvedNotifications);
	}

	@Test
	void resolutionRejectionNotifiesWorker() throws Exception {
		ComplaintResponseDto complaint = createWaterComplaint();
		AssignmentResponseDto assignment = assignWorker(complaint.getId());
		assignmentService.startWork(assignment.getId(), workerPrincipal());
		uploadWorkerEvidence(complaint.getId());

		ResolutionResponseDto resolution = resolutionService.submitResolution(assignment.getId(),
				ResolutionSubmitDto.builder().completionNotes("Attempted fix, needs verification.").build(),
				workerPrincipal());
		resolutionService.rejectResolution(resolution.getId(),
				ResolutionReviewDto.builder()
						.reviewStatus(com.urbanproblems.domain.entity.Resolution.ReviewStatus.REJECTED)
						.rejectionReason("Evidence is insufficient.")
						.build(),
				waterAdminPrincipal());

		assertEquals(2, unreadCount(workerAToken)); // assigned + rejected
		assertTrue(notificationRepository
				.findByRecipientIdAndRecipientRoleOrderByCreatedAtDesc(workerA.getId(), "WORKER",
						org.springframework.data.domain.PageRequest.of(0, 1))
				.getContent().get(0).getMessage().contains("insufficient"));
	}

	@Test
	void userCanOnlyReadAndMarkOwnNotifications() throws Exception {
		createWaterComplaint();
		Long adminNotificationId = notificationRepository
				.findByRecipientIdAndRecipientRoleOrderByCreatedAtDesc(waterAdmin.getId(), "DEPARTMENT_ADMIN",
						org.springframework.data.domain.PageRequest.of(0, 1))
				.getContent().get(0).getId();

		// Another role cannot mark someone else's notification read (identity is id + role).
		mockMvc.perform(put("/api/notifications/" + adminNotificationId + "/read")
				.header("Authorization", "Bearer " + workerAToken))
				.andExpect(status().isNotFound());

		// Owner marks it read; unread count drops.
		mockMvc.perform(put("/api/notifications/" + adminNotificationId + "/read")
				.header("Authorization", "Bearer " + waterAdminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.read").value(true));
		assertEquals(0, unreadCount(waterAdminToken));

		// Read-all only affects the caller's own notifications.
		mockMvc.perform(put("/api/notifications/read-all")
				.header("Authorization", "Bearer " + citizenAToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.count").value(0));
		assertEquals(0, unreadCount(workerAToken));
	}

	@Test
	void notificationsRequireAuthentication() throws Exception {
		mockMvc.perform(get("/api/notifications/my"))
				.andExpect(status().isUnauthorized());
	}

	// ---- helpers ----

	private void cleanupAll() {
		notificationRepository.deleteAll();
		complaintEvidenceRepository.deleteAll();
		recurrenceReportRepository.deleteAll();
		complaintRepository.deleteAll();
		citizenRepository.deleteAll();
		workerRepository.deleteAll();
		adminUserRepository.deleteAll();
		departmentRepository.deleteAll();
	}

	private int complaintCounter = 0;

	private ComplaintResponseDto createWaterComplaint() {
		complaintCounter++;
		return complaintService.createComplaint(citizenA.getId(), ComplaintRequestDto.builder()
				.title("Water leak on main road " + complaintCounter)
				.description("Large water leak near the bus stop")
				.category(ComplaintCategory.WATER_SUPPLY)
				.location("Main Road, Sector 4 #" + complaintCounter)
				.latitude(22.7196)
				.longitude(75.8577)
				.build());
	}

	private org.springframework.test.web.servlet.ResultActions postComplaint(String token,
			ComplaintCategory category) throws Exception {
		return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
				.post("/api/complaints")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"title":"Gap test complaint","description":"Description of the gap test complaint",
						 "category":"%s","location":"Test Location","latitude":22.7,"longitude":75.8}
						""".formatted(category.name())));
	}

	private AssignmentResponseDto assignWorker(Long complaintId) {
		return assignmentService.assignComplaintToWorker(complaintId,
				AssignmentRequestDto.builder().workerId(workerA.getId()).build(),
				waterAdminPrincipal());
	}

	private void uploadWorkerEvidence(Long complaintId) throws Exception {
		byte[] pdf = "%PDF-1.4 test document body".getBytes(StandardCharsets.US_ASCII);
		mockMvc.perform(multipart("/api/complaints/" + complaintId + "/evidence/upload")
				.file(new MockMultipartFile("file", "photo.pdf", "application/pdf", pdf))
				.header("Authorization", "Bearer " + workerAToken))
				.andExpect(status().isCreated());
	}

	private long unreadCount(String token) throws Exception {
		String body = mockMvc.perform(get("/api/notifications/my/unread-count")
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return Long.parseLong(body.replaceAll("\\D", ""));
	}

	private UserPrincipal workerPrincipal() {
		return new UserPrincipal(workerA.getId(), workerA.getEmail(), null, "WORKER", "WORKER",
				water.getId(), true);
	}

	private UserPrincipal waterAdminPrincipal() {
		return new UserPrincipal(waterAdmin.getId(), waterAdmin.getEmail(), null, "DEPARTMENT_ADMIN", null,
				water.getId(), true);
	}

	private AdminUser admin(String email, UserRole role, Department dept) {
		return AdminUser.builder().email(email).firstName("Admin").lastName("User")
				.passwordHash(passwordEncoder.encode("Password123!")).role(role).department(dept)
				.status(UserStatus.ACTIVE).build();
	}

	private String token(Long id, String email, String role, Long departmentId) {
		return jwtTokenProvider.generateTokenFromUserPrincipal(
				new UserPrincipal(id, email, null, role, null, departmentId, true));
	}
}
