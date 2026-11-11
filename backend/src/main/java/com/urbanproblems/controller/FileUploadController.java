package com.urbanproblems.controller;

import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.urbanproblems.domain.entity.ComplaintEvidence;
import com.urbanproblems.dto.ComplaintEvidenceResponseDto;
import com.urbanproblems.security.AccessRules;
import com.urbanproblems.service.FileUploadService;
import com.urbanproblems.service.FileUploadService.DownloadedEvidence;

@RestController
@RequestMapping("/api/complaints/{complaintId}/evidence")
public class FileUploadController {

	private final FileUploadService fileUploadService;

	public FileUploadController(FileUploadService fileUploadService) {
		this.fileUploadService = fileUploadService;
	}

	@PostMapping("/upload")
	@PreAuthorize("hasAnyRole('CITIZEN', 'WORKER')")
	public ResponseEntity<ComplaintEvidenceResponseDto> uploadEvidence(
			@PathVariable Long complaintId,
			@RequestParam("file") MultipartFile file) {
		ComplaintEvidence evidence = fileUploadService.uploadEvidence(complaintId, file, AccessRules.currentPrincipal());
		return ResponseEntity.status(201).body(mapToDto(evidence));
	}

	@GetMapping
	@PreAuthorize("hasAnyRole('CITIZEN', 'WORKER', 'DEPARTMENT_ADMIN', 'SYSTEM_ADMIN')")
	public ResponseEntity<List<ComplaintEvidenceResponseDto>> getEvidence(@PathVariable Long complaintId) {
		return ResponseEntity.ok(fileUploadService.getEvidenceForComplaint(complaintId, AccessRules.currentPrincipal())
				.stream().map(this::mapToDto).toList());
	}

	@GetMapping("/{evidenceId}/download")
	@PreAuthorize("hasAnyRole('CITIZEN', 'WORKER', 'DEPARTMENT_ADMIN', 'SYSTEM_ADMIN')")
	public ResponseEntity<byte[]> downloadEvidence(@PathVariable Long complaintId, @PathVariable Long evidenceId) {
		DownloadedEvidence file = fileUploadService.downloadEvidence(
				complaintId, evidenceId, AccessRules.currentPrincipal());
		MediaType type = file.contentType() == null
				? MediaType.APPLICATION_OCTET_STREAM
				: MediaType.parseMediaType(file.contentType());
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION,
						ContentDisposition.attachment().filename(file.fileName()).build().toString())
				.header("X-Content-Type-Options", "nosniff")
				.contentType(type)
				.body(file.content());
	}

	/** Only the uploader may delete evidence. */
	@DeleteMapping("/{evidenceId}")
	@PreAuthorize("hasAnyRole('CITIZEN', 'WORKER')")
	public ResponseEntity<Void> deleteEvidence(@PathVariable Long complaintId, @PathVariable Long evidenceId) {
		fileUploadService.deleteEvidence(complaintId, evidenceId, AccessRules.currentPrincipal());
		return ResponseEntity.noContent().build();
	}

	private ComplaintEvidenceResponseDto mapToDto(ComplaintEvidence evidence) {
		return ComplaintEvidenceResponseDto.builder()
				.id(evidence.getId())
				.complaintId(evidence.getComplaint().getId())
				.fileName(evidence.getFileName())
				.fileType(evidence.getFileType())
				.fileSize(evidence.getFileSize())
				.uploadedBy(evidence.getUploadedBy())
				.createdAt(evidence.getCreatedAt())
				.downloadUrl("/api/complaints/" + evidence.getComplaint().getId()
						+ "/evidence/" + evidence.getId() + "/download")
				.build();
	}
}
