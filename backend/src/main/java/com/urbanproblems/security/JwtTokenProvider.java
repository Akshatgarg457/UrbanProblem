package com.urbanproblems.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * Issues and verifies HS256 JWTs (jjwt 0.12 API).
 * The secret is validated at startup so a missing or weak secret fails fast.
 */
@Slf4j
@Component
@Profile("test")
public class JwtTokenProvider {

	private static final int MIN_SECRET_BYTES = 32;

	@Value("${jwt.secret:}")
	private String jwtSecret;

	@Value("${jwt.expiration-ms:86400000}")
	private long jwtExpirationMs;

	private SecretKey signingKey;

	@PostConstruct
	void init() {
		if (jwtSecret == null || jwtSecret.isBlank()) {
			throw new IllegalStateException("JWT_SECRET must be set to a value of at least 32 bytes.");
		}
		if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
			throw new IllegalStateException("JWT_SECRET must be at least 32 bytes long.");
		}
		this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
	}

	public String generateTokenFromUserPrincipal(UserPrincipal principal) {
		Date now = new Date();
		var builder = Jwts.builder()
				.subject(principal.getEmail())
				.claim("userId", principal.getId())
				.claim("role", principal.getRole())
				.issuedAt(now)
				.expiration(new Date(now.getTime() + jwtExpirationMs));
		if (principal.getType() != null) {
			builder.claim("type", principal.getType());
		}
		if (principal.getDepartmentId() != null) {
			builder.claim("departmentId", principal.getDepartmentId());
		}
		return builder.signWith(signingKey).compact();
	}

	/**
	 * Verifies signature and expiry and returns the authenticated principal, or null when the token is
	 * missing, malformed, tampered with, or expired.
	 */
	public UserPrincipal authenticate(String token) {
		if (token == null || token.isBlank()) {
			return null;
		}
		try {
			Claims claims = Jwts.parser()
					.verifyWith(signingKey)
					.build()
					.parseSignedClaims(token)
					.getPayload();
			Object userId = claims.get("userId");
			String role = claims.get("role", String.class);
			if (!(userId instanceof Number) || role == null) {
				return null;
			}
			Object deptId = claims.get("departmentId");
			Long departmentId = deptId instanceof Number n ? n.longValue() : null;
			return new UserPrincipal(
					((Number) userId).longValue(),
					claims.getSubject(),
					null,
					role,
					claims.get("type", String.class),
					departmentId,
					true);
		} catch (JwtException | IllegalArgumentException e) {
			log.debug("Rejected JWT: {}", e.getClass().getSimpleName());
			return null;
		}
	}
}
