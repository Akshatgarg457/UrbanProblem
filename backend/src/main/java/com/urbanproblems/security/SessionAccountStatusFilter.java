package com.urbanproblems.security;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.filter.OncePerRequestFilter;

import com.urbanproblems.service.AccountStatusService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class SessionAccountStatusFilter extends OncePerRequestFilter {

	private final AccountStatusService accountStatusService;
	private final SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();

	public SessionAccountStatusFilter(AccountStatusService accountStatusService) {
		this.accountStatusService = accountStatusService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal
				&& !accountStatusService.isActive(principal.getRole(), principal.getId())) {
			logoutHandler.logout(request, response, authentication);
			response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			response.setContentType("application/json");
			response.getWriter().write(
					"{\"success\":false,\"error\":\"ACCOUNT_INACTIVE\",\"message\":\"This account is no longer active.\"}");
			return;
		}
		filterChain.doFilter(request, response);
	}
}
