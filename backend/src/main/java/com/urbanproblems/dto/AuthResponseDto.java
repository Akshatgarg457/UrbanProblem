package com.urbanproblems.dto;

import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Builder
public class AuthResponseDto {

	private Long userId;
	private String email;
	private String role;
	private String type;
	private Long departmentId;

	public AuthResponseDto(Long userId, String email, String role, String type, Long departmentId) {
		this.userId = userId;
		this.email = email;
		this.role = role;
		this.type = type;
		this.departmentId = departmentId;
	}
}
