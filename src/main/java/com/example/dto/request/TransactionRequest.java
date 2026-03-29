package com.example.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

import com.example.enums.Currency;
import com.example.enums.TransactionChannel;
import com.example.enums.TransactionType;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request object for processing a transaction")
public class TransactionRequest {

    @NotNull(message = "Card ID is required")
    @Schema(description = "Unique identifier of the card", example = "550e8400-e29b-41d4-a716-446655440000", required = true)
    private UUID cardId;

    @NotNull
    @Schema(description = "Type of transaction", example = "PURCHASE", allowableValues = {"PURCHASE", "REFUND"})
    private TransactionType transactionType;
    
    @NotNull
    @Schema(description = "Channel of transaction", example = "ONLINE", allowableValues = {"ONLINE", "POS","ATM"})
    private TransactionChannel transactionChannel;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Amount must be a valid monetary value")
    @Schema(description = "Transaction amount", example = "1500.50", minimum = "0.01")
    private BigDecimal amount;

    @NotNull
    private Currency currency = Currency.INR;

    @Size(max = 100, message = "Merchant name cannot exceed 100 characters")
    @Schema(description = "Merchant name", example = "Amazon India")
    private String merchantName;

    @Pattern(regexp = "^\\d{4}$", message = "MCC must be exactly 4 digits e.g. 5411")
    @Schema(description = "Merchant Category Code (MCC)", example = "5411")
    private String merchantCategoryCode;

    @Size(max = 100, message = "Merchant category name cannot exceed 100 characters")
    @Schema(description = "Merchant category name", example = "Grocery Stores")
    private String merchantCategoryName;
}