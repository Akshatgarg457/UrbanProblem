package com.urbanproblems.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.urbanproblems.domain.entity.Department;
import com.urbanproblems.domain.enums.DepartmentStatus;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
	Optional<Department> findByName(String name);

	Page<Department> findAllByStatus(DepartmentStatus status, Pageable pageable);

	Optional<Department> findByNameAndStatus(String name, DepartmentStatus status);
}
