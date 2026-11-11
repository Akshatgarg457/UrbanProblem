package com.urbanproblems.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.urbanproblems.domain.entity.Citizen;
import com.urbanproblems.domain.enums.UserStatus;
import com.urbanproblems.domain.entity.AdminUser;
import com.urbanproblems.domain.entity.Worker;
import com.urbanproblems.dto.AuthenticatedUserDto;
import com.urbanproblems.dto.AuthRequestDto;
import com.urbanproblems.dto.AuthResponseDto;
import com.urbanproblems.dto.CitizenRegistrationDto;
import com.urbanproblems.repository.AdminUserRepository;
import com.urbanproblems.repository.CitizenRepository;
import com.urbanproblems.repository.WorkerRepository;
import com.urbanproblems.security.UserPrincipal;

@Service
@Transactional
public class AuthService {

	private final AuthenticationManager authenticationManager;
	private final CitizenRepository citizenRepository;
	private final WorkerRepository workerRepository;
	private final AdminUserRepository adminUserRepository;
	private final PasswordEncoder passwordEncoder;

	public AuthService(AuthenticationManager authenticationManager,
			CitizenRepository citizenRepository, WorkerRepository workerRepository,
			AdminUserRepository adminUserRepository, PasswordEncoder passwordEncoder) {
		this.authenticationManager = authenticationManager;
		this.citizenRepository = citizenRepository;
		this.workerRepository = workerRepository;
		this.adminUserRepository = adminUserRepository;
		this.passwordEncoder = passwordEncoder;
	}

	/** Public self-registration creates citizens only. Workers and admins are created by privileged roles. */
	public AuthResponseDto registerCitizen(CitizenRegistrationDto dto) {
		String email = normalize(dto.getEmail());
		if (citizenRepository.existsByEmail(email) || adminUserRepository.existsByEmail(email)) {
			throw new IllegalArgumentException("Email already registered");
		}

		Citizen citizen = citizenRepository.save(Citizen.builder()
				.email(email)
				.firstName(dto.getFirstName())
				.lastName(dto.getLastName())
				.phoneNumber(dto.getPhoneNumber())
				.passwordHash(passwordEncoder.encode(dto.getPassword()))
				.securityQuestion(dto.getSecurityQuestion().trim())
				.securityAnswerHash(passwordEncoder.encode(normalizeAnswer(dto.getSecurityAnswer())))
				.status(UserStatus.ACTIVE)
				.build());

		return new AuthResponseDto(citizen.getId(), citizen.getEmail(), "CITIZEN", "CITIZEN", null);
	}

	public UserPrincipal login(AuthRequestDto loginDto) {
		String workspace = normalizeWorkspace(loginDto.getWorkspace());
		String identifier = normalize(loginDto.getEmail());
		String email = identifier.contains("@") ? identifier : resolveUsername(identifier, workspace);
		String loginIdentity = workspace == null ? email : workspace + "|" + email;
		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(loginIdentity, loginDto.getPassword()));

		UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
		if (!canAccessWorkspace(loginDto.getWorkspace(), principal.getRole())) {
			throw new BadCredentialsException("Invalid credentials");
		}
		return principal;
	}

	/**
	 * Resolves a username (the local part of a stored email) to its full address. Accounts are
	 * stored by email only, so a username is a convenience alias. Unknown or ambiguous usernames
	 * fail like wrong credentials so account details are never disclosed.
	 */
	private String resolveUsername(String username, String workspace) {
		List<String> matches = new ArrayList<>();
		if (workspace == null || "CITIZEN".equals(workspace)) {
			citizenRepository.findByEmailStartingWithIgnoreCase(username + "@").stream()
					.map(Citizen::getEmail)
					.filter(candidate -> matchesLocalPart(candidate, username))
					.forEach(matches::add);
		}
		if (workspace == null || "WORKER".equals(workspace)) {
			workerRepository.findByEmailStartingWithIgnoreCase(username + "@").stream()
					.map(Worker::getEmail)
					.filter(candidate -> matchesLocalPart(candidate, username))
					.forEach(matches::add);
		}
		if (workspace == null || "ADMIN".equals(workspace)) {
			adminUserRepository.findByEmailStartingWithIgnoreCase(username + "@").stream()
					.map(AdminUser::getEmail)
					.filter(candidate -> matchesLocalPart(candidate, username))
					.forEach(matches::add);
		}
		if (matches.size() != 1) {
			throw new BadCredentialsException("Invalid credentials");
		}
		return matches.get(0);
	}

	private static boolean matchesLocalPart(String email, String username) {
		int at = email.indexOf('@');
		return (at < 0 ? email : email.substring(0, at)).equalsIgnoreCase(username);
	}

	private static String normalizeWorkspace(String workspace) {
		if (workspace == null || workspace.isBlank()) {
			return null;
		}
		return workspace.trim().toUpperCase(Locale.ROOT);
	}

	@Transactional(readOnly = true)
	public AuthenticatedUserDto currentUser(UserPrincipal principal) {
		return switch (principal.getRole()) {
			case "CITIZEN" -> citizenRepository.findById(principal.getId())
					.map(citizen -> new AuthenticatedUserDto(citizen.getId(), citizen.getEmail(),
							citizen.getFirstName(), citizen.getLastName(), principal.getRole(), null, null,
							citizen.getStatus() == UserStatus.ACTIVE, citizen.getCreatedAt()))
					.orElseThrow(() -> new IllegalStateException("Authenticated citizen account no longer exists."));
			case "WORKER" -> workerRepository.findById(principal.getId())
					.map(worker -> new AuthenticatedUserDto(worker.getId(), worker.getEmail(),
							worker.getFirstName(), worker.getLastName(), principal.getRole(),
							worker.getDepartment().getId(), worker.getDepartment().getName(),
							worker.getStatus() == UserStatus.ACTIVE, worker.getCreatedAt()))
					.orElseThrow(() -> new IllegalStateException("Authenticated worker account no longer exists."));
			case "DEPARTMENT_ADMIN", "SYSTEM_ADMIN" -> adminUserRepository.findById(principal.getId())
					.map(admin -> new AuthenticatedUserDto(admin.getId(), admin.getEmail(),
							admin.getFirstName(), admin.getLastName(), principal.getRole(),
							admin.getDepartment() == null ? null : admin.getDepartment().getId(),
							admin.getDepartment() == null ? null : admin.getDepartment().getName(),
							admin.getStatus() == UserStatus.ACTIVE, admin.getCreatedAt()))
					.orElseThrow(() -> new IllegalStateException("Authenticated administrator account no longer exists."));
			default -> throw new IllegalStateException("Unsupported authenticated account role.");
		};
	}

	/**
	 * Looks up the security question for an identifier (email or username local part).
	 * Returns null if no unique active account has a security question set, without
	 * revealing whether the account exists.
	 */
	@Transactional(readOnly = true)
	public String findSecurityQuestion(String identifier) {
		String normalized = normalize(identifier);
		String email = normalized.contains("@") ? normalized : resolveUsernameForRecovery(normalized);
		if (email == null) {
			return null;
		}
		String question = citizenRepository.findByEmail(email)
				.filter(c -> c.getStatus() == UserStatus.ACTIVE)
				.map(Citizen::getSecurityQuestion)
				.orElse(null);
		if (question == null) {
			question = workerRepository.findByEmail(email)
					.filter(w -> w.getStatus() == UserStatus.ACTIVE)
					.map(Worker::getSecurityQuestion)
					.orElse(null);
		}
		if (question == null) {
			question = adminUserRepository.findByEmail(email)
					.filter(a -> a.getStatus() == UserStatus.ACTIVE)
					.map(AdminUser::getSecurityQuestion)
					.orElse(null);
		}
		return question;
	}

	/**
	 * Resets the password for an account after verifying its security answer.
	 * Throws BadCredentialsException when the identifier or answer is wrong so details
	 * are never disclosed.
	 */
	public void resetPasswordWithSecurityAnswer(String identifier, String answer, String newPassword) {
		String normalized = normalize(identifier);
		String email = normalized.contains("@") ? normalized : resolveUsernameForRecovery(normalized);
		if (email == null) {
			throw new BadCredentialsException("Invalid credentials");
		}
		boolean updated = resetIfMatch(citizenRepository.findByEmail(email).orElse(null), answer, newPassword);
		if (!updated) {
			updated = resetIfMatch(workerRepository.findByEmail(email).orElse(null), answer, newPassword);
		}
		if (!updated) {
			updated = resetIfMatch(adminUserRepository.findByEmail(email).orElse(null), answer, newPassword);
		}
		if (!updated) {
			throw new BadCredentialsException("Invalid credentials");
		}
	}

	private boolean resetIfMatch(Object account, String answer, String newPassword) {
		if (account == null) {
			return false;
		}
		String answerHash = null;
		String question = null;
		boolean active = false;
		if (account instanceof Citizen citizen) {
			answerHash = citizen.getSecurityAnswerHash();
			question = citizen.getSecurityQuestion();
			active = citizen.getStatus() == UserStatus.ACTIVE;
			if (active && question != null && answerHash != null && passwordEncoder.matches(normalizeAnswer(answer), answerHash)) {
				citizen.setPasswordHash(passwordEncoder.encode(newPassword));
				return true;
			}
		} else if (account instanceof Worker worker) {
			answerHash = worker.getSecurityAnswerHash();
			question = worker.getSecurityQuestion();
			active = worker.getStatus() == UserStatus.ACTIVE;
			if (active && question != null && answerHash != null && passwordEncoder.matches(normalizeAnswer(answer), answerHash)) {
				worker.setPasswordHash(passwordEncoder.encode(newPassword));
				return true;
			}
		} else if (account instanceof AdminUser admin) {
			answerHash = admin.getSecurityAnswerHash();
			question = admin.getSecurityQuestion();
			active = admin.getStatus() == UserStatus.ACTIVE;
			if (active && question != null && answerHash != null && passwordEncoder.matches(normalizeAnswer(answer), answerHash)) {
				admin.setPasswordHash(passwordEncoder.encode(newPassword));
				return true;
			}
		}
		return false;
	}

	/**
	 * Resolves a username to a single email for password recovery. Returns null when the
	 * username is unknown or ambiguous.
	 */
	private String resolveUsernameForRecovery(String username) {
		List<String> matches = new ArrayList<>();
		citizenRepository.findByEmailStartingWithIgnoreCase(username + "@").stream()
				.map(Citizen::getEmail)
				.filter(candidate -> matchesLocalPart(candidate, username))
				.forEach(matches::add);
		workerRepository.findByEmailStartingWithIgnoreCase(username + "@").stream()
				.map(Worker::getEmail)
				.filter(candidate -> matchesLocalPart(candidate, username))
				.forEach(matches::add);
		adminUserRepository.findByEmailStartingWithIgnoreCase(username + "@").stream()
				.map(AdminUser::getEmail)
				.filter(candidate -> matchesLocalPart(candidate, username))
				.forEach(matches::add);
		return matches.size() == 1 ? matches.get(0) : null;
	}

	private static String normalizeAnswer(String answer) {
		return answer.trim().toLowerCase(Locale.ROOT);
	}

	private static String normalize(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}

	private static boolean canAccessWorkspace(String workspace, String role) {
		if (workspace == null || workspace.isBlank()) {
			return true;
		}

		return switch (workspace.trim().toUpperCase()) {
			case "CITIZEN" -> "CITIZEN".equals(role);
			case "WORKER" -> "WORKER".equals(role);
			case "ADMIN" -> "DEPARTMENT_ADMIN".equals(role) || "SYSTEM_ADMIN".equals(role);
			default -> false;
		};
	}
}
