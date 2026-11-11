package com.urbanproblems.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.urbanproblems.config.FileUploadConfiguration.FileUploadProperties;
import com.urbanproblems.domain.entity.Assignment;
import com.urbanproblems.domain.entity.Complaint;
import com.urbanproblems.domain.entity.ComplaintEvidence;
import com.urbanproblems.domain.enums.AssignmentStatus;
import com.urbanproblems.exception.ResourceNotFoundException;
import com.urbanproblems.repository.AssignmentRepository;
import com.urbanproblems.repository.ComplaintEvidenceRepository;
import com.urbanproblems.security.AccessRules;
import com.urbanproblems.security.UserPrincipal;

/**
 * Evidence storage. Every operation first checks that the caller may see the complaint.
 * Files are validated by content (magic bytes), stored under random names, and served only through
 * this service. No path is ever built from client input.
 */
@Service
@Transactional
public class FileUploadService {

	/** Content signatures for each extension the system can accept. Extensions not listed are rejected. */
	private static final Map<String, List<byte[]>> SIGNATURES = Map.of(
			"pdf", List.of(bytes('%', 'P', 'D', 'F')),
			"png", List.of(bytes(0x89, 'P', 'N', 'G')),
			"jpg", List.of(bytes(0xFF, 0xD8, 0xFF)),
			"jpeg", List.of(bytes(0xFF, 0xD8, 0xFF)),
			"doc", List.of(bytes(0xD0, 0xCF, 0x11, 0xE0)),
			"docx", List.of(bytes('P', 'K', 0x03, 0x04)));

