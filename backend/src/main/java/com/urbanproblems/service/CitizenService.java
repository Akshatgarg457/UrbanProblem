package com.urbanproblems.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.urbanproblems.domain.entity.Citizen;
import com.urbanproblems.dto.CitizenProfileUpdateRequestDto;
import com.urbanproblems.dto.CitizenResponseDto;
import com.urbanproblems.exception.ResourceNotFoundException;
import com.urbanproblems.repository.CitizenRepository;

@Service
@Transactional
public class CitizenService {

	private final CitizenRepository citizenRepository;

	public CitizenService(CitizenRepository citizenRepository) {
		this.citizenRepository = citizenRepository;
	}

	@Transactional(readOnly = true)
	public CitizenResponseDto getProfile(Long citizenId) {
		Citizen citizen = citizenRepository.findById(citizenId)
				.orElseThrow(() -> new ResourceNotFoundException("Citizen profile not found"));
		return mapToDto(citizen);
	}

	public CitizenResponseDto updateProfile(Long citizenId, CitizenProfileUpdateRequestDto dto) {
		Citizen citizen = citizenRepository.findById(citizenId)
				.orElseThrow(() -> new ResourceNotFoundException("Citizen profile not found"));

		citizen.setFirstName(dto.firstName().trim());
		citizen.setLastName(dto.lastName().trim());
		citizen.setPhoneNumber(dto.phoneNumber().trim());
		applySavedLocation(citizen, dto.address(), dto.city(), dto.state(), dto.postalCode(),
				dto.latitude(), dto.longitude());

		Citizen saved = citizenRepository.save(citizen);
		return mapToDto(saved);
	}

	private static void applySavedLocation(Citizen citizen, String address, String city, String state,
			String postalCode, Double latitude, Double longitude) {
		if ((latitude == null) != (longitude == null)) {
			throw new IllegalArgumentException("Latitude and longitude must be provided together.");
		}
		citizen.setAddress(blankToNull(address));
		citizen.setCity(blankToNull(city));
		citizen.setState(blankToNull(state));
		citizen.setPostalCode(blankToNull(postalCode));
		citizen.setLatitude(latitude);
		citizen.setLongitude(longitude);
	}

	private static String blankToNull(String value) {
		return value == null || value.isBlank() ? null : value.trim();
	}

	public static CitizenResponseDto mapToDto(Citizen citizen) {
		return CitizenResponseDto.builder()
				.id(citizen.getId())
				.email(citizen.getEmail())
				.firstName(citizen.getFirstName())
				.lastName(citizen.getLastName())
				.phoneNumber(citizen.getPhoneNumber())
				.address(citizen.getAddress())
				.city(citizen.getCity())
				.state(citizen.getState())
				.postalCode(citizen.getPostalCode())
				.latitude(citizen.getLatitude())
				.longitude(citizen.getLongitude())
				.status(citizen.getStatus())
				.createdAt(citizen.getCreatedAt())
				.updatedAt(citizen.getUpdatedAt())
				.build();
	}
}

