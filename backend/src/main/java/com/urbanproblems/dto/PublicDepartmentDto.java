package com.urbanproblems.dto;

/** Minimal department view exposed to the public worker-registration form. */
public record PublicDepartmentDto(Long id, String name, String description) {
}
