package com.urbanproblems.controller;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.urbanproblems.dto.DatabaseHealthResponseDto;

import lombok.extern.slf4j.Slf4j;

/**
 * Lightweight database health endpoint.
 * Performs a connectivity check using the application's configured DataSource.
 * Sanitizes all output to prevent exposing credentials, queries, or infrastructure details.
 */
@Slf4j
@RestController
@RequestMapping("/api/system/health")
public class DatabaseHealthController {

	private final DataSource dataSource;

	public DatabaseHealthController(ObjectProvider<DataSource> dataSourceProvider) {
		this.dataSource = dataSourceProvider.getIfAvailable();
	}

	@GetMapping("/database")
	public ResponseEntity<DatabaseHealthResponseDto> getDatabaseHealth() {
		if (dataSource == null) {
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
					.body(DatabaseHealthResponseDto.notConfigured("Database connection settings are not configured."));
		}

		try (Connection connection = dataSource.getConnection()) {
			if (connection.isValid(2)) {
				String productName = "Database";
				try {
					productName = connection.getMetaData().getDatabaseProductName();
				} catch (Exception ignored) {
					// Fall back to default label if metadata read fails
				}
				return ResponseEntity.ok(DatabaseHealthResponseDto.connected(productName, "Database connection established"));
			} else {
				log.warn("Database connection returned invalid status during health check");
				return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
						.body(DatabaseHealthResponseDto.disconnected("Database connection unavailable"));
			}
		} catch (SQLException | RuntimeException e) {
			log.warn("Database health check failed: {}", e.getClass().getSimpleName());
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
					.body(DatabaseHealthResponseDto.disconnected("Database connection unavailable"));
		}
	}
}

