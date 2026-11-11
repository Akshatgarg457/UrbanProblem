package com.urbanproblems.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;

@Configuration
@EnableConfigurationProperties(FileUploadConfiguration.FileUploadProperties.class)
public class FileUploadConfiguration {

	private final FileUploadProperties fileUploadProperties;

	public FileUploadConfiguration(FileUploadProperties fileUploadProperties) {
		this.fileUploadProperties = fileUploadProperties;
	}

	// Uploaded files are intentionally NOT exposed as static resources. They are served only
	// through FileUploadService after an ownership check.

	@Getter
	@Setter
	@ConfigurationProperties(prefix = "app.file-upload")
	public static class FileUploadProperties {
		private String uploadDir = "/tmp/urban-problem-reporting/uploads/";
		private long maxFileSize = 10485760; // 10MB
		private int maxFiles = 5;
		private String allowedExtensions = "jpg,jpeg,png,pdf,doc,docx";

		public String[] getAllowedExtensionsArray() {
			return allowedExtensions.split(",");
		}
	}
}
