package com.example.controller;

import com.example.api.TransactionApi;
import com.example.dto.request.TransactionRequest;
import com.example.dto.response.ApiResponse;
import com.example.dto.response.TransactionDetailResponse;
import com.example.dto.response.TransactionSummaryResponse;
import com.example.enums.TransactionStatus;
import com.example.enums.TransactionType;
import com.example.idempotency.IdempotencyRecord;
import com.example.idempotency.TransactionIdempotencyService;
import com.example.security.CustomUserPrincipal;
import com.example.service.TransactionService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TransactionController implements TransactionApi {

    private final TransactionService transactionService;
    private final TransactionIdempotencyService transactionIdempotencyService;



    // CUSTOMER ENDPOINTS (ACCOUNT SCOPED)

    /**
     * Create transaction for an account
     * POST /api/v1/accounts/{accountId}/transactions
     */
   
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<TransactionSummaryResponse>> createTransaction(
            @PathVariable UUID cardId,
            @Valid @RequestBody TransactionRequest request,
            @AuthenticationPrincipal CustomUserPrincipal principal) {

    	IdempotencyRecord<TransactionSummaryResponse> record =
    	        transactionIdempotencyService.process(
    	                principal.getUserId(),
    	                cardId,
    	                request
    	        );

    	// 🔥 THIS IS THE EXACT PLACE YOU WERE ASKING FOR
    	if (record.isDuplicate()) {
    	    return ResponseEntity.status(HttpStatus.CONFLICT)
    	            .body(ApiResponse.success(
    	                    HttpStatus.CONFLICT,
    	                    "Duplicate request. Transaction already processed",
    	                    record.getResponseBody()
    	            ));
    	}

    	return ResponseEntity.status(HttpStatus.CREATED)
    	        .body(ApiResponse.success(
    	                HttpStatus.CREATED,
    	                "Transaction processed successfully",
    	                record.getResponseBody()
    	        ));
    }


    /**
     * Get transactions for an account (with optional filters)
     * GET /api/v1/accounts/{accountId}/transactions
     */
    @Override
    public ResponseEntity<ApiResponse<Page<TransactionSummaryResponse>>> getAccountTransactions(
            UUID accountId,
            TransactionStatus status,
            TransactionType type,
            UUID cardId,
            CustomUserPrincipal principal,
            int page,
            int size) {

        Page<TransactionSummaryResponse> result =
                transactionService.getAccountTransactions(
                        principal.getUserId(),
                        accountId,
                        status,
                        type,
                        cardId,
                        page,
                        size
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK,
                        "Transactions fetched successfully",
                        result
                )
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

}