package com.example.controller;

import com.example.api.TransactionApi;
import com.example.dto.request.TransactionRequest;
import com.example.dto.response.ApiResponse;
import com.example.dto.response.TransactionDetailResponse;
import com.example.dto.response.TransactionSummaryResponse;
import com.example.enums.TransactionStatus;
import com.example.enums.TransactionType;
import com.example.security.CustomUserPrincipal;
import com.example.service.TransactionService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class TransactionController implements TransactionApi {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    // CUSTOMER ENDPOINTS (ACCOUNT SCOPED)

    /**
     * Create transaction for an account
     * POST /api/v1/accounts/{accountId}/transactions
     */
    @Override
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<TransactionSummaryResponse>> createTransaction(
            @Valid @RequestBody TransactionRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        TransactionSummaryResponse result =
                transactionService.postTransaction(
                        principal.getUserId(),
                        request.getCardId(),   
                        request
                );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        HttpStatus.CREATED,
                        "Transaction processed successfully",
                        result
                ));
    }

    /**
     * Get transactions for an account (with optional filters)
     * GET /api/v1/accounts/{accountId}/transactions
     */
    @Override
    public ResponseEntity<ApiResponse<List<TransactionSummaryResponse>>> getAccountTransactions(
            UUID accountId,
            TransactionStatus status,
            TransactionType type,
            UUID cardId,
            CustomUserPrincipal principal) {

        List<TransactionSummaryResponse> result =
                transactionService.getAccountTransactions(
                        principal.getUserId(),
                        accountId,
                        status,
                        type,
                        cardId
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                		HttpStatus.OK,
                		"Transactions fetched successfully",
                		result)
        );
    }

    /**
     * Get specific transaction from an account
     * GET /api/v1/accounts/{accountId}/transactions/{transactionId}
     */
    @Override
    public ResponseEntity<ApiResponse<TransactionDetailResponse>> getAccountTransactionById(
            UUID accountId,
            UUID transactionId,
            CustomUserPrincipal principal) {

        TransactionDetailResponse result =
                transactionService.getAccountTransactionById(
                        principal.getUserId(),
                        accountId,
                        transactionId
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                		HttpStatus.OK,
                		"Transaction fetched successfully", 
                		result)
        );
    }

//    // =====================================================
//    // ADMIN ENDPOINTS (GLOBAL)
//    // =====================================================
//
//    /**
//     * Get all transactions (admin) with filters
//     * GET /api/v1/transactions
//     */
//    @Override
//    public ResponseEntity<ApiResponse<List<TransactionSummaryResponse>>> getAllTransactions(
//            TransactionStatus status,
//            TransactionType type,
//            UUID accountId,
//            UUID userId) {
//
//        List<TransactionSummaryResponse result =
//                transactionService.getAllTransactions(
//                        status,
//                        type,
//                        accountId,
//                        userId
//                );
//
//        return ResponseEntity.ok(
//                ApiResponse.success("Transactions fetched successfully", result)
//        );
//    }
//
//    /**
//     * Get transaction by ID (admin)
//     * GET /api/v1/transactions/{transactionId}
//     */
//    @Override
//    public ResponseEntity<ApiResponse<TransactionDetailResponse>> getTransactionById(
//            UUID transactionId) {
//
//        TransactionDetailResponse result =
//                transactionService.getTransactionById(transactionId);
//
//        return ResponseEntity.ok(
//                ApiResponse.success("Transaction fetched successfully", result)
//        );
//    }
}