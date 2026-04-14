package com.example.mapper;

import com.example.dto.response.TransactionDetailResponse;
import com.example.dto.response.TransactionSummaryResponse;
import com.example.entity.Transaction;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public TransactionDetailResponse toResponse(Transaction txn) {

        return TransactionDetailResponse.builder()
                .transactionId(txn.getTransactionId())
                .referenceNumber(txn.getReferenceNumber())
                .transactionReference(txn.getTransactionReference())
                .cardId(txn.getCard().getCardId())
                .maskedCardNumber(txn.getCard().getMaskedCardNumber())
                .cardFormat(txn.getCard().getCardFormat().name())
                .accountId(txn.getAccount().getAccountId())
                .accountNumber(txn.getAccount().getAccountNumber())
                .transactionType(txn.getTransactionType())
                .transactionStatus(txn.getTransactionStatus())
                .amount(txn.getAmount())
                .currency(txn.getCurrency())
                .merchantName(txn.getMerchantName())
                .merchantCategoryCode(txn.getMerchantCategoryCode())
                .merchantCategoryName(txn.getMerchantCategoryName())
                .balanceBefore(txn.getBalanceBefore())
                .balanceAfter(txn.getBalanceAfter())
                .declineReason(txn.getDeclineReason())
                .transactionTime(txn.getTransactionTime())
                .build();
    }
    
    public TransactionSummaryResponse toSummaryResponse(Transaction txn) {

        return TransactionSummaryResponse.builder()
                .transactionId(txn.getTransactionId())
                .referenceNumber(txn.getReferenceNumber())
                .transactionStatus(txn.getTransactionStatus())
                .transactionType(txn.getTransactionType())
                .amount(txn.getAmount())
                .currency(txn.getCurrency())
                .merchantName(txn.getMerchantName())
                .transactionTime(txn.getTransactionTime())
                .build();
    }
}