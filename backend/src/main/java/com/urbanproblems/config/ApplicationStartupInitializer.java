package com.urbanproblems.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.urbanproblems.domain.entity.Department;
import com.urbanproblems.domain.enums.ComplaintCategory;
import com.urbanproblems.domain.enums.DepartmentStatus;
import com.urbanproblems.repository.DepartmentRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Profile("!test")
@Order(Ordered.LOWEST_PRECEDENCE - 1)
public class ApplicationStartupInitializer implements ApplicationRunner {

	private final DepartmentRepository departmentRepository;

	public ApplicationStartupInitializer(DepartmentRepository departmentRepository) {
		this.departmentRepository = departmentRepository;
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		initializeDefaultDepartments();
	}

	private void initializeDefaultDepartments() {
		log.info("Initializing default departments...");
		for (ComplaintCategory category : ComplaintCategory.values()) {
			String deptName = category.getDepartmentName();
			if (departmentRepository.findByName(deptName).isEmpty()) {
				Department department = Department.builder()
						.name(deptName)
						.description("Handles " + category.getCode() + " related complaints")
						.headEmail("head-" + deptName.toLowerCase().replace(" ", "-") + "@municipal.gov")
						.status(DepartmentStatus.ACTIVE)
						.build();
				departmentRepository.save(department);
				log.info("Created department: {}", deptName);
			}
		}
		log.info("Department initialization complete");
	}
}
