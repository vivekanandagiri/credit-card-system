package com.example.dto.response;

import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.example.enums.Currency;
import com.example.enums.TransactionStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Minimal transaction response shown to customer after transaction completion")
public class TransactionSummaryResponse {

    @Schema(
        description = "Unique transaction identifier",
        example = "d290f1ee-6c54-4b01-90e6-d701748f0851"
    )
    private UUID transactionId;

    @Schema(
        description = "Reference number for tracking the transaction",
        example = "TXN123456789"
    )
    private String referenceNumber;

    @Schema(
        description = "Transaction status",
        example = "APPROVED",
        allowableValues = {"APPROVED", "DECLINED"}
    )
    private TransactionStatus transactionStatus;

    @Schema(
        description = "Transaction amount",
        example = "1500.50"
    )
    private BigDecimal amount;

    @Schema(
        description = "Currency code (ISO 4217)",
        example = "INR"
    )
    private Currency currency;

    @Schema(
        description = "Merchant name",
        example = "Amazon"
    )
    private String merchantName;

    @Schema(
        description = "Transaction timestamp (UTC)",
        example = "2026-03-20T10:15:30Z"
    )
    private Instant transactionTime;
}