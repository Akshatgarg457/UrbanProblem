package com.urbanproblems.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CitizenProfileUpdateRequestDto(
		@NotBlank(message = "First name is required")
		@Size(min = 1, max = 50, message = "First name must be between 1 and 50 characters")
		String firstName,

		@NotBlank(message = "Last name is required")
		@Size(min = 1, max = 50, message = "Last name must be between 1 and 50 characters")
		String lastName,

		@NotBlank(message = "Phone number is required")
		@Size(min = 10, max = 20, message = "Phone number must be between 10 and 20 characters")
		String phoneNumber,

		@Size(max = 255, message = "Address must be at most 255 characters")
		String address,

		@Size(max = 100, message = "City must be at most 100 characters")
		String city,

		@Size(max = 100, message = "State must be at most 100 characters")
		String state,

		@Size(max = 20, message = "Postal code must be at most 20 characters")
		String postalCode,

		@DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
		@DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
		Double latitude,

		@DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
		@DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
		Double longitude) {
}
