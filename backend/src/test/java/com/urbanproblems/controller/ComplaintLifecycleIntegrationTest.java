package com.urbanproblems.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.urbanproblems.IntegrationTestBase;
import com.urbanproblems.domain.entity.AdminUser;
import com.urbanproblems.domain.entity.Citizen;
import com.urbanproblems.domain.entity.Department;
import com.urbanproblems.domain.entity.Worker;
import com.urbanproblems.domain.enums.UserRole;
import com.urbanproblems.domain.enums.UserStatus;

/**
 * Full complaint lifecycle over HTTP: citizen reports with a pinned location, department admin
 * assigns, worker starts and uploads evidence, submits, admin approves or rejects, and the citizen
 * and worker receive notifications. Also covers the duplicate-submission and coordinate guards.
 */
class ComplaintLifecycleIntegrationTest extends IntegrationTestBase {

	private final ObjectMapper json = new ObjectMapper();

	private Department water;
	private Department electricity;
	private Citizen citizen;
	private Worker workerA;
	private Worker workerB;
	private AdminUser waterAdmin;
	private AdminUser electricityAdmin;

	private String citizenToken;
	private String workerAToken;
	private String workerBToken;
	private String waterAdminToken;
	private String electricityAdminToken;

	@BeforeEach
	void setUp() {
		cleanupTestData();
		java.util.Map<String, Department> departments = seedDepartments();
		water = departments.get("Water and Sanitation");
		electricity = departments.get("Electricity");

		citizen = citizenRepository.save(Citizen.builder()
				.email("lifecycle-citizen@test.org").firstName("Life").lastName("Cycle")
				.phoneNumber("5550000001").passwordHash(passwordEncoder.encode("Password123!"))
				.status(UserStatus.ACTIVE).build());
		workerA = workerRepository.save(worker("lifecycle-worker-a@test.org", water));
		workerB = workerRepository.save(worker("lifecycle-worker-b@test.org", water));
		waterAdmin = adminUserRepository.save(admin("lifecycle-water-admin@test.org", UserRole.DEPARTMENT_ADMIN, water));
		electricityAdmin = adminUserRepository.save(
				admin("lifecycle-elec-admin@test.org", UserRole.DEPARTMENT_ADMIN, electricity));

		citizenToken = token(citizen.getId(), citizen.getEmail(), "CITIZEN", null);
		workerAToken = token(workerA.getId(), workerA.getEmail(), "WORKER", water.getId());
		workerBToken = token(workerB.getId(), workerB.getEmail(), "WORKER", water.getId());
		waterAdminToken = token(waterAdmin.getId(), waterAdmin.getEmail(), "DEPARTMENT_ADMIN", water.getId());
		electricityAdminToken = token(electricityAdmin.getId(), electricityAdmin.getEmail(),
				"DEPARTMENT_ADMIN", electricity.getId());
	}

