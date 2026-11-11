package com.urbanproblems.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.urbanproblems.dto.ApiResponse;
import com.urbanproblems.dto.AuthenticatedUserDto;
import com.urbanproblems.dto.AuthRequestDto;
import com.urbanproblems.dto.AuthResponseDto;
import com.urbanproblems.dto.CitizenRegistrationDto;
import com.urbanproblems.dto.ForgotPasswordQuestionRequestDto;
import com.urbanproblems.dto.ForgotPasswordResetRequestDto;
import com.urbanproblems.dto.PublicDepartmentDto;
import com.urbanproblems.dto.SecurityQuestionResponseDto;
import com.urbanproblems.dto.WorkerRegistrationDto;
import com.urbanproblems.dto.WorkerResponseDto;
import com.urbanproblems.service.AuthService;
import com.urbanproblems.service.DepartmentService;
import com.urbanproblems.service.WorkerService;
import com.urbanproblems.security.UserPrincipal;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final AuthService authService;
	private final WorkerService workerService;
	private final DepartmentService departmentService;
	private final SecurityContextRepository securityContextRepository;

	public AuthController(AuthService authService, WorkerService workerService,
			DepartmentService departmentService, SecurityContextRepository securityContextRepository) {
		this.authService = authService;
		this.workerService = workerService;
		this.departmentService = departmentService;
		this.securityContextRepository = securityContextRepository;
	}

	@GetMapping("/csrf")
	public ResponseEntity<Map<String, String>> csrfToken(CsrfToken csrfToken) {
		return ResponseEntity.ok(Map.of("token", csrfToken.getToken()));
	}

	@GetMapping("/me")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<AuthenticatedUserDto> currentUser() {
		UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		return ResponseEntity.ok(authService.currentUser(principal));
	}

	@PostMapping("/register")
	public ResponseEntity<AuthResponseDto> registerCitizen(@Valid @RequestBody CitizenRegistrationDto registrationDto) {
		return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerCitizen(registrationDto));
	}

	/**
	 * Public worker application. Creates an INACTIVE account and issues no token: a system
	 * administrator must approve it before the worker can authenticate.
	 */
	@PostMapping("/register/worker")
	public ResponseEntity<WorkerResponseDto> registerWorker(@Valid @RequestBody WorkerRegistrationDto registrationDto) {
		return ResponseEntity.status(HttpStatus.CREATED).body(workerService.applyAsWorker(registrationDto));
	}

	/** Active departments, for the public worker-registration form. */
	@GetMapping("/departments")
	public ResponseEntity<List<PublicDepartmentDto>> getPublicDepartments() {
		return ResponseEntity.ok(departmentService.getPublicDepartments());
	}

	@PostMapping("/login")
	public ResponseEntity<?> login(@Valid @RequestBody AuthRequestDto loginDto,
			HttpServletRequest request, HttpServletResponse response) {
		try {
			UserPrincipal principal = authService.login(loginDto);
			if (request.getSession(false) != null) {
				request.changeSessionId();
			}
			UserPrincipal sessionPrincipal = new UserPrincipal(principal.getId(), principal.getEmail(), null,
					principal.getRole(), principal.getType(), principal.getDepartmentId(), principal.isEnabled());
			var authentication = UsernamePasswordAuthenticationToken.authenticated(
					sessionPrincipal, null, sessionPrincipal.getAuthorities());
			SecurityContext context = SecurityContextHolder.createEmptyContext();
			context.setAuthentication(authentication);
			SecurityContextHolder.setContext(context);
			securityContextRepository.saveContext(context, request, response);
			return ResponseEntity.ok(authService.currentUser(sessionPrincipal));
		} catch (DisabledException e) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN)
					.body(ApiResponse.error("ACCOUNT_INACTIVE",
							"This account is not active. Worker applications must be approved by an administrator before signing in."));
		} catch (AuthenticationException e) {
			// Keep invalid credentials and workspace mismatches indistinguishable.
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(ApiResponse.error("INVALID_CREDENTIALS",
							"Email or password is incorrect, or this account cannot access the selected workspace."));
		}
	}

	@PostMapping("/logout")
	public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
		new SecurityContextLogoutHandler().logout(
				request, response, SecurityContextHolder.getContext().getAuthentication());
		return ResponseEntity.noContent().build();
	}

	/**
	 * Returns the security question for an account. When the identifier is unknown or the account
	 * has no security question, the response still returns 200 with a null question so account
	 * existence is not revealed.
	 */
	@PostMapping("/forgot-password/question")
	public ResponseEntity<SecurityQuestionResponseDto> getSecurityQuestion(
			@Valid @RequestBody ForgotPasswordQuestionRequestDto requestDto) {
		String question = authService.findSecurityQuestion(requestDto.getIdentifier());
		return ResponseEntity.ok(new SecurityQuestionResponseDto(question));
	}

	/**
	 * Resets a password after verifying the account's security answer. On failure returns 401
	 * without distinguishing between a missing account and a wrong answer.
	 */
	@PostMapping("/forgot-password/reset")
	public ResponseEntity<ApiResponse> resetPassword(
			@Valid @RequestBody ForgotPasswordResetRequestDto requestDto) {
		try {
			authService.resetPasswordWithSecurityAnswer(
					requestDto.getIdentifier(), requestDto.getAnswer(), requestDto.getNewPassword());
			return ResponseEntity.ok(ApiResponse.success("Password reset successfully."));
		} catch (org.springframework.security.core.AuthenticationException e) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(ApiResponse.error("INVALID_ANSWER", "The answer is incorrect or the account cannot be recovered this way."));
		}
	}
}
