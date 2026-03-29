package com.example.api;

import com.example.dto.request.TransactionRequest;
import com.example.dto.response.ApiResponse;
import com.example.dto.response.TransactionDetailResponse;
import com.example.dto.response.TransactionSummaryResponse;
import com.example.enums.TransactionStatus;
import com.example.enums.TransactionType;
import com.example.security.CustomUserPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequestMapping("/api/v1")
@Tag(name = "Transaction API")
public interface TransactionApi {

    // ================= CUSTOMER =================

	@Operation(summary = "Create transaction (Card based)")
	@PostMapping("/transactions")
	ResponseEntity<ApiResponse<TransactionSummaryResponse>> createTransaction(
	        @Valid @RequestBody TransactionRequest request,
	        @AuthenticationPrincipal CustomUserPrincipal principal);
	
	
    @Operation(summary = "Get account transactions with filters")
    @GetMapping("/accounts/{accountId}/transactions")
    ResponseEntity<ApiResponse<List<TransactionSummaryResponse>>> getAccountTransactions(
            @PathVariable UUID accountId,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) UUID cardId,
            @AuthenticationPrincipal CustomUserPrincipal principal);

    @Operation(summary = "Get transaction by ID for account")
    @GetMapping("/accounts/{accountId}/transactions/{transactionId}")
    ResponseEntity<ApiResponse<TransactionDetailResponse>> getAccountTransactionById(
            @PathVariable UUID accountId,
            @PathVariable UUID transactionId,
            @AuthenticationPrincipal CustomUserPrincipal principal);

    // ================= ADMIN =================

//    @Operation(summary = "Get all transactions (Admin)")
//    @GetMapping("/transactions")
//    ResponseEntity<ApiResponse<List<TransactionSummaryResponse>>> getAllTransactions(
//            @RequestParam(required = false) TransactionStatus status,
//            @RequestParam(required = false) TransactionType type,
//            @RequestParam(required = false) UUID accountId,
//            @RequestParam(required = false) UUID userId);
//
//    @Operation(summary = "Get transaction by ID (Admin)")
//    @GetMapping("/transactions/{transactionId}")
//    ResponseEntity<ApiResponse<TransactionDetailResponse>> getTransactionById(
//            @PathVariable UUID transactionId);
}