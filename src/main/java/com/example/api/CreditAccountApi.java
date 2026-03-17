package com.example.api;

import com.example.dto.request.CreditAccountStatusUpdateRequest;
import com.example.dto.response.CreditAccountResponse;
import com.example.dto.response.ApiResponse;
import com.example.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Credit Account API", description = "Credit Account management endpoints")
@RequestMapping("/api/v1/credit-accounts")
public interface CreditAccountApi {

    // ================= CUSTOMER =================

    @Operation(summary = "Get all accounts of logged-in customer")
    @GetMapping("/me")
    ResponseEntity<ApiResponse<List<CreditAccountResponse>>> getMyAccounts(
            @AuthenticationPrincipal CustomUserPrincipal principal);

    @Operation(summary = "Get specific account of logged-in customer")
    @GetMapping("/me/{accountId}")
    ResponseEntity<ApiResponse<CreditAccountResponse>> getMyAccountById(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID accountId);

    // ================= ADMIN =================

    @Operation(summary = "Get all accounts (Admin)")
    @GetMapping
    ResponseEntity<ApiResponse<List<CreditAccountResponse>>> getAllAccounts();

    @Operation(summary = "Filter accounts by status (Admin)")
    @GetMapping(params = "status")
    ResponseEntity<ApiResponse<List<CreditAccountResponse>>> getAccountsByStatus(
            @RequestParam String status);

    @Operation(summary = "Get account by ID (Admin)")
    @GetMapping("/{accountId}")
    ResponseEntity<ApiResponse<CreditAccountResponse>> getAccountById(
            @PathVariable UUID accountId);

    @Operation(summary = "Update account status (Admin)")
    @PatchMapping("/{accountId}")
    ResponseEntity<ApiResponse<CreditAccountResponse>> updateAccountStatus(
            @PathVariable UUID accountId,
            @Valid @RequestBody CreditAccountStatusUpdateRequest request);
}