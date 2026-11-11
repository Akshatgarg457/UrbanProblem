package com.urbanproblems.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.urbanproblems.domain.entity.Citizen;
import com.urbanproblems.domain.entity.Complaint;
import com.urbanproblems.domain.entity.Department;
import com.urbanproblems.domain.enums.ComplaintCategory;
import com.urbanproblems.domain.enums.ComplaintStatus;
import com.urbanproblems.domain.enums.DepartmentStatus;
import com.urbanproblems.domain.enums.UserStatus;
import com.urbanproblems.dto.ComplaintRequestDto;
import com.urbanproblems.dto.ComplaintResponseDto;
import com.urbanproblems.repository.CitizenRepository;
import com.urbanproblems.repository.ComplaintRepository;
import com.urbanproblems.repository.ComplaintStatusHistoryRepository;
import com.urbanproblems.repository.DepartmentRepository;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import({ ComplaintService.class, NotificationService.class })
@ActiveProfiles("test")
public class ComplaintServiceTest {

	@Autowired
	private ComplaintService complaintService;

	@Autowired
	private CitizenRepository citizenRepository;

	@Autowired
	private DepartmentRepository departmentRepository;

	@Autowired
	private ComplaintRepository complaintRepository;

	@Autowired
	private ComplaintStatusHistoryRepository statusHistoryRepository;

	private Citizen testCitizen;
	private Department testDepartment;

	@BeforeEach
	void setUp() {
		// Create test department
		testDepartment = Department.builder()
				.name("Water and Sanitation")
				.description("Test department")
				.headEmail("admin@water.gov")
				.status(DepartmentStatus.ACTIVE)
				.build();
		testDepartment = departmentRepository.save(testDepartment);

		// Create test citizen
		testCitizen = Citizen.builder()
				.email("citizen@test.com")
				.firstName("John")
				.lastName("Doe")
				.phoneNumber("5551234567")
				.passwordHash("hashed_password")
				.status(UserStatus.ACTIVE)
				.build();
		testCitizen = citizenRepository.save(testCitizen);
	}

	@Test
	void testCreateComplaint_Success() {
		ComplaintRequestDto requestDto = ComplaintRequestDto.builder()
				.title("Water leak on Main Street")
				.description("Large water leak from broken pipe")
				.category(ComplaintCategory.WATER_SUPPLY)
				.location("123 Main Street")
				.latitude(40.7128)
				.longitude(-74.0060)
				.build();

		ComplaintResponseDto response = complaintService.createComplaint(testCitizen.getId(), requestDto);

		assertNotNull(response.getId());
		assertNotNull(response.getPublicId());
		assertEquals("Water leak on Main Street", response.getTitle());
		assertEquals(ComplaintStatus.SUBMITTED, response.getStatus());
		assertEquals(testCitizen.getId(), response.getCitizenId());
		assertEquals(testDepartment.getId(), response.getDepartmentId());
	}

	@Test
	void testCreateComplaint_InvalidCitizen() {
		ComplaintRequestDto requestDto = ComplaintRequestDto.builder()
				.title("Test complaint")
				.description("Test description")
				.category(ComplaintCategory.WATER_SUPPLY)
				.location("Test location")
				.build();

		assertThrows(com.urbanproblems.exception.ResourceNotFoundException.class, () -> {
			complaintService.createComplaint(999L, requestDto);
		});
	}

	@Test
	void testCreateComplaint_NoMatchingDepartment() {
		departmentRepository.deleteAll();

		ComplaintRequestDto requestDto = ComplaintRequestDto.builder()
				.title("Test complaint")
				.description("Test description")
				.category(ComplaintCategory.WATER_SUPPLY)
				.location("Test location")
				.build();

		assertThrows(IllegalStateException.class, () -> {
			complaintService.createComplaint(testCitizen.getId(), requestDto);
		});
	}

	@Test
	void testGetComplaintByPublicId() {
		// Create a complaint first
		Complaint complaint = Complaint.builder()
				.title("Test complaint")
				.description("Test description")
				.category(ComplaintCategory.ROADS_AND_POTHOLES)
				.location("Test location")
				.citizen(testCitizen)
				.department(testDepartment)
				.status(ComplaintStatus.SUBMITTED)
				.build();
		complaint = complaintRepository.save(complaint);

		// Retrieve by public ID
		ComplaintResponseDto response = complaintService.getComplaintByPublicId(complaint.getPublicId(), systemAdmin());

		assertNotNull(response);
		assertEquals(complaint.getTitle(), response.getTitle());
		assertEquals(complaint.getPublicId(), response.getPublicId());
	}

	@Test
	void testGetComplaintByPublicId_NotFound() {
		assertThrows(com.urbanproblems.exception.ResourceNotFoundException.class, () -> {
			complaintService.getComplaintByPublicId("nonexistent-id", systemAdmin());
		});
	}

	@Test
	void testUpdateComplaintStatus() {
		// Create a complaint
		Complaint complaint = Complaint.builder()
				.title("Test complaint")
				.description("Test description")
				.category(ComplaintCategory.ROADS_AND_POTHOLES)
				.location("Test location")
				.citizen(testCitizen)
				.department(testDepartment)
				.status(ComplaintStatus.SUBMITTED)
				.build();
		complaint = complaintRepository.save(complaint);

		// Update status
		complaintService.updateComplaintStatus(
				complaint.getId(),
				ComplaintStatus.ASSIGNED,
				"Assigned to worker",
				"DEPARTMENT_ADMIN");

		// Verify status was updated
		Complaint updated = complaintRepository.findById(complaint.getId()).orElse(null);
		assertNotNull(updated);
		assertEquals(ComplaintStatus.ASSIGNED, updated.getStatus());

		// Verify history was recorded
		var history = statusHistoryRepository.findByComplaintIdOrderByChangedAtDesc(complaint.getId());
		assertEquals(1, history.size()); // Only the transition: this fixture bypasses createComplaint, so no initial row
		assertEquals(ComplaintStatus.ASSIGNED, history.get(0).getNewStatus());
	}

	@Test
	void testComplaintCanBeAssigned() {
		Complaint complaint = Complaint.builder()
				.title("Test")
				.description("Test")
				.category(ComplaintCategory.ROADS_AND_POTHOLES)
				.location("Test")
				.citizen(testCitizen)
				.department(testDepartment)
				.status(ComplaintStatus.SUBMITTED)
				.build();

		assertTrue(complaint.canBeAssigned());

		complaint.setStatus(ComplaintStatus.ASSIGNED);
		assertFalse(complaint.canBeAssigned());

		complaint.setStatus(ComplaintStatus.RESOLVED);
		assertFalse(complaint.canBeAssigned());

		complaint.setStatus(ComplaintStatus.REJECTED);
		assertFalse(complaint.canBeAssigned());
	}

	private static com.urbanproblems.security.UserPrincipal systemAdmin() {
		return new com.urbanproblems.security.UserPrincipal(1L, "sys@test.org", null, "SYSTEM_ADMIN", null, null, true);
	}
}
