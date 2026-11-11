package com.urbanproblems.dto;

import com.urbanproblems.domain.enums.ComplaintCategory;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class ComplaintRequestDto {

	@NotBlank(message = "Title is required")
	@Size(min = 5, max = 200, message = "Title must be between 5 and 200 characters")
	private String title;

	@NotBlank(message = "Description is required")
	@Size(min = 10, max = 2000, message = "Description must be between 10 and 2000 characters")
	private String description;

	@NotNull(message = "Category is required")
	private ComplaintCategory category;

	@NotBlank(message = "Location is required")
	@Size(min = 5, max = 500, message = "Location must be between 5 and 500 characters")
	private String location;

	@DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
	@DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
	private Double latitude;

	@DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
	@DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
	private Double longitude;
}
