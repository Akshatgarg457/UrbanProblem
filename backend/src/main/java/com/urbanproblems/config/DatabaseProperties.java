package com.urbanproblems.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties(prefix = "app.datasource")
public record DatabaseProperties(
		@NotBlank(message = "Set DB_URL to the MySQL JDBC URL.")
		String url,
		@NotBlank(message = "Set DB_USERNAME to the MySQL username.")
		String username,
		@NotBlank(message = "Set DB_PASSWORD to the MySQL password.")
		String password) {
}
