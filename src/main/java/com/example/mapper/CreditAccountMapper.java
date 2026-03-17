package com.example.mapper;

import com.example.dto.response.CreditAccountResponse;
import com.example.entity.CreditAccount;

import org.springframework.stereotype.Component;

@Component
public class CreditAccountMapper {

    public CreditAccountResponse toResponse(CreditAccount account) {

        return CreditAccountResponse.builder()
                .accountId(account.getAccountId())
                .accountNumber(account.getAccountNumber())
                .customerId(account.getCustomer().getCustomerId())
                .customerName(account.getCustomer().getFirstName()
                        + " " + account.getCustomer().getLastName())
                .creditProductId(account.getCreditProduct().getCreditProductId())
                .creditProductName(account.getCreditProduct().getProductName())
                .applicationId(account.getApplication().getApplicationId())
                .accountStatus(account.getAccountStatus().name())
                .creditLimit(account.getCreditLimit())
                .apr(account.getApr())
                .currentBalance(account.getCurrentBalance())
                .availableBalance(account.getAvailableBalance())
                .statementCycleDay(account.getStatementCycleDay())
                .lastStatementDate(account.getLastStatementDate())
                .lastStatementBalance(account.getLastStatementBalance())
                .nextDueDate(account.getNextDueDate())
                .minimumDueAmount(account.getMinimumDueAmount())
                .lastPaymentDate(account.getLastPaymentDate())
                .lastPaymentAmount(account.getLastPaymentAmount())
                .activatedAt(account.getActivatedAt())
                .closedAt(account.getClosedAt())
                .build();
    }
}