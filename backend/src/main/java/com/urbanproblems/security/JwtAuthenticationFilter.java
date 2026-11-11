package com.urbanproblems.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.urbanproblems.service.AccountStatusService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtTokenProvider tokenProvider;
	private final AccountStatusService accountStatusService;

	public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, AccountStatusService accountStatusService) {
		this.tokenProvider = tokenProvider;
		this.accountStatusService = accountStatusService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		UserPrincipal principal = tokenProvider.authenticate(getJwtFromRequest(request));
		// Re-check the live account status so a deactivated or unapproved account cannot keep
		// using a token that was issued while it was active.
		if (principal != null && accountStatusService.isActive(principal.getRole(), principal.getId())) {
			UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
					principal, null, principal.getAuthorities());
			authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
			SecurityContextHolder.getContext().setAuthentication(authentication);
		}
		filterChain.doFilter(request, response);
	}

	private String getJwtFromRequest(HttpServletRequest request) {
		String bearerToken = request.getHeader("Authorization");
		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
			return bearerToken.substring(7);
		}
		return null;
	}
}