	@Test
	void complaintFlowFromReportToCitizenNotification() throws Exception {
		JsonNode complaint = createComplaint("Water leak near sector 4 market",
				"12 Main Road, Bhopal, Madhya Pradesh 462001", 23.2599, 77.4126);
		Long complaintId = complaint.get("id").asLong();
		String publicId = complaint.get("publicId").asText();

		// Location is persisted exactly as reported.
		mockMvc.perform(get("/api/complaints/" + publicId)
				.header("Authorization", "Bearer " + citizenToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.location").value("12 Main Road, Bhopal, Madhya Pradesh 462001"))
				.andExpect(jsonPath("$.latitude").value(23.2599))
				.andExpect(jsonPath("$.longitude").value(77.4126))
				.andExpect(jsonPath("$.status").value("SUBMITTED"));

		Long assignmentId = assign(complaintId, workerA.getId());
		mockMvc.perform(get("/api/assignments/" + assignmentId)
				.header("Authorization", "Bearer " + workerAToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.complaintPublicId").value(publicId))
				.andExpect(jsonPath("$.status").value("ASSIGNED"));

		// Worker task detail exposes the actual complaint location.
		mockMvc.perform(get("/api/complaints/" + publicId)
				.header("Authorization", "Bearer " + workerAToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.location").value("12 Main Road, Bhopal, Madhya Pradesh 462001"))
				.andExpect(jsonPath("$.latitude").value(23.2599));

		// Another worker cannot act on this task.
		mockMvc.perform(post("/api/assignments/" + assignmentId + "/start")
				.header("Authorization", "Bearer " + workerBToken))
				.andExpect(status().isForbidden());

		mockMvc.perform(post("/api/assignments/" + assignmentId + "/start")
				.header("Authorization", "Bearer " + workerAToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("IN_PROGRESS"));

		uploadEvidence(complaintId, workerAToken, "before.pdf", "%PDF-1.4 before photo");
		uploadEvidence(complaintId, workerAToken, "after.pdf", "%PDF-1.4 after photo");

		JsonNode resolution = submitResolution(assignmentId);
		long resolutionId = resolution.get("id").asLong();

		mockMvc.perform(get("/api/complaints/" + publicId)
				.header("Authorization", "Bearer " + citizenToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("PENDING_VERIFICATION"));

		// A different department cannot review this work.
		mockMvc.perform(post("/api/resolutions/" + resolutionId + "/approve")
				.header("Authorization", "Bearer " + electricityAdminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"reviewStatus\":\"APPROVED\",\"adminNotes\":\"OK\"}"))
				.andExpect(status().isForbidden());

		mockMvc.perform(post("/api/resolutions/" + resolutionId + "/approve")
				.header("Authorization", "Bearer " + waterAdminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"reviewStatus\":\"APPROVED\",\"adminNotes\":\"Work verified on site.\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.reviewStatus").value("APPROVED"));

		mockMvc.perform(get("/api/complaints/" + publicId)
				.header("Authorization", "Bearer " + citizenToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("RESOLVED"));

		// Final state is recorded in the complaint history.
		mockMvc.perform(get("/api/complaints/" + publicId + "/history")
				.header("Authorization", "Bearer " + citizenToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].toStatus", hasItem("RESOLVED")))
				.andExpect(jsonPath("$[*].actorRole", hasItem("DEPARTMENT_ADMIN")));

		// Citizen and worker were notified about the resolution.
		mockMvc.perform(get("/api/notifications/my")
				.header("Authorization", "Bearer " + citizenToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[*].complaintPublicId", hasItem(publicId)));
		mockMvc.perform(get("/api/notifications/my")
				.header("Authorization", "Bearer " + workerAToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[*].complaintPublicId", hasItem(publicId)));
	}

	@Test
	void rejectionReturnsTaskForReworkAndNotifiesWorker() throws Exception {
		JsonNode complaint = createComplaint("Drain blocked behind community hall",
				"Community Hall Lane, Bhopal, Madhya Pradesh 462003", 23.2500, 77.4200);
		Long complaintId = complaint.get("id").asLong();
		String publicId = complaint.get("publicId").asText();

		Long assignmentId = assign(complaintId, workerA.getId());
		mockMvc.perform(post("/api/assignments/" + assignmentId + "/start")
				.header("Authorization", "Bearer " + workerAToken))
				.andExpect(status().isOk());
		uploadEvidence(complaintId, workerAToken, "drain.pdf", "%PDF-1.4 drain photo");

		long resolutionId = submitResolution(assignmentId).get("id").asLong();

		mockMvc.perform(post("/api/resolutions/" + resolutionId + "/reject")
				.header("Authorization", "Bearer " + waterAdminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"reviewStatus\":\"REJECTED\",\"rejectionReason\":\"Show the cleared drain.\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.reviewStatus").value("REJECTED"))
				.andExpect(jsonPath("$.rejectionReason").value("Show the cleared drain."));

		mockMvc.perform(get("/api/complaints/" + publicId)
				.header("Authorization", "Bearer " + citizenToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("IN_PROGRESS"));

		// The worker sees the feedback and can resubmit without a new assignment.
		mockMvc.perform(get("/api/resolutions/assignments/" + assignmentId + "/latest")
				.header("Authorization", "Bearer " + workerAToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.rejectionReason").value("Show the cleared drain."));

		mockMvc.perform(get("/api/notifications/my")
				.header("Authorization", "Bearer " + workerAToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[*].complaintPublicId", hasItem(publicId)));

		mockMvc.perform(post("/api/resolutions/assignments/" + assignmentId + "/submit")
				.header("Authorization", "Bearer " + workerAToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"completionNotes\":\"Cleared the drain and photographed it.\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.reviewStatus").value("PENDING"));

		mockMvc.perform(post("/api/resolutions/" + resolutionId + "/approve")
				.header("Authorization", "Bearer " + waterAdminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"reviewStatus\":\"APPROVED\",\"adminNotes\":\"Verified.\"}"))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/complaints/" + publicId)
				.header("Authorization", "Bearer " + citizenToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("RESOLVED"));
	}

	@Test
	void duplicateComplaintAndInvalidCoordinatesAreRejected() throws Exception {
		createComplaint("Streetlight not working on park road",
				"Park Road, Bhopal, Madhya Pradesh 462001", 23.2599, 77.4126);

		mockMvc.perform(post("/api/complaints")
				.header("Authorization", "Bearer " + citizenToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(complaintJson("Streetlight not working on park road",
						"Park Road, Bhopal, Madhya Pradesh 462001", 23.2599, 77.4126)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("CONFLICT"));

		mockMvc.perform(post("/api/complaints")
				.header("Authorization", "Bearer " + citizenToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"title":"Coordinates missing longitude","description":"Only latitude was supplied here",
						 "category":"WATER_SUPPLY","location":"Somewhere in Bhopal 462001","latitude":23.2}
						"""))
				.andExpect(status().isBadRequest());

		mockMvc.perform(post("/api/complaints")
				.header("Authorization", "Bearer " + citizenToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(complaintJson("Coordinates outside valid range",
						"Somewhere in Bhopal 462001", 230.0, 77.4)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void evidenceUploadStopsOnceWorkIsSubmittedForReview() throws Exception {
		JsonNode complaint = createComplaint("Overflowing bin near bus depot",
				"Bus Depot Road, Bhopal, Madhya Pradesh 462008", 23.2400, 77.4300);
		Long complaintId = complaint.get("id").asLong();
		Long assignmentId = assign(complaintId, workerA.getId());

		// Not started yet: evidence is allowed while assigned.
		uploadEvidence(complaintId, workerAToken, "first.pdf", "%PDF-1.4 first photo");

		mockMvc.perform(post("/api/assignments/" + assignmentId + "/start")
				.header("Authorization", "Bearer " + workerAToken))
				.andExpect(status().isOk());
		submitResolution(assignmentId);

		uploadEvidenceExpectingStatus(complaintId, workerAToken, "late.pdf", "%PDF-1.4 late photo", 409);
	}

	// ---- helpers ----

	private JsonNode createComplaint(String title, String location, double latitude, double longitude)
			throws Exception {
		MvcResult result = mockMvc.perform(post("/api/complaints")
				.header("Authorization", "Bearer " + citizenToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(complaintJson(title, location, latitude, longitude)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.title").value(title))
				.andReturn();
		return read(result);
	}

	private String complaintJson(String title, String location, double latitude, double longitude) {
		return """
				{"title":"%s","description":"Reported through the full lifecycle integration test",
				 "category":"WATER_SUPPLY","location":"%s","latitude":%s,"longitude":%s}
				""".formatted(title, location, latitude, longitude);
	}

	private Long assign(Long complaintId, Long workerId) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/assignments/complaints/" + complaintId + "/assign")
				.header("Authorization", "Bearer " + waterAdminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"workerId\":" + workerId + ",\"assignmentNotes\":\"Please inspect today.\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("ASSIGNED"))
				.andReturn();
		return read(result).get("id").asLong();
	}

	private JsonNode submitResolution(Long assignmentId) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/resolutions/assignments/" + assignmentId + "/submit")
				.header("Authorization", "Bearer " + workerAToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"completionNotes\":\"Repair finished and cleaned up.\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.reviewStatus").value("PENDING"))
				.andReturn();
		return read(result);
	}

	private void uploadEvidence(Long complaintId, String token, String filename, String body) throws Exception {
		uploadEvidenceExpectingStatus(complaintId, token, filename, body, 201);
	}

	private void uploadEvidenceExpectingStatus(Long complaintId, String token, String filename, String body,
			int expectedStatus) throws Exception {
		byte[] pdf = body.getBytes(StandardCharsets.US_ASCII);
		mockMvc.perform(multipart("/api/complaints/" + complaintId + "/evidence/upload")
				.file(new MockMultipartFile("file", filename, "application/pdf", pdf))
				.header("Authorization", "Bearer " + token))
				.andExpect(status().is(expectedStatus));
	}

	private JsonNode read(MvcResult result) throws Exception {
		return json.readTree(result.getResponse().getContentAsString());
	}

	private String token(Long id, String email, String role, Long departmentId) {
		return jwtTokenProvider.generateTokenFromUserPrincipal(
				new com.urbanproblems.security.UserPrincipal(id, email, null, role, null, departmentId, true));
	}

	private Worker worker(String email, Department department) {
		return Worker.builder().email(email).firstName("W").lastName("Worker").phoneNumber("5550000100")
				.passwordHash(passwordEncoder.encode("Password123!")).department(department)
				.status(UserStatus.ACTIVE).build();
	}

	private AdminUser admin(String email, UserRole role, Department department) {
		return AdminUser.builder().email(email).firstName("Admin").lastName("User")
				.passwordHash(passwordEncoder.encode("Password123!")).role(role).department(department)
				.status(UserStatus.ACTIVE).build();
	}
}
