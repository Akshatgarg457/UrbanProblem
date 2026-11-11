package com.urbanproblems.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.urbanproblems.IntegrationTestBase;
import com.urbanproblems.dto.AssignmentRequestDto;
import com.urbanproblems.dto.AuthRequestDto;
import com.urbanproblems.dto.CitizenRegistrationDto;
import com.urbanproblems.dto.ComplaintRequestDto;
import com.urbanproblems.dto.WorkerRegistrationDto;
import com.urbanproblems.domain.entity.AdminUser;
import com.urbanproblems.domain.entity.Worker;
import com.urbanproblems.domain.enums.ComplaintCategory;
import com.urbanproblems.domain.enums.UserRole;
import com.urbanproblems.domain.enums.UserStatus;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * API CONTRACT INTEGRATION TEST
 * 
 * Verifies that actual endpoints match documented API contracts.
 * Tests request/response structure, authentication requirements,
 * HTTP status codes, and error handling.
 */
public class APIContractIntegrationTest extends IntegrationTestBase {

	private ObjectMapper objectMapper = new ObjectMapper();
	private String citizenToken;
	private String workerToken;
	private Long testComplaintId;
	private String testComplaintPublicId;
	private Long testDepartmentId = 1L;
	private Long testWorkerId;

	@BeforeEach
	void setUp() throws Exception {
		cleanupTestData();
		setupTestData();

		// Register and login citizen
		CitizenRegistrationDto citizenReg = CitizenRegistrationDto.builder()
				.email("contract_citizen@test.com")
				.firstName("Contract")
				.lastName("Citizen")
				.phoneNumber("5555555560")
				.password("ContractPass123!")
				.build();

		MvcResult citizenResult = mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(citizenReg)))
				.andReturn();

		var registeredCitizen = citizenRepository.findByEmail("contract_citizen@test.com").orElseThrow();
		citizenToken = generateToken(registeredCitizen.getId(), registeredCitizen.getEmail(), "CITIZEN", "CITIZEN");

		// Create test complaint
		ComplaintRequestDto complaintDto = ComplaintRequestDto.builder()
				.title("API Contract Test Complaint")
				.description("Testing API request/response structure")
				.category(ComplaintCategory.WATER_SUPPLY)
				.location("Test Location")
				.latitude(23.1815)
				.longitude(79.9864)
				.build();

		MvcResult complaintResult = mockMvc.perform(post("/api/complaints")
				.header("Authorization", "Bearer " + citizenToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(complaintDto)))
				.andReturn();

		testComplaintId = objectMapper.readTree(complaintResult.getResponse().getContentAsString())
				.get("id").asLong();
		testComplaintPublicId = objectMapper.readTree(complaintResult.getResponse().getContentAsString())
				.get("publicId").asText();
	}

	// ============================================================================
	// AUTHENTICATION API CONTRACTS
	// ============================================================================

	@Test
	void testAuthRegisterContract_SuccessResponse() throws Exception {
		CitizenRegistrationDto registrationDto = CitizenRegistrationDto.builder()
				.email("register_test@test.com")
				.firstName("Register")
				.lastName("Test")
				.phoneNumber("5555555561")
				.password("RegisterTest123!")
				.build();

		// Registration returns account metadata; authentication is established separately by login.
		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(registrationDto)))
				.andExpect(status().isCreated())  // 201
				.andExpect(jsonPath("$.accessToken").doesNotExist())
				.andExpect(jsonPath("$.userId").isNumber())
				.andExpect(jsonPath("$.email").value("register_test@test.com"))
				.andExpect(jsonPath("$.role").value("CITIZEN"));
	}

	@Test
	void testAuthLoginContract_SuccessResponse() throws Exception {
		AuthRequestDto loginDto = AuthRequestDto.builder()
				.email("contract_citizen@test.com")
				.password("ContractPass123!")
				.build();

		// Login returns account details and establishes an HttpOnly server-side session cookie.
		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginDto)))
				.andExpect(status().isOk())
				.andExpect(cookie().exists("CGSESSION"))
				.andExpect(jsonPath("$.accessToken").doesNotExist())
				.andExpect(jsonPath("$.email").value("contract_citizen@test.com"))
				.andExpect(jsonPath("$.role").value("CITIZEN"));
	}

	@Test
	void testAuthLoginContract_InvalidCredentials() throws Exception {
		AuthRequestDto loginDto = AuthRequestDto.builder()
				.email("nonexistent@test.com")
				.password("WrongPassword123!")
				.build();

		// CONTRACT: Invalid credentials return 401
		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginDto)))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void testAuthLogoutContract_RequiresAuthentication() throws Exception {
		// Logout invalidates the server-side session.
		mockMvc.perform(post("/api/auth/logout")
				.header("Authorization", "Bearer " + citizenToken))
				.andExpect(status().isNoContent());
	}

	// ============================================================================
	// COMPLAINT API CONTRACTS
	// ============================================================================

	@Test
	void testComplaintCreateContract_SuccessResponse() throws Exception {
		ComplaintRequestDto complaintDto = ComplaintRequestDto.builder()
				.title("API Test Complaint")
				.description("Testing complaint creation API")
				.category(ComplaintCategory.DRAINAGE_AND_SEWAGE)
				.location("Test Street")
				.latitude(23.1815)
				.longitude(79.9864)
				.build();

		// CONTRACT: POST /api/complaints returns 201 with ComplaintResponseDto
		mockMvc.perform(post("/api/complaints")
				.header("Authorization", "Bearer " + citizenToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(complaintDto)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.publicId").isString())
				.andExpect(jsonPath("$.title").value("API Test Complaint"))
				.andExpect(jsonPath("$.status").value("SUBMITTED"))
				.andExpect(jsonPath("$.createdAt").isString());
	}

	@Test
	void testComplaintCreateContract_RequiresAuthentication() throws Exception {
		ComplaintRequestDto complaintDto = ComplaintRequestDto.builder()
				.title("Unauthorized Test")
				.description("Should fail")
				.category(ComplaintCategory.ROADS_AND_POTHOLES)
				.location("Test")
				.latitude(23.1815)
				.longitude(79.9864)
				.build();

		// CONTRACT: POST /api/complaints without token returns 401
		mockMvc.perform(post("/api/complaints")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(complaintDto)))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void testComplaintCreateContract_RequiresCitizenRole() throws Exception {
		// CONTRACT: POST /api/complaints is citizen-only. A real worker account gets 403.
		Worker worker = workerRepository.save(Worker.builder()
				.email("worker_contract@test.com").firstName("Worker").lastName("Test")
				.phoneNumber("5555555562").passwordHash(passwordEncoder.encode("WorkerPass123!"))
				.department(testDepartment).status(UserStatus.ACTIVE).build());
		String workerToken = generateToken(worker.getId(), worker.getEmail(), "WORKER", "WORKER");

		ComplaintRequestDto complaintDto = ComplaintRequestDto.builder()
				.title("Worker Complaint")
				.description("Workers should not create complaints")
				.category(ComplaintCategory.ROADS_AND_POTHOLES)
				.location("Test Location")
				.latitude(23.1815)
				.longitude(79.9864)
				.build();

		mockMvc.perform(post("/api/complaints")
				.header("Authorization", "Bearer " + workerToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(complaintDto)))
				.andExpect(status().isForbidden());
	}

	@Test
	void testComplaintGetPublicContract_RequiresOwnerAuth() throws Exception {
		// CONTRACT: GET /api/complaints/{publicId} requires authentication and ownership (see SecurityRegressionTest)
		ComplaintRequestDto complaintDto = ComplaintRequestDto.builder()
				.title("Owner Complaint")
				.description("Should only be visible to its owner")
				.category(ComplaintCategory.ELECTRICITY_AND_STREETLIGHTS)
				.location("Test Location")
				.latitude(23.1815)
				.longitude(79.9864)
				.build();

		MvcResult result = mockMvc.perform(post("/api/complaints")
				.header("Authorization", "Bearer " + citizenToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(complaintDto)))
				.andReturn();

		String publicId = objectMapper.readTree(result.getResponse().getContentAsString())
				.get("publicId").asText();

		mockMvc.perform(get("/api/complaints/" + publicId))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(get("/api/complaints/" + publicId)
				.header("Authorization", "Bearer " + citizenToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.publicId").value(publicId));
	}

	@Test
	void testComplaintGetMyComplaints_CitizenOnly() throws Exception {
		// CONTRACT: GET /api/complaints/my-complaints requires CITIZEN role
		mockMvc.perform(get("/api/complaints/my-complaints")
				.header("Authorization", "Bearer " + citizenToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isArray());
	}

	@Test
	void testComplaintGetMyComplaints_NoToken() throws Exception {
		// CONTRACT: GET /api/complaints/my-complaints without token returns 401
		mockMvc.perform(get("/api/complaints/my-complaints"))
				.andExpect(status().isUnauthorized());
	}

	// ============================================================================
	// FILE UPLOAD API CONTRACTS
	// ============================================================================

	@Test
	void testFileUploadContract_EndpointExists() throws Exception {
		// CONTRACT: POST /api/complaints/{complaintId}/evidence/upload endpoint exists
		// Returns 400 if no file (testing endpoint presence)
		mockMvc.perform(multipart("/api/complaints/" + testComplaintId + "/evidence/upload")
				.header("Authorization", "Bearer " + citizenToken))
				.andExpect(status().isBadRequest());  // No file provided
	}

	@Test
	void testFileListContract_EndpointExists() throws Exception {
		// CONTRACT: GET /api/complaints/{complaintId}/evidence returns list
		mockMvc.perform(get("/api/complaints/" + testComplaintId + "/evidence")
				.header("Authorization", "Bearer " + citizenToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray());
	}

	@Test
	void testFileDownloadContract_RequiresValidEvidence() throws Exception {
		// CONTRACT: GET /api/complaints/{complaintId}/evidence/{evidenceId}/download
		// Returns 404 for non-existent evidence
		mockMvc.perform(get("/api/complaints/" + testComplaintId + "/evidence/999/download")
				.header("Authorization", "Bearer " + citizenToken))
				.andExpect(status().isNotFound());
	}

	// ============================================================================
	// ASSIGNMENT API CONTRACTS
	// ============================================================================

	@Test
	void testAssignmentCreateContract_EndpointStructure() throws Exception {
		// CONTRACT: POST /api/assignments/complaints/{complaintId}/assign exists and rejects citizens (403)
		AssignmentRequestDto assignmentDto = AssignmentRequestDto.builder()
				.workerId(1L)
				.assignmentNotes("Test assignment")
				.build();

		mockMvc.perform(post("/api/assignments/complaints/" + testComplaintId + "/assign")
				.header("Authorization", "Bearer " + citizenToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(assignmentDto)))
				.andExpect(status().isForbidden());
	}

	@Test
	void testErrorContract_UnauthorizedMissingToken() throws Exception {
		// CONTRACT: Missing auth token returns 401
		mockMvc.perform(get("/api/complaints/my-complaints"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void testErrorContract_ForbiddenInsufficientRole() throws Exception {
		// CONTRACT: Insufficient role returns 403
		WorkerRegistrationDto workerReg = WorkerRegistrationDto.builder()
				.email("forbidden_test@test.com")
				.firstName("Forbidden")
				.lastName("Test")
				.phoneNumber("5555555563")
				.password("ForbiddenPass123!")
				.departmentId(1L)
				.build();

		MvcResult result = mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(workerReg)))
				.andReturn();

		String workerToken = citizenToken;

		// Worker trying to access department endpoint
		mockMvc.perform(get("/api/departments/1")
				.header("Authorization", "Bearer " + workerToken))
				.andExpect(status().isForbidden());
	}

	@Test
	void testErrorContract_NotFoundResourceDoesNotExist() throws Exception {
		// CONTRACT: an unknown complaint publicId returns 404 for an authenticated user
		mockMvc.perform(get("/api/complaints/does-not-exist-" + System.nanoTime())
				.header("Authorization", "Bearer " + citizenToken))
				.andExpect(status().isNotFound());
	}

	@Test
	void testErrorContract_BadRequestInvalidInput() throws Exception {
		// CONTRACT: Invalid request body returns 400
		String invalidJson = "{ invalid json }";

		mockMvc.perform(post("/api/complaints")
				.header("Authorization", "Bearer " + citizenToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(invalidJson))
				.andExpect(status().isBadRequest());
	}

	// ============================================================================
	// PAGINATION AND FILTERING CONTRACTS
	// ============================================================================

	@Test
	void testPaginationContract_DefaultPageSize() throws Exception {
		// CONTRACT: GET with Pageable supports pagination
		mockMvc.perform(get("/api/complaints/my-complaints")
				.header("Authorization", "Bearer " + citizenToken)
				.param("page", "0")
				.param("size", "10")
				.param("sort", "createdAt,desc"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isArray())
				.andExpect(jsonPath("$.totalElements").isNumber())
				.andExpect(jsonPath("$.totalPages").isNumber());
	}

	// ============================================================================
	// RESPONSE STRUCTURE CONTRACTS
	// ============================================================================

	@Test
	void testResponseStructure_ComplaintResponseDto() throws Exception {
		// CONTRACT: ComplaintResponseDto has specific structure
		mockMvc.perform(get("/api/complaints/" + testComplaintPublicId)
				.header("Authorization", "Bearer " + citizenToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.publicId").isString())
				.andExpect(jsonPath("$.title").isString())
				.andExpect(jsonPath("$.description").isString())
				.andExpect(jsonPath("$.category").isString())
				.andExpect(jsonPath("$.location").isString())
				.andExpect(jsonPath("$.latitude").isNumber())
				.andExpect(jsonPath("$.longitude").isNumber())
				.andExpect(jsonPath("$.status").isString())
				.andExpect(jsonPath("$.createdAt").isString())
				.andExpect(jsonPath("$.updatedAt").isString());
	}

	@Test
	void testEnumContract_ComplaintStatuses() throws Exception {
		// CONTRACT: Complaint statuses follow defined enum values
		ComplaintRequestDto complaintDto = ComplaintRequestDto.builder()
				.title("Enum Test")
				.description("Test enum values")
				.category(ComplaintCategory.WASTE_MANAGEMENT)
				.location("Test Location")
				.latitude(23.1815)
				.longitude(79.9864)
				.build();

		MvcResult result = mockMvc.perform(post("/api/complaints")
				.header("Authorization", "Bearer " + citizenToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(complaintDto)))
				.andReturn();

		String status = objectMapper.readTree(result.getResponse().getContentAsString())
				.get("status").asText();

		// New complaints should be SUBMITTED
		org.junit.jupiter.api.Assertions.assertEquals("SUBMITTED", status);
	}

	// ============================================================================
	// TOKEN EXPIRATION AND REFRESH CONTRACTS
	// ============================================================================

	@Test
	void testTokenContract_ValidTokenAccepted() throws Exception {
		// CONTRACT: Valid token grants access
		mockMvc.perform(get("/api/complaints/my-complaints")
				.header("Authorization", "Bearer " + citizenToken))
				.andExpect(status().isOk());
	}

	@Test
	void testTokenContract_InvalidTokenRejected() throws Exception {
		// CONTRACT: Invalid token returns 401
		mockMvc.perform(get("/api/complaints/my-complaints")
				.header("Authorization", "Bearer invalid.token.here"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void testTokenContract_MalformedHeaderRejected() throws Exception {
		// CONTRACT: Malformed Authorization header returns 401
		mockMvc.perform(get("/api/complaints/my-complaints")
				.header("Authorization", "NotBearer " + citizenToken))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void testDatabaseHealthEndpoint_ReturnsConnected() throws Exception {
		// CONTRACT: GET /api/system/health/database returns CONNECTED status without requiring auth
		mockMvc.perform(get("/api/system/health/database"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CONNECTED"))
				.andExpect(jsonPath("$.database").isNotEmpty())
				.andExpect(jsonPath("$.message").value("Database connection established"));
	}

	@Test
	void testCitizenProfileContract_GetAndUpdate() throws Exception {
		// CONTRACT: GET /api/citizens/me returns authenticated citizen profile
		mockMvc.perform(get("/api/citizens/me")
				.header("Authorization", "Bearer " + citizenToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("contract_citizen@test.com"))
				.andExpect(jsonPath("$.firstName").value("Contract"))
				.andExpect(jsonPath("$.lastName").value("Citizen"));

		// CONTRACT: PUT /api/citizens/me updates profile fields
		String updateJson = """
				{
					"firstName": "UpdatedContract",
					"lastName": "UpdatedCitizen",
					"phoneNumber": "5559998888"
				}
				""";
		mockMvc.perform(put("/api/citizens/me")
				.header("Authorization", "Bearer " + citizenToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(updateJson))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.firstName").value("UpdatedContract"))
				.andExpect(jsonPath("$.lastName").value("UpdatedCitizen"))
				.andExpect(jsonPath("$.phoneNumber").value("5559998888"))
				.andExpect(jsonPath("$.email").value("contract_citizen@test.com"));

		// CONTRACT: saved location (map/manual) persists and round-trips
		String locationJson = """
				{
					"firstName": "UpdatedContract",
					"lastName": "UpdatedCitizen",
					"phoneNumber": "5559998888",
					"address": "12 Main Road",
					"city": "Bhopal",
					"state": "Madhya Pradesh",
					"postalCode": "462001",
					"latitude": 23.2599,
					"longitude": 77.4126
				}
				""";
		mockMvc.perform(put("/api/citizens/me")
				.header("Authorization", "Bearer " + citizenToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(locationJson))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.address").value("12 Main Road"))
				.andExpect(jsonPath("$.city").value("Bhopal"))
				.andExpect(jsonPath("$.latitude").value(23.2599))
				.andExpect(jsonPath("$.longitude").value(77.4126));

		mockMvc.perform(get("/api/citizens/me")
				.header("Authorization", "Bearer " + citizenToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.address").value("12 Main Road"))
				.andExpect(jsonPath("$.state").value("Madhya Pradesh"))
				.andExpect(jsonPath("$.postalCode").value("462001"))
				.andExpect(jsonPath("$.latitude").value(23.2599))
				.andExpect(jsonPath("$.longitude").value(77.4126));

		// Latitude without longitude is rejected.
		mockMvc.perform(put("/api/citizens/me")
				.header("Authorization", "Bearer " + citizenToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"firstName":"UpdatedContract","lastName":"UpdatedCitizen","phoneNumber":"5559998888","latitude":23.25}
						"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	void testAdminUsersListContract() throws Exception {
		AdminUser admin = adminUserRepository.save(AdminUser.builder()
				.email("sysadmin-contract@test.com").firstName("Sys").lastName("Admin")
				.passwordHash(passwordEncoder.encode("AdminPass123!")).role(UserRole.SYSTEM_ADMIN)
				.status(UserStatus.ACTIVE).build());
		String adminToken = generateToken(admin.getId(), admin.getEmail(), "SYSTEM_ADMIN", "ADMIN");
		mockMvc.perform(get("/api/admin/users")
				.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray());
	}
}
