package com.urbanproblems.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.urbanproblems.domain.entity.Citizen;

@Repository
public interface CitizenRepository extends JpaRepository<Citizen, Long> {
	Optional<Citizen> findByEmail(String email);

	/** Candidate accounts for a username (email local part) lookup. */
	List<Citizen> findByEmailStartingWithIgnoreCase(String prefix);

	boolean existsByEmail(String email);

	Page<Citizen> findAll(Pageable pageable);
}
