package com.example.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.example.api.CreditAccountApi;
import com.example.dto.request.CreditAccountStatusUpdateRequest;
import com.example.dto.response.ApiResponse;
import com.example.dto.response.CreditAccountResponse;
import com.example.enums.AccountStatus;
import com.example.security.CustomUserPrincipal;
import com.example.service.CreditAccountService;

import jakarta.validation.Valid;

@RestController
public class CreditAccountController implements CreditAccountApi {

    private final CreditAccountService accountService;

    public CreditAccountController(CreditAccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * CUSTOMER → gets only their accounts
     * ADMIN → gets all accounts (optional filter by status)
     */
    @Override
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public ResponseEntity<ApiResponse<List<CreditAccountResponse>>> getAccounts(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestParam(required = false) AccountStatus status) {

        List<CreditAccountResponse> responses =
                accountService.getAccounts(
                        principal.getUserId(),
                        principal.getRole(), // ✅ pass enum directly (better)
                        status
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK,
                        "Accounts fetched successfully",
                        responses
                )
        );
    }

    /**
     * CUSTOMER → only own account
     * ADMIN → any account
     */
    @Override
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public ResponseEntity<ApiResponse<CreditAccountResponse>> getAccountById(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID accountId) {

        CreditAccountResponse response =
                accountService.getAccountById(
                        principal.getUserId(),
                        principal.getRole(), // ✅ enum instead of string
                        accountId
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK,
                        "Account fetched successfully",
                        response
                )
        );
    }

    /**
     * ADMIN → update account status
     */
    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CreditAccountResponse>> updateAccountStatus(
            @PathVariable UUID accountId,
            @Valid @RequestBody CreditAccountStatusUpdateRequest request) {

        CreditAccountResponse response =
                accountService.updateAccountStatus(accountId, request);

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK,
                        "Account status updated successfully",
                        response
                )
        );
    }
}