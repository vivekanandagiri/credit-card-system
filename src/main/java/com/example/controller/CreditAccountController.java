package com.example.controller;

import com.example.api.CreditAccountApi;
import com.example.dto.request.CreditAccountStatusUpdateRequest;
import com.example.dto.response.CreditAccountResponse;
import com.example.dto.response.ApiResponse;
import com.example.security.CustomUserPrincipal;
import com.example.service.CreditAccountService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class CreditAccountController implements CreditAccountApi {

    private final CreditAccountService accountService;

    public CreditAccountController(CreditAccountService accountService) {
		this.accountService = accountService;
	}

	@Override
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<List<CreditAccountResponse>>> getMyAccounts(
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        return ResponseEntity.ok(
                accountService.getMyAccounts(principal.getUserId()));
    }

    @Override
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<CreditAccountResponse>> getMyAccountById(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            UUID accountId) {

        return ResponseEntity.ok(
                accountService.getMyAccountById(principal.getUserId(), accountId));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<CreditAccountResponse>>> getAllAccounts() {
        return ResponseEntity.ok(accountService.getAllAccounts());
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<CreditAccountResponse>>> getAccountsByStatus(
            String status) {

        return ResponseEntity.ok(accountService.getAccountsByStatus(status));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CreditAccountResponse>> getAccountById(UUID accountId) {
        return ResponseEntity.ok(accountService.getAccountById(accountId));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CreditAccountResponse>> updateAccountStatus(
            UUID accountId,
            @Valid CreditAccountStatusUpdateRequest request) {

        return ResponseEntity.ok(
                accountService.updateAccountStatus(accountId, request));
    }
}