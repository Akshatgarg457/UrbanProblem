package com.urbanproblems.domain.enums;

public enum ComplaintCategory {
	ROADS_AND_POTHOLES("ROADS_AND_POTHOLES", "Roads and Infrastructure"),
	WATER_SUPPLY("WATER_SUPPLY", "Water and Sanitation"),
	ELECTRICITY_AND_STREETLIGHTS("ELECTRICITY_AND_STREETLIGHTS", "Electricity"),
	DRAINAGE_AND_SEWAGE("DRAINAGE_AND_SEWAGE", "Water and Sanitation"),
	WASTE_MANAGEMENT("WASTE_MANAGEMENT", "Waste Management"),
	HEALTH_AND_ENVIRONMENT("HEALTH_AND_ENVIRONMENT", "Health and Environment"),
	PUBLIC_TRANSPORT("PUBLIC_TRANSPORT", "Public Transport"),
	PARKS_AND_RECREATION("PARKS_AND_RECREATION", "Parks and Recreation"),
	SAFETY_AND_SECURITY("SAFETY_AND_SECURITY", "Safety and Security");

	private final String code;
	private final String departmentName;

	ComplaintCategory(String code, String departmentName) {
		this.code = code;
		this.departmentName = departmentName;
	}

	public String getCode() {
		return code;
	}

	public String getDepartmentName() {
		return departmentName;
	}
}
