package com.example.service;

import com.example.dto.request.TransactionRequest;
import com.example.dto.response.TransactionDetailResponse;
import com.example.dto.response.TransactionSummaryResponse;
import com.example.entity.CreditAccount;
import com.example.entity.Payment;
import com.example.enums.TransactionStatus;
import com.example.enums.TransactionType;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
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
	Page<TransactionSummaryResponse> getAccountTransactions(
			UUID userId,
			UUID accountId,
			TransactionStatus status,
			TransactionType type,
			UUID cardId,
			int page,
			int size
	);
    // View a specific transaction
	TransactionDetailResponse getAccountTransactionById(
            UUID userId,
            UUID accountId,
            UUID transactionId
    );
	
	//make bill payment 
	TransactionSummaryResponse recordPayment(
		    CreditAccount account,
		    Payment payment,
		    BigDecimal balanceBefore,
		    BigDecimal balanceAfter
		);
	
	TransactionSummaryResponse getByTransactionReference(String transactionReference);
	
	//Interest and Late fee 
	public TransactionSummaryResponse postSystemTransaction(
	        CreditAccount account,
	        TransactionType type,
	        BigDecimal amount,
	        String description,
	        String reference);

}