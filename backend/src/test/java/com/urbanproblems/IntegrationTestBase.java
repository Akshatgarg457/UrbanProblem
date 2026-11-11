package com.urbanproblems;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.urbanproblems.domain.entity.Citizen;
import com.urbanproblems.domain.entity.Department;
import com.urbanproblems.domain.entity.Worker;
import com.urbanproblems.domain.enums.ComplaintCategory;
import com.urbanproblems.domain.enums.DepartmentStatus;
import com.urbanproblems.domain.enums.UserStatus;
import com.urbanproblems.repository.AdminUserRepository;
import com.urbanproblems.repository.CitizenRepository;
import com.urbanproblems.repository.ComplaintEvidenceRepository;
import com.urbanproblems.repository.ComplaintRepository;
import com.urbanproblems.repository.NotificationRepository;
import com.urbanproblems.repository.RecurrenceReportRepository;
import com.urbanproblems.repository.DepartmentRepository;
import com.urbanproblems.repository.WorkerRepository;
import com.urbanproblems.security.JwtTokenProvider;

import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class IntegrationTestBase {

	@Autowired
	protected MockMvc mockMvc;

	@Autowired
	protected CitizenRepository citizenRepository;

	@Autowired
	protected DepartmentRepository departmentRepository;

	@Autowired
	protected AdminUserRepository adminUserRepository;

	@Autowired
	protected ComplaintEvidenceRepository complaintEvidenceRepository;

	@Autowired
	protected ComplaintRepository complaintRepository;

	@Autowired
	protected NotificationRepository notificationRepository;

	@Autowired
	protected RecurrenceReportRepository recurrenceReportRepository;

	@Autowired
	protected WorkerRepository workerRepository;

	@Autowired
	protected JwtTokenProvider jwtTokenProvider;

	@Autowired
	protected PasswordEncoder passwordEncoder;

	// Test fixtures
	protected Citizen testCitizen;
	protected Department testDepartment;
	protected Worker testWorker;
	protected String citizenToken;
	protected String workerToken;
	protected String adminToken;

	protected void setupTestData() {
		// Create test department
		testDepartment = seedDepartments().get("Water and Sanitation");

		// Create test citizen
		testCitizen = Citizen.builder()
				.email("citizen@example.com")
				.firstName("John")
				.lastName("Citizen")
				.phoneNumber("5551234567")
				.passwordHash(passwordEncoder.encode("TestPassword123!"))
				.status(UserStatus.ACTIVE)
				.build();
		testCitizen = citizenRepository.save(testCitizen);

		// Create test worker
		testWorker = Worker.builder()
				.email("worker@example.com")
				.firstName("Jane")
				.lastName("Worker")
				.phoneNumber("5559876543")
				.passwordHash(passwordEncoder.encode("WorkerPass123!"))
				.department(testDepartment)
				.status(UserStatus.ACTIVE)
				.build();
		testWorker = workerRepository.save(testWorker);

		// Generate tokens for testing
		citizenToken = generateToken(testCitizen.getId(), testCitizen.getEmail(), "CITIZEN", "CITIZEN");
		workerToken = generateToken(testWorker.getId(), testWorker.getEmail(), "WORKER", "WORKER");
	}

	protected String generateToken(Long userId, String email, String role, String type) {
		com.urbanproblems.security.UserPrincipal principal = new com.urbanproblems.security.UserPrincipal(
				userId, email, "", role, type, null, true);
		return jwtTokenProvider.generateTokenFromUserPrincipal(principal);
	}

	/** Creates every department that a complaint category routes to, if missing. Returns them by name. */
	protected java.util.Map<String, Department> seedDepartments() {
		java.util.Map<String, Department> byName = new java.util.HashMap<>();
		for (ComplaintCategory category : ComplaintCategory.values()) {
			String name = category.getDepartmentName();
			byName.computeIfAbsent(name, n -> departmentRepository.findByName(n).orElseGet(() ->
					departmentRepository.save(Department.builder()
							.name(n)
							.description("Test department")
							.headEmail("head@test.org")
							.status(DepartmentStatus.ACTIVE)
							.build())));
		}
		return byName;
	}

	protected void cleanupTestData() {
		notificationRepository.deleteAll();
		complaintEvidenceRepository.deleteAll();
		recurrenceReportRepository.deleteAll();
		complaintRepository.deleteAll();
		citizenRepository.deleteAll();
		workerRepository.deleteAll();
		adminUserRepository.deleteAll();
		departmentRepository.deleteAll();
	}
}
