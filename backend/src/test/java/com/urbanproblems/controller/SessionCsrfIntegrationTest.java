package com.urbanproblems.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.Cookie;

@SpringBootTest(properties = "app.security.csrf-enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SessionCsrfIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void unsafeRequestsRequireCsrfCookieAndMatchingHeader() throws Exception {
		String registration = """
				{"email":"csrf-user@example.org","firstName":"CSRF","lastName":"User",
				"phoneNumber":"5551234567","password":"SecurePassword123!",
				"securityQuestion":"What city were you born in?","securityAnswer":"Testville"}
				""";

		mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(registration))
				.andExpect(status().isForbidden());

		MvcResult csrfResult = mockMvc.perform(get("/api/auth/csrf"))
				.andExpect(status().isOk())
				.andExpect(cookie().exists("XSRF-TOKEN"))
				.andReturn();
		Cookie csrfCookie = csrfResult.getResponse().getCookie("XSRF-TOKEN");
		JsonNode csrfResponse = objectMapper.readTree(csrfResult.getResponse().getContentAsString());
		String csrfToken = csrfResponse.get("token").asText();

		mockMvc.perform(post("/api/auth/register")
				.cookie(csrfCookie)
				.header("X-XSRF-TOKEN", csrfToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(registration))
				.andExpect(status().isCreated());
	}
}
