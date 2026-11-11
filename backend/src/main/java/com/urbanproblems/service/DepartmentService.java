package com.urbanproblems.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.urbanproblems.domain.entity.Department;
import com.urbanproblems.domain.enums.DepartmentStatus;
import com.urbanproblems.dto.DepartmentRequestDto;
import com.urbanproblems.dto.DepartmentResponseDto;
import com.urbanproblems.repository.ComplaintRepository;
import com.urbanproblems.repository.DepartmentRepository;
import com.urbanproblems.repository.WorkerRepository;

@Service
@Transactional
public class DepartmentService {

	private static final java.util.List<com.urbanproblems.domain.enums.ComplaintStatus> OPEN_STATUSES = java.util.List.of(
			com.urbanproblems.domain.enums.ComplaintStatus.SUBMITTED,
			com.urbanproblems.domain.enums.ComplaintStatus.ASSIGNED,
			com.urbanproblems.domain.enums.ComplaintStatus.IN_PROGRESS,
			com.urbanproblems.domain.enums.ComplaintStatus.PENDING_VERIFICATION,
			com.urbanproblems.domain.enums.ComplaintStatus.APPROVED);

	private final DepartmentRepository departmentRepository;
	private final WorkerRepository workerRepository;
	private final ComplaintRepository complaintRepository;

	public DepartmentService(DepartmentRepository departmentRepository,
			WorkerRepository workerRepository,
			ComplaintRepository complaintRepository) {
		this.departmentRepository = departmentRepository;
		this.workerRepository = workerRepository;
		this.complaintRepository = complaintRepository;
	}

	public DepartmentResponseDto createDepartment(DepartmentRequestDto requestDto) {
		if (departmentRepository.findByName(requestDto.getName()).isPresent()) {
			throw new IllegalArgumentException("Department already exists with this name");
		}

		Department department = Department.builder()
				.name(requestDto.getName())
				.description(requestDto.getDescription())
				.headEmail(requestDto.getHeadEmail())
				.status(DepartmentStatus.ACTIVE)
				.build();

		department = departmentRepository.save(department);
		return mapToResponseDto(department);
	}

	public DepartmentResponseDto getDepartment(Long id, com.urbanproblems.security.UserPrincipal user) {
		Department department = departmentRepository.findById(id)
				.orElseThrow(() -> new com.urbanproblems.exception.ResourceNotFoundException("Department not found"));
		com.urbanproblems.security.AccessRules.requireDepartmentScope(user, department);
		return mapToResponseDto(department);
	}

	public Page<DepartmentResponseDto> getAllDepartments(Pageable pageable) {
		return departmentRepository.findAll(pageable)
				.map(this::mapToResponseDto);
	}

	public Page<DepartmentResponseDto> getActiveDepartments(Pageable pageable) {
		return departmentRepository.findAllByStatus(DepartmentStatus.ACTIVE, pageable)
				.map(this::mapToResponseDto);
	}

	/** Public view for the worker-registration form; exposes no internal contact data. */
	public java.util.List<com.urbanproblems.dto.PublicDepartmentDto> getPublicDepartments() {
		return departmentRepository.findAll()
				.stream()
				.filter(d -> d.getStatus() == DepartmentStatus.ACTIVE)
				.sorted(java.util.Comparator.comparing(Department::getName))
				.map(d -> new com.urbanproblems.dto.PublicDepartmentDto(d.getId(), d.getName(), d.getDescription()))
				.toList();
	}

	public DepartmentResponseDto updateDepartment(Long id, DepartmentRequestDto requestDto) {
		Department department = departmentRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Department not found"));

		department.setName(requestDto.getName());
		department.setDescription(requestDto.getDescription());
		department.setHeadEmail(requestDto.getHeadEmail());
		department = departmentRepository.save(department);

		return mapToResponseDto(department);
	}

	public DepartmentResponseDto changeDepartmentStatus(Long id, DepartmentStatus status) {
		Department department = departmentRepository.findById(id)
				.orElseThrow(() -> new IllegalArgumentException("Department not found"));

		// Validate that department can be deactivated
		if (status == DepartmentStatus.INACTIVE) {
			// Check for active assignments or complaints
			long activeComplaints = complaintRepository.countByDepartmentIdAndStatusIn(id, OPEN_STATUSES);
			if (activeComplaints > 0) {
				throw new IllegalStateException(
						"Cannot deactivate department with active complaints. Transfer or resolve them first.");
			}
		}

		department.setStatus(status);
		department = departmentRepository.save(department);
		return mapToResponseDto(department);
	}

	private DepartmentResponseDto mapToResponseDto(Department department) {
		long workerCount = workerRepository.countByDepartmentId(department.getId());
		long complaintCount = complaintRepository.countByDepartmentId(department.getId());

		return DepartmentResponseDto.builder()
				.id(department.getId())
				.name(department.getName())
				.description(department.getDescription())
				.headEmail(department.getHeadEmail())
				.status(department.getStatus())
				.workerCount(workerCount)
				.complaintCount(complaintCount)
				.createdAt(department.getCreatedAt())
				.updatedAt(department.getUpdatedAt())
				.build();
	}
}
