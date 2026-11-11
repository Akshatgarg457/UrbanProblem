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
public class WorkerResponseDto {

	private Long id;
	private String email;
	private String firstName;
	private String lastName;
	private String phoneNumber;
	private Long departmentId;
	private String departmentName;
	private UserStatus status;
	private long activeAssignmentCount;
	private Instant createdAt;
	private Instant updatedAt;

	public String getFullName() {
		return firstName + " " + lastName;
	}
}