	private static final Map<String, String> CONTENT_TYPES = Map.of(
			"pdf", "application/pdf",
			"png", "image/png",
			"jpg", "image/jpeg",
			"jpeg", "image/jpeg",
			"doc", "application/msword",
			"docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

	private static final int MAX_NAME_LENGTH = 120;

	/** Content of a stored evidence file ready to be returned to an authorized caller. */
	public record DownloadedEvidence(String fileName, String contentType, byte[] content) {
	}

	private final FileUploadProperties properties;
	private final ComplaintService complaintService;
	private final ComplaintEvidenceRepository evidenceRepository;
	private final AssignmentRepository assignmentRepository;

	public FileUploadService(FileUploadProperties properties, ComplaintService complaintService,
			ComplaintEvidenceRepository evidenceRepository, AssignmentRepository assignmentRepository) {
		this.properties = properties;
		this.complaintService = complaintService;
		this.evidenceRepository = evidenceRepository;
		this.assignmentRepository = assignmentRepository;
	}

	public ComplaintEvidence uploadEvidence(Long complaintId, MultipartFile file, UserPrincipal user) {
		if (!AccessRules.CITIZEN.equals(user.getRole()) && !AccessRules.WORKER.equals(user.getRole())) {
			throw new AccessDeniedException("Access denied");
		}
		Complaint complaint = complaintService.findComplaint(complaintId);
		AccessRules.requireComplaintVisible(user, complaint, assignmentRepository);

		if (AccessRules.WORKER.equals(user.getRole())) {
			Assignment assignment = assignmentRepository.findByComplaintId(complaintId)
					.orElseThrow(() -> new AccessDeniedException("Access denied"));
			boolean active = assignment.getStatus() == AssignmentStatus.ASSIGNED
					|| assignment.getStatus() == AssignmentStatus.IN_PROGRESS;
			if (!assignment.getAssignedWorker().getId().equals(user.getId()) || !active) {
				throw new IllegalStateException("Evidence can only be uploaded while your task is assigned or in progress.");
			}
		}

		byte[] data = readBytes(file);
		String extension = validateFile(file, data);

		if (evidenceRepository.countByComplaintId(complaintId) >= properties.getMaxFiles()) {
			throw new IllegalStateException("Maximum number of files (" + properties.getMaxFiles() + ") reached");
		}

		Path baseDir = baseDirectory();
		Path target = resolveInside(baseDir, UUID.randomUUID() + "." + extension);
		try {
			Files.createDirectories(baseDir);
			Files.write(target, data, StandardOpenOption.CREATE_NEW);
		} catch (IOException e) {
			throw new IllegalStateException("Could not store the file.", e);
		}

		try {
			return evidenceRepository.save(ComplaintEvidence.builder()
					.complaint(complaint)
					.fileName(sanitizeFileName(file.getOriginalFilename()))
					.fileType(CONTENT_TYPES.get(extension))
					.fileSize((long) data.length)
					.filePath(target.getFileName().toString())
					.uploadedBy(user.getRole())
					.uploadedById(user.getId())
					.build());
		} catch (RuntimeException e) {
			deleteQuietly(target);
			throw e;
		}
	}

	@org.springframework.transaction.annotation.Transactional(readOnly = true)
	public List<ComplaintEvidence> getEvidenceForComplaint(Long complaintId, UserPrincipal user) {
		Complaint complaint = complaintService.findComplaint(complaintId);
		AccessRules.requireComplaintVisible(user, complaint, assignmentRepository);
		return evidenceRepository.findByComplaintId(complaintId);
	}

	@org.springframework.transaction.annotation.Transactional(readOnly = true)
	public DownloadedEvidence downloadEvidence(Long complaintId, Long evidenceId, UserPrincipal user) {
		ComplaintEvidence evidence = findEvidenceOnComplaint(complaintId, evidenceId);
		AccessRules.requireComplaintVisible(user, evidence.getComplaint(), assignmentRepository);

		Path path = resolveInside(baseDirectory(), evidence.getFilePath());
		try {
			return new DownloadedEvidence(evidence.getFileName(), evidence.getFileType(), Files.readAllBytes(path));
		} catch (IOException e) {
			throw new ResourceNotFoundException("Evidence file is unavailable.");
		}
	}

	/** Only the uploader may delete evidence. The complaint in the URL must match the evidence. */
	public void deleteEvidence(Long complaintId, Long evidenceId, UserPrincipal user) {
		ComplaintEvidence evidence = findEvidenceOnComplaint(complaintId, evidenceId);
		boolean isUploader = user.getRole().equals(evidence.getUploadedBy())
				&& user.getId().equals(evidence.getUploadedById());
		if (!isUploader) {
			throw new AccessDeniedException("Access denied");
		}
		String storedName = evidence.getFilePath();
		evidenceRepository.delete(evidence);
		deleteQuietly(resolveInside(baseDirectory(), storedName));
	}

	private ComplaintEvidence findEvidenceOnComplaint(Long complaintId, Long evidenceId) {
		ComplaintEvidence evidence = evidenceRepository.findById(evidenceId)
				.orElseThrow(() -> new ResourceNotFoundException("Evidence not found"));
		if (!evidence.getComplaint().getId().equals(complaintId)) {
			throw new ResourceNotFoundException("Evidence not found");
		}
		return evidence;
	}

	private byte[] readBytes(MultipartFile file) {
		try {
			return file.getBytes();
		} catch (IOException e) {
			throw new IllegalArgumentException("File could not be read.");
		}
	}

	/** Checks size, name, extension allow-list, and content signature. Returns the normalized extension. */
	private String validateFile(MultipartFile file, byte[] data) {
		if (data.length == 0) {
			throw new IllegalArgumentException("File cannot be empty");
		}
		if (data.length > properties.getMaxFileSize()) {
			throw new IllegalArgumentException("File size exceeds maximum allowed: " + properties.getMaxFileSize() + " bytes");
		}

		String name = sanitizeFileName(file.getOriginalFilename());
		int dot = name.lastIndexOf('.');
		if (dot <= 0 || dot == name.length() - 1) {
			throw new IllegalArgumentException("Invalid file name");
		}
		String extension = name.substring(dot + 1).toLowerCase(Locale.ROOT);

		List<String> allowed = Arrays.stream(properties.getAllowedExtensionsArray())
				.map(s -> s.trim().toLowerCase(Locale.ROOT))
				.toList();
		List<byte[]> signatures = SIGNATURES.get(extension);
		if (signatures == null || !allowed.contains(extension)) {
			throw new IllegalArgumentException("File type not allowed. Allowed types: " + String.join(", ", allowed));
		}

		boolean matches = signatures.stream().anyMatch(sig -> startsWith(data, sig));
		if (!matches) {
			throw new IllegalArgumentException("File content does not match its extension.");
		}
		return extension;
	}

	/** Strips directory parts and any character outside a conservative set. Display use only. */
	static String sanitizeFileName(String raw) {
		if (raw == null) {
			return "";
		}
		String name = raw.replace('\\', '/');
		name = name.substring(name.lastIndexOf('/') + 1);
		name = name.replaceAll("[^A-Za-z0-9._ -]", "_").strip();
		if (name.length() > MAX_NAME_LENGTH) {
			name = name.substring(name.length() - MAX_NAME_LENGTH);
		}
		return name;
	}

	private Path baseDirectory() {
		return Path.of(properties.getUploadDir()).toAbsolutePath().normalize();
	}

	/** Resolves a stored file name and refuses anything that escapes the upload directory. */
	private static Path resolveInside(Path baseDir, String storedName) {
		Path resolved = baseDir.resolve(storedName).normalize();
		if (!resolved.startsWith(baseDir)) {
			throw new IllegalArgumentException("Invalid stored file name");
		}
		return resolved;
	}

	private static void deleteQuietly(Path path) {
		try {
			Files.deleteIfExists(path);
		} catch (IOException ignored) {
			// The database record is already consistent; an orphaned file is logged by ops tooling.
		}
	}

	private static boolean startsWith(byte[] data, byte[] prefix) {
		return data.length >= prefix.length && Arrays.equals(Arrays.copyOf(data, prefix.length), prefix);
	}

	private static byte[] bytes(int... values) {
		byte[] out = new byte[values.length];
		for (int i = 0; i < values.length; i++) {
			out[i] = (byte) values[i];
		}
		return out;
	}
}
