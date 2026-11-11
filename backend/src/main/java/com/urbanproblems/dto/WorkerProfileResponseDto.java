package com.urbanproblems.dto;

import java.time.Instant;

import com.urbanproblems.domain.enums.UserStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkerProfileResponseDto {

	private Long id;
	private String email;
	private String firstName;
	private String lastName;
	private String phoneNumber;
	private String address;
	private String city;
	private String state;
	private String postalCode;
	private Double latitude;
	private Double longitude;
	private Long departmentId;
	private String departmentName;
	private UserStatus status;
	private Instant createdAt;
	private Instant updatedAt;

	public String getFullName() {
		return firstName + " " + lastName;
	}
}
