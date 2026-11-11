package com.urbanproblems.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import jakarta.servlet.http.Cookie;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.urbanproblems.IntegrationTestBase;
import com.urbanproblems.domain.entity.AdminUser;
import com.urbanproblems.dto.AuthRequestDto;
import com.urbanproblems.dto.CitizenRegistrationDto;
import com.urbanproblems.domain.enums.UserRole;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

public class AuthControllerIntegrationTest extends IntegrationTestBase {

	private ObjectMapper objectMapper = new ObjectMapper();

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		cleanupTestData();
		setupTestData();
	}

	@Test
	void testRegisterCitizen_Success() throws Exception {
		CitizenRegistrationDto registrationDto = CitizenRegistrationDto.builder()
				.email("newcitizen@example.com")
				.firstName("New")
				.lastName("Citizen")
				.phoneNumber("5555555555")
				.password("SecurePassword123!")
				.build();

		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(registrationDto)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.userId").isNumber())
				.andExpect(jsonPath("$.accessToken").doesNotExist())
				.andExpect(jsonPath("$.email").value("newcitizen@example.com"))
				.andExpect(jsonPath("$.role").value("CITIZEN"));
	}

	@Test
	void testRegisterCitizen_DuplicateEmail() throws Exception {
		CitizenRegistrationDto registrationDto = CitizenRegistrationDto.builder()
				.email("citizen@example.com") // Already exists
				.firstName("Duplicate")
				.lastName("Citizen")
				.phoneNumber("5555555555")
				.password("SecurePassword123!")
				.build();

		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(registrationDto)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void testRegisterCitizen_InvalidEmail() throws Exception {
		CitizenRegistrationDto registrationDto = CitizenRegistrationDto.builder()
				.email("invalid-email") // Invalid email format
				.firstName("Invalid")
				.lastName("Citizen")
				.phoneNumber("5555555555")
				.password("SecurePassword123!")
				.build();

		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(registrationDto)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void testRegisterCitizen_ShortPassword() throws Exception {
		CitizenRegistrationDto registrationDto = CitizenRegistrationDto.builder()
				.email("newcitizen@example.com")
				.firstName("New")
				.lastName("Citizen")
				.phoneNumber("5555555555")
				.password("short") // Too short
				.build();

		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(registrationDto)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void testLogin_Success() throws Exception {
		AuthRequestDto loginDto = AuthRequestDto.builder()
				.email("citizen@example.com")
				.password("TestPassword123!")
				.build();

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginDto)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(testCitizen.getId()))
				.andExpect(jsonPath("$.accessToken").doesNotExist())
				.andExpect(jsonPath("$.email").value("citizen@example.com"))
				.andExpect(jsonPath("$.role").value("CITIZEN"));
	}

	@Test
	void testLogin_InvalidCredentials() throws Exception {
		AuthRequestDto loginDto = AuthRequestDto.builder()
				.email("citizen@example.com")
				.password("WrongPassword")
				.build();

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginDto)))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void testLogin_UserNotFound() throws Exception {
		AuthRequestDto loginDto = AuthRequestDto.builder()
				.email("nonexistent@example.com")
				.password("AnyPassword123!")
				.build();

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginDto)))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void testLoginRejectsCitizenFromAdminWorkspace() throws Exception {
		AuthRequestDto loginDto = AuthRequestDto.builder()
				.email("citizen@example.com")
				.password("TestPassword123!")
				.workspace("ADMIN")
				.build();

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginDto)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));
	}

	@Test
	void testLoginRejectsCitizenFromWorkerWorkspace() throws Exception {
		AuthRequestDto loginDto = AuthRequestDto.builder()
				.email("citizen@example.com")
				.password("TestPassword123!")
				.workspace("WORKER")
				.build();

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginDto)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));
	}

	@Test
	void testLoginRejectsWorkerFromAdminWorkspace() throws Exception {
		AuthRequestDto loginDto = AuthRequestDto.builder()
				.email("worker@example.com")
				.password("WorkerPass123!")
				.workspace("ADMIN")
				.build();

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginDto)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error").value("INVALID_CREDENTIALS"));
	}

	@Test
	void testLoginWorkerIntoWorkerWorkspace() throws Exception {
		AuthRequestDto loginDto = AuthRequestDto.builder()
				.email("worker@example.com")
				.password("WorkerPass123!")
				.workspace("WORKER")
				.build();

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginDto)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.role").value("WORKER"));
	}

	@Test
	void testLoginAdminIntoAdminWorkspace() throws Exception {
		adminUserRepository.save(AdminUser.builder()
				.email("admin@example.com")
				.firstName("System")
				.lastName("Admin")
				.passwordHash(passwordEncoder.encode("AdminPass123!"))
				.role(UserRole.SYSTEM_ADMIN)
				.build());

		AuthRequestDto loginDto = AuthRequestDto.builder()
				.email("admin@example.com")
				.password("AdminPass123!")
				.workspace("ADMIN")
				.build();

		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginDto)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.role").value("SYSTEM_ADMIN"));
	}

	@Test
	void testLogout_Success() throws Exception {
		AuthRequestDto loginDto = AuthRequestDto.builder()
				.email("citizen@example.com")
				.password("TestPassword123!")
				.build();
		Cookie session = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginDto)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getCookie("CGSESSION");

		mockMvc.perform(post("/api/auth/logout").cookie(session))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/auth/me").cookie(session))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void testLoginCreatesReusableServerSideSession() throws Exception {
		AuthRequestDto loginDto = AuthRequestDto.builder()
				.email("citizen@example.com")
				.password("TestPassword123!")
				.build();

		var loginResult = mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(loginDto)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.role").value("CITIZEN"))
				.andReturn();

		Cookie session = loginResult.getResponse().getCookie("CGSESSION");
		org.junit.jupiter.api.Assertions.assertNotNull(session);
		org.junit.jupiter.api.Assertions.assertTrue(session.isHttpOnly());
		Integer storedSessions = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM SPRING_SESSION", Integer.class);
		org.junit.jupiter.api.Assertions.assertTrue(storedSessions != null && storedSessions > 0,
				"Login should persist the authenticated session in JDBC storage.");

		mockMvc.perform(get("/api/auth/me").cookie(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value("citizen@example.com"));
		mockMvc.perform(get("/api/complaints/my-complaints")
				.cookie(session))
				.andExpect(status().isOk());
	}
}
