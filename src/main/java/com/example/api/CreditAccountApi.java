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

@Tag(name = "Credit Account API")
@RequestMapping("/api/v1/accounts")
public interface CreditAccountApi {

    @Operation(summary = "Get accounts (Customer: own, Admin: all)")
    @GetMapping
    ResponseEntity<ApiResponse<List<CreditAccountResponse>>> getAccounts(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestParam(required = false) String status);

    @Operation(summary = "Get account by ID")
    @GetMapping("/{accountId}")
    ResponseEntity<ApiResponse<CreditAccountResponse>> getAccountById(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID accountId);

    @Operation(summary = "Update account status (Admin)")
    @PatchMapping("/{accountId}/status")
    ResponseEntity<ApiResponse<CreditAccountResponse>> updateAccountStatus(
            @PathVariable UUID accountId,
            @Valid @RequestBody CreditAccountStatusUpdateRequest request);
}