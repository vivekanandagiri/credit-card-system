package com.example.dto.response;

import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.example.enums.Currency;
import com.example.enums.TransactionChannel;
import com.example.enums.TransactionStatus;
import com.example.enums.TransactionType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response object representing a transaction result")
public class TransactionDetailResponse {

    @Schema(description = "Unique transaction ID", example = "d290f1ee-6c54-4b01-90e6-d701748f0851")
    private UUID transactionId;

    @Schema(description = "Unique reference number for tracking", example = "TXN123456789")
    private String internalReference;
    
    @Schema(description = "Unique Network reference number for Idempotency", example = "NET873827387")
    private String networkReference;

    // Card info
    @Schema(description = "Card ID used for transaction")
    private UUID cardId;

    @Schema(description = "Masked card number", example = "XXXX-XXXX-XXXX-1234")
    private String maskedCardNumber;

    @Schema(description = "Card format", example = "VIRTUAL", allowableValues = {"VIRTUAL", "PHYSICAL"})
    private String cardFormat; 

    // Account info
    @Schema(description = "Account ID linked to the card")
    private UUID accountId;

    @Schema(description = "Masked account number", example = "XXXXXX1234")
    private String accountNumber;

    // Transaction details
    @Schema(description = "Transaction type", example = "PURCHASE", allowableValues = {"PURCHASE", "ONLINE"})
    private TransactionType transactionType;
    
    @Schema(description = "Transaction Channel", example = "ONLINE")
    private TransactionChannel transactionChannel;

    @Schema(description = "Transaction status", example = "APPROVED", allowableValues = {"APPROVED", "DECLINED"})
    private TransactionStatus transactionStatus;

    // Amount
    @Schema(description = "Transaction amount", example = "1500.50")
    private BigDecimal amount;

    @Schema(description = "Currency code (ISO 4217)", example = "INR")
    private Currency currency;

    // Merchant
    @Schema(description = "Merchant name", example = "Amazon India")
    private String merchantName;

    @Schema(description = "Merchant Category Code", example = "5411")
    private String merchantCategoryCode;

    @Schema(description = "Merchant category name", example = "GROCERY_STORES")
    private String merchantCategoryName;


    // Decline reason
    @Schema(description = "Reason for decline (if any)", example = "INSUFFICIENT_FUNDS")
    private String declineReason;

    // Timestamp
    @Schema(description = "Transaction timestamp in UTC", example = "2026-03-20T10:15:30Z")
    private Instant transactionTime;
}