package com.urbanproblems.security;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import com.urbanproblems.dto.ComplaintRequestDto;
import com.urbanproblems.dto.ComplaintResponseDto;
import com.urbanproblems.repository.ComplaintRepository;
import com.urbanproblems.service.AssignmentService;
import com.urbanproblems.service.ComplaintService;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Security regression tests. Each test asserts a concrete status code or data property that a
 * vulnerability would violate. Tokens are generated directly, so these tests do not depend on login.
 *
 * Note: these tests were written in an environment without Maven/network access and have NOT been
 * executed. Run `./mvnw clean verify` before relying on them.
 */
class SecurityRegressionTest extends IntegrationTestBase {

	private static final String TEST_SECRET = "test-secret-key-for-testing-only-must-be-at-least-32-characters-long";

	@Autowired
	private ComplaintService complaintService;

	@Autowired
	private AssignmentService assignmentService;

	@Autowired
	private ComplaintRepository complaintRepository;

	@Autowired
	private PasswordEncoder encoder;

	private final ObjectMapper json = new ObjectMapper();

	private Department water;
	private Department electricity;
	private Citizen citizenA;
	private Citizen citizenB;
	private Worker workerA;
	private Worker workerB;
	private AdminUser waterAdmin;
	private AdminUser electricityAdmin;
	private AdminUser systemAdmin;

	private String citizenAToken;
	private String citizenBToken;
	private String workerAToken;
	private String workerBToken;
	private String waterAdminToken;
	private String electricityAdminToken;
	private String systemAdminToken;

	private Long complaintId;
	private String complaintPublicId;

	@BeforeEach
	void setUp() {
		cleanupTestData();

		java.util.Map<String, Department> departments = seedDepartments();
		water = departments.get("Water and Sanitation");
		electricity = departments.get("Electricity");

		citizenA = citizenRepository.save(Citizen.builder()
				.email("citizen-a@test.org").firstName("A").lastName("One").phoneNumber("5550000001")
				.passwordHash(encoder.encode("Password123!")).status(UserStatus.ACTIVE).build());
		citizenB = citizenRepository.save(Citizen.builder()
				.email("citizen-b@test.org").firstName("B").lastName("Two").phoneNumber("5550000002")
				.passwordHash(encoder.encode("Password123!")).status(UserStatus.ACTIVE).build());

		workerA = workerRepository.save(worker("worker-a@test.org", water));
		workerB = workerRepository.save(worker("worker-b@test.org", water));

		waterAdmin = adminUserRepository.save(admin("water-admin@test.org", UserRole.DEPARTMENT_ADMIN, water));
		electricityAdmin = adminUserRepository.save(admin("elec-admin@test.org", UserRole.DEPARTMENT_ADMIN, electricity));
		systemAdmin = adminUserRepository.save(admin("sys-admin@test.org", UserRole.SYSTEM_ADMIN, null));

		citizenAToken = token(citizenA.getId(), citizenA.getEmail(), "CITIZEN", "CITIZEN", null);
		citizenBToken = token(citizenB.getId(), citizenB.getEmail(), "CITIZEN", "CITIZEN", null);
		workerAToken = token(workerA.getId(), workerA.getEmail(), "WORKER", "WORKER", water.getId());
		workerBToken = token(workerB.getId(), workerB.getEmail(), "WORKER", "WORKER", water.getId());
		waterAdminToken = token(waterAdmin.getId(), waterAdmin.getEmail(), "DEPARTMENT_ADMIN", null, water.getId());
		electricityAdminToken = token(electricityAdmin.getId(), electricityAdmin.getEmail(),
				"DEPARTMENT_ADMIN", null, electricity.getId());
		systemAdminToken = token(systemAdmin.getId(), systemAdmin.getEmail(), "SYSTEM_ADMIN", null, null);

		ComplaintResponseDto complaint = complaintService.createComplaint(citizenA.getId(),
				ComplaintRequestDto.builder()
						.title("Water leak on main road")
						.description("Large water leak near the bus stop")
						.category(ComplaintCategory.WATER_SUPPLY)
						.location("Main Road, Sector 4")
						.latitude(22.7196)
						.longitude(75.8577)
						.build());
		complaintId = complaint.getId();
		complaintPublicId = complaint.getPublicId();
	}

	// ---- Authentication ----

