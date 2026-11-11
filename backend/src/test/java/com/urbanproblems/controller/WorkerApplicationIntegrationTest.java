package com.urbanproblems.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.urbanproblems.IntegrationTestBase;
import com.urbanproblems.domain.entity.AdminUser;
import com.urbanproblems.domain.entity.Department;
import com.urbanproblems.domain.entity.Worker;
import com.urbanproblems.domain.enums.DepartmentStatus;
import com.urbanproblems.domain.enums.UserRole;
import com.urbanproblems.domain.enums.UserStatus;
import com.urbanproblems.dto.AuthRequestDto;
import com.urbanproblems.dto.CitizenRegistrationDto;
import com.urbanproblems.dto.WorkerRegistrationDto;

/**
 * Worker application lifecycle: public registration creates a pending account, a system
 * administrator approves or rejects it, and login/authorization follow the live account status.
 */
class WorkerApplicationIntegrationTest extends IntegrationTestBase {

	private final ObjectMapper json = new ObjectMapper();

	private Department water;
	private String systemAdminToken;
	private String departmentAdminToken;

	@BeforeEach
	void setUp() {
		cleanupTestData();
		water = seedDepartments().get("Water and Sanitation");

		AdminUser systemAdmin = adminUserRepository.save(AdminUser.builder()
				.email("sys-admin@test.org").firstName("Sys").lastName("Admin")
				.passwordHash(passwordEncoder.encode("Password123!")).role(UserRole.SYSTEM_ADMIN)
				.status(UserStatus.ACTIVE).build());
		AdminUser departmentAdmin = adminUserRepository.save(AdminUser.builder()
				.email("water-admin@test.org").firstName("Water").lastName("Admin")
				.passwordHash(passwordEncoder.encode("Password123!")).role(UserRole.DEPARTMENT_ADMIN)
				.department(water).status(UserStatus.ACTIVE).build());

		systemAdminToken = generateToken(systemAdmin.getId(), systemAdmin.getEmail(), "SYSTEM_ADMIN", "ADMIN");
		departmentAdminToken = generateToken(departmentAdmin.getId(), departmentAdmin.getEmail(),
				"DEPARTMENT_ADMIN", "ADMIN");
	}

	@Test
	void publicWorkerRegistrationCreatesPendingAccountWithoutToken() throws Exception {
		String email = "apply@test.org";

		mockMvc.perform(post("/api/auth/register/worker")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(application(email, water.getId()))))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.email").value(email))
				.andExpect(jsonPath("$.status").value("INACTIVE"))
				.andExpect(jsonPath("$.departmentId").value(water.getId()))
				.andExpect(jsonPath("$.accessToken").doesNotExist())
				.andExpect(content().string(not(containsString("passwordHash"))));

