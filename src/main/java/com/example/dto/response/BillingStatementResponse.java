package com.example.dto.response;

import com.example.enums.StatementStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class BillingStatementResponse {

    private UUID statementId;
    private UUID accountId;
    private String accountNumber;

    private LocalDate billingPeriodStart;
    private LocalDate billingPeriodEnd;

    private BigDecimal openingBalance;
    private BigDecimal totalDebits;
    private BigDecimal totalCredits;
    private BigDecimal interestCharged;
    private BigDecimal remainingAmount;
    private BigDecimal lateFee;
    private BigDecimal closingBalance;

    private BigDecimal totalAmountDue;
    private BigDecimal minimumDueAmount;

    private LocalDate dueDate;

    private BigDecimal amountPaid;
    private StatementStatus statementStatus;

    private Instant generatedAt;
}