	@Test
	void unauthenticatedRequestToProtectedEndpointReturns401() throws Exception {
		mockMvc.perform(get("/api/complaints/" + complaintPublicId))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void tamperedTokenIsRejectedWith401() throws Exception {
		String tampered = citizenAToken.substring(0, citizenAToken.length() - 2)
				+ (citizenAToken.endsWith("A") ? "B" : "A");
		mockMvc.perform(get("/api/complaints/" + complaintPublicId)
				.header("Authorization", "Bearer " + tampered))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void expiredTokenIsRejectedWith401() throws Exception {
		String expired = Jwts.builder()
				.subject(citizenA.getEmail())
				.claim("userId", citizenA.getId())
				.claim("role", "CITIZEN")
				.issuedAt(new Date(System.currentTimeMillis() - 7_200_000L))
				.expiration(new Date(System.currentTimeMillis() - 3_600_000L))
				.signWith(Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8)))
				.compact();
		mockMvc.perform(get("/api/complaints/" + complaintPublicId)
				.header("Authorization", "Bearer " + expired))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void tokenSignedWithAnotherKeyIsRejectedWith401() throws Exception {
		String forged = Jwts.builder()
				.subject(citizenA.getEmail())
				.claim("userId", citizenA.getId())
				.claim("role", "SYSTEM_ADMIN")
				.expiration(new Date(System.currentTimeMillis() + 3_600_000L))
				.signWith(Keys.hmacShaKeyFor("an-attacker-controlled-secret-key-of-sufficient-length".getBytes(StandardCharsets.UTF_8)))
				.compact();
		mockMvc.perform(get("/api/complaints/" + complaintPublicId)
				.header("Authorization", "Bearer " + forged))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void registrationCannotSelfPromoteToAdmin() throws Exception {
		String body = """
				{"email":"promote@test.org","firstName":"P","lastName":"Q","phoneNumber":"5550000009",
				 "password":"Password123!","role":"SYSTEM_ADMIN",
				 "securityQuestion":"What city were you born in?","securityAnswer":"Testville"}""";
		mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.role").value("CITIZEN"))
				.andExpect(content().string(not(containsString("passwordHash"))));
	}

	// ---- Complaint ownership and department isolation ----

	@Test
	void citizenCannotReadAnotherCitizensComplaint() throws Exception {
		mockMvc.perform(get("/api/complaints/" + complaintPublicId)
				.header("Authorization", "Bearer " + citizenBToken))
				.andExpect(status().isForbidden());
	}

	@Test
	void ownerCanReadComplaintAndResponseNeverContainsPasswordHash() throws Exception {
		mockMvc.perform(get("/api/complaints/" + complaintPublicId)
				.header("Authorization", "Bearer " + citizenAToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.publicId").value(complaintPublicId))
				.andExpect(content().string(not(containsString("passwordHash"))));
	}

	@Test
	void departmentAdminCannotListAnotherDepartmentsComplaints() throws Exception {
		mockMvc.perform(get("/api/complaints/department/" + water.getId())
				.header("Authorization", "Bearer " + electricityAdminToken))
				.andExpect(status().isForbidden());
	}

	@Test
	void departmentAdminCanListOwnDepartmentsComplaints() throws Exception {
		mockMvc.perform(get("/api/complaints/department/" + water.getId())
				.header("Authorization", "Bearer " + waterAdminToken))
				.andExpect(status().isOk());
	}

	// ---- Assignment ----

	@Test
	void citizenCannotAssignWorkers() throws Exception {
		mockMvc.perform(post("/api/assignments/complaints/" + complaintId + "/assign")
				.header("Authorization", "Bearer " + citizenAToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"workerId\":" + workerA.getId() + "}"))
				.andExpect(status().isForbidden());
		assertEquals(ComplaintStatus.SUBMITTED, complaintRepository.findById(complaintId).orElseThrow().getStatus());
	}

	@Test
	void adminOfAnotherDepartmentCannotAssignComplaint() throws Exception {
		mockMvc.perform(post("/api/assignments/complaints/" + complaintId + "/assign")
				.header("Authorization", "Bearer " + electricityAdminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"workerId\":" + workerA.getId() + "}"))
				.andExpect(status().isForbidden());
		assertEquals(ComplaintStatus.SUBMITTED, complaintRepository.findById(complaintId).orElseThrow().getStatus());
	}

	@Test
	void departmentAdminAssignsWorkerWithinOwnDepartment() throws Exception {
		mockMvc.perform(post("/api/assignments/complaints/" + complaintId + "/assign")
				.header("Authorization", "Bearer " + waterAdminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"workerId\":" + workerA.getId() + "}"))
				.andExpect(status().isCreated());
		assertEquals(ComplaintStatus.ASSIGNED, complaintRepository.findById(complaintId).orElseThrow().getStatus());
	}

	@Test
	void workerCannotViewAnotherWorkersAssignment() throws Exception {
		Long assignmentId = assignWorker(workerA);

		mockMvc.perform(get("/api/assignments/" + assignmentId)
				.header("Authorization", "Bearer " + workerBToken))
				.andExpect(status().isForbidden());

		mockMvc.perform(get("/api/assignments/" + assignmentId)
				.header("Authorization", "Bearer " + workerAToken))
				.andExpect(status().isOk());
	}

	// ---- Resolution review ----

	@Test
	void workerCannotApproveResolutions() throws Exception {
		mockMvc.perform(post("/api/resolutions/1/approve")
				.header("Authorization", "Bearer " + workerAToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"reviewStatus\":\"APPROVED\"}"))
				.andExpect(status().isForbidden());
	}

	// ---- Evidence ----

	@Test
	void citizenCannotDownloadAnotherCitizensEvidence() throws Exception {
		byte[] pdf = "%PDF-1.4 test document body".getBytes(StandardCharsets.US_ASCII);
		MvcResult upload = mockMvc.perform(multipart("/api/complaints/" + complaintId + "/evidence/upload")
				.file(new MockMultipartFile("file", "photo.pdf", "application/pdf", pdf))
				.header("Authorization", "Bearer " + citizenAToken))
				.andExpect(status().isCreated())
				.andReturn();
		long evidenceId = json.readTree(upload.getResponse().getContentAsString()).get("id").asLong();

		mockMvc.perform(get("/api/complaints/" + complaintId + "/evidence/" + evidenceId + "/download")
				.header("Authorization", "Bearer " + citizenBToken))
				.andExpect(status().isForbidden());

		mockMvc.perform(get("/api/complaints/" + complaintId + "/evidence/" + evidenceId + "/download")
				.header("Authorization", "Bearer " + citizenAToken))
				.andExpect(status().isOk());
	}

	@Test
	void uploadRejectsContentThatDoesNotMatchItsExtension() throws Exception {
		byte[] executable = new byte[] {'M', 'Z', (byte) 0x90, 0x00, 0x03, 0x00};
		mockMvc.perform(multipart("/api/complaints/" + complaintId + "/evidence/upload")
				.file(new MockMultipartFile("file", "invoice.pdf", "application/pdf", executable))
				.header("Authorization", "Bearer " + citizenAToken))
				.andExpect(status().isBadRequest());
	}

	@Test
	void uploadedFilesAreNotExposedAsStaticResources() throws Exception {
		mockMvc.perform(get("/uploads/anything.pdf").header("Authorization", "Bearer " + citizenAToken))
				.andExpect(status().isNotFound());
	}

	// ---- Recurrence ----

	@Test
	void citizensCannotReadRecurrenceReports() throws Exception {
		mockMvc.perform(get("/api/recurrence/complaints/" + complaintId + "/report")
				.header("Authorization", "Bearer " + citizenAToken))
				.andExpect(status().isForbidden());
	}

	// ---- helpers ----

	private Long assignWorker(Worker worker) {
		UserPrincipal admin = new UserPrincipal(waterAdmin.getId(), waterAdmin.getEmail(), null,
				"DEPARTMENT_ADMIN", null, water.getId(), true);
		return assignmentService.assignComplaintToWorker(complaintId,
				AssignmentRequestDto.builder().workerId(worker.getId()).build(), admin).getId();
	}

	private String token(Long id, String email, String role, String type, Long departmentId) {
		return jwtTokenProvider.generateTokenFromUserPrincipal(
				new UserPrincipal(id, email, null, role, type, departmentId, true));
	}

	private Worker worker(String email, Department dept) {
		return Worker.builder().email(email).firstName("W").lastName("Worker").phoneNumber("5550000100")
				.passwordHash(encoder.encode("Password123!")).department(dept).status(UserStatus.ACTIVE).build();
	}

	private AdminUser admin(String email, UserRole role, Department dept) {
		return AdminUser.builder().email(email).firstName("Admin").lastName("User")
				.passwordHash(encoder.encode("Password123!")).role(role).department(dept)
				.status(UserStatus.ACTIVE).build();
	}

}