		Worker worker = workerRepository.findByEmail(email).orElseThrow();
		assertEquals(UserStatus.INACTIVE, worker.getStatus());
	}

	@Test
	void citizenCanApplyAsWorkerWithSameEmailAndSeparatePassword() throws Exception {
		String email = "citizen-worker@test.org";
		String citizenPassword = "CitizenPass123!";
		String workerPassword = "WorkerPass456!";
		CitizenRegistrationDto citizen = CitizenRegistrationDto.builder()
				.email(email)
				.firstName("Casey")
				.lastName("Citizen")
				.phoneNumber("5551112222")
				.password(citizenPassword)
				.build();

		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(citizen)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.role").value("CITIZEN"));

		WorkerRegistrationDto workerApplication = application(email, water.getId());
		workerApplication.setPassword(workerPassword);
		mockMvc.perform(post("/api/auth/register/worker")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(workerApplication)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("INACTIVE"))
				.andExpect(jsonPath("$.accessToken").doesNotExist());

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(workspaceLogin(email, citizenPassword, "CITIZEN"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.role").value("CITIZEN"));

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(workspaceLogin(email, workerPassword, "WORKER"))))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").value("ACCOUNT_INACTIVE"));

		Long workerId = workerRepository.findByEmail(email).orElseThrow().getId();
		mockMvc.perform(post("/api/workers/" + workerId + "/approve")
				.header("Authorization", "Bearer " + systemAdminToken))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(workspaceLogin(email, workerPassword, "WORKER"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.role").value("WORKER"));

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(workspaceLogin(email, workerPassword, "CITIZEN"))))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void pendingWorkerCannotSignInUntilApproved() throws Exception {
		String email = registerApplicant("pending@test.org");

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(login(email))))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").value("ACCOUNT_INACTIVE"));
	}

	@Test
	void pendingListIsVisibleToSystemAdminOnly() throws Exception {
		String email = registerApplicant("list@test.org");

		mockMvc.perform(get("/api/workers/pending")
				.header("Authorization", "Bearer " + systemAdminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[*].email", hasItem(email)));

		mockMvc.perform(get("/api/workers/pending")
				.header("Authorization", "Bearer " + departmentAdminToken))
				.andExpect(status().isForbidden());

		mockMvc.perform(get("/api/workers/pending")
				.header("Authorization", "Bearer " + citizenToken()))
				.andExpect(status().isForbidden());
	}

	@Test
	void systemAdminApprovalAllowsSignInAndRemovesPendingApplication() throws Exception {
		String email = registerApplicant("approve@test.org");
		Long workerId = workerRepository.findByEmail(email).orElseThrow().getId();

		mockMvc.perform(post("/api/workers/" + workerId + "/approve")
				.header("Authorization", "Bearer " + systemAdminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ACTIVE"));

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(login(email))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").doesNotExist())
				.andExpect(jsonPath("$.id").value(workerId))
				.andExpect(jsonPath("$.role").value("WORKER"));

		mockMvc.perform(get("/api/workers/pending")
				.header("Authorization", "Bearer " + systemAdminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[*].email", not(hasItem(email))));
	}

	@Test
	void departmentAdminCannotApproveOrRejectApplications() throws Exception {
		String email = registerApplicant("scope@test.org");
		Long workerId = workerRepository.findByEmail(email).orElseThrow().getId();

		mockMvc.perform(post("/api/workers/" + workerId + "/approve")
				.header("Authorization", "Bearer " + departmentAdminToken))
				.andExpect(status().isForbidden());

		mockMvc.perform(post("/api/workers/" + workerId + "/reject")
				.header("Authorization", "Bearer " + departmentAdminToken))
				.andExpect(status().isForbidden());

		assertEquals(UserStatus.INACTIVE, workerRepository.findById(workerId).orElseThrow().getStatus());
	}

	@Test
	void rejectedWorkerCannotSignInOrAppearInPendingApplications() throws Exception {
		String email = registerApplicant("reject@test.org");
		Long workerId = workerRepository.findByEmail(email).orElseThrow().getId();

		mockMvc.perform(post("/api/workers/" + workerId + "/reject")
				.header("Authorization", "Bearer " + systemAdminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("SUSPENDED"));

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(login(email))))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.error").value("ACCOUNT_INACTIVE"));

		mockMvc.perform(get("/api/workers/pending")
				.header("Authorization", "Bearer " + systemAdminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content[*].email", not(hasItem(email))));

		assertEquals(UserStatus.SUSPENDED, workerRepository.findById(workerId).orElseThrow().getStatus());
	}

	@Test
	void rejectedApplicationCannotBeApprovedThroughTheApprovalEndpoint() throws Exception {
		String email = registerApplicant("reject-then-approve@test.org");
		Long workerId = workerRepository.findByEmail(email).orElseThrow().getId();

		mockMvc.perform(post("/api/workers/" + workerId + "/reject")
				.header("Authorization", "Bearer " + systemAdminToken))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/workers/" + workerId + "/approve")
				.header("Authorization", "Bearer " + systemAdminToken))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error").value("CONFLICT"));

		assertEquals(UserStatus.SUSPENDED, workerRepository.findById(workerId).orElseThrow().getStatus());
	}

	@Test
	void tokenIssuedBeforeApprovalStopsWorkingUntilApproved() throws Exception {
		String email = registerApplicant("stale-token@test.org");
		Long workerId = workerRepository.findByEmail(email).orElseThrow().getId();
		String staleToken = generateToken(workerId, email, "WORKER", "WORKER");

		mockMvc.perform(get("/api/workers/me")
				.header("Authorization", "Bearer " + staleToken))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(post("/api/workers/" + workerId + "/approve")
				.header("Authorization", "Bearer " + systemAdminToken))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/workers/me")
				.header("Authorization", "Bearer " + staleToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(email))
				.andExpect(content().string(not(containsString("passwordHash"))));
	}

	@Test
	void applicationToInactiveDepartmentIsRejected() throws Exception {
		Department inactive = departmentRepository.save(Department.builder()
				.name("Inactive Department").description("Closed").headEmail("head@test.org")
				.status(DepartmentStatus.INACTIVE).build());

		mockMvc.perform(post("/api/auth/register/worker")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(application("closed@test.org", inactive.getId()))))
				.andExpect(status().isConflict());

		mockMvc.perform(post("/api/auth/register/worker")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(application("missing-dept@test.org", 999_999L))))
				.andExpect(status().isNotFound());
	}

	// ---- helpers ----

	private String registerApplicant(String email) throws Exception {
		mockMvc.perform(post("/api/auth/register/worker")
				.contentType(MediaType.APPLICATION_JSON)
				.content(json.writeValueAsString(application(email, water.getId()))))
				.andExpect(status().isCreated());
		return email;
	}

	private String citizenToken() {
		var citizen = citizenRepository.save(com.urbanproblems.domain.entity.Citizen.builder()
				.email("citizen-" + System.nanoTime() + "@test.org").firstName("C").lastName("T")
				.phoneNumber("5550000001").passwordHash(passwordEncoder.encode("Password123!"))
				.status(UserStatus.ACTIVE).build());
		return generateToken(citizen.getId(), citizen.getEmail(), "CITIZEN", "CITIZEN");
	}

	private WorkerRegistrationDto application(String email, Long departmentId) {
		return WorkerRegistrationDto.builder()
				.email(email).firstName("App").lastName("Licant").phoneNumber("5550000100")
				.password("Password123!").departmentId(departmentId).build();
	}

	private AuthRequestDto login(String email) {
		return AuthRequestDto.builder().email(email).password("Password123!").build();
	}

	private AuthRequestDto workspaceLogin(String email, String password, String workspace) {
		return AuthRequestDto.builder().email(email).password(password).workspace(workspace).build();
	}
}
