package com.example.service;

import com.example.dto.request.TransactionRequest;
import com.example.dto.response.TransactionDetailResponse;
import com.example.dto.response.TransactionSummaryResponse;
import com.example.enums.TransactionStatus;
import com.example.enums.TransactionType;

import java.util.List;
import java.util.UUID;

public interface TransactionService {

    // ── Customer ──

    // Post a transaction using a card
	TransactionSummaryResponse postTransaction(
            UUID userId,
            UUID cardId,
            TransactionRequest request
    );
    // View all transactions on a specific account
	List<TransactionSummaryResponse> getAccountTransactions(
            UUID userId,
            UUID accountId,
            TransactionStatus status,
            TransactionType type,
            UUID cardId
    );
    // View a specific transaction
	TransactionDetailResponse getAccountTransactionById(
            UUID userId,
            UUID accountId,
            UUID transactionId
    );

    // ── Admin ──

//    // View all transactions
//	List<TransactionSummaryResponse> getAllTransactions(
//            TransactionStatus status,
//            TransactionType type,
//            UUID accountId,
//            UUID userId
//    );
//
//    // View a specific transaction by ID
//    TransactionDetailResponse getTransactionById(UUID transactionId);
}