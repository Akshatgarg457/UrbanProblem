package com.urbanproblems.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.urbanproblems.dto.ChangePasswordRequestDto;
import com.urbanproblems.security.AccessRules;
import com.urbanproblems.service.AccountService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/account")
public class AccountController {

	private final AccountService accountService;

	public AccountController(AccountService accountService) {
		this.accountService = accountService;
	}

	@PostMapping("/change-password")
	public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequestDto request) {
		accountService.changePassword(AccessRules.currentPrincipal(), request);
		return ResponseEntity.noContent().build();
	}
}
