package com.example.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.example.enums.*;

import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Detailed response for a credit card")
public class CreditCardResponse {

	@Schema(description = "Unique card ID")
	private UUID cardId;

	// ================= ACCOUNT =================
	@Schema(description = "Account ID")
	private UUID accountId;

	@Schema(description = "Account number", example = "ACC123456")
	private String accountNumber;

	// ================= CUSTOMER =================
	@Schema(description = "Customer ID")
	private UUID customerId;

	@Schema(description = "Customer full name", example = "Vivek Kumar")
	private String customerName;

	// ================= CARD PRODUCT =================
	@Schema(description = "Card product ID")
	private UUID cardProductId;

	@Schema(description = "Card product name", example = "Platinum Credit Card")
	private String cardProductName;

	@Schema(description = "Network type", example = "VISA")
	private NetworkType networkType;

	@Schema(description = "Card type", example = "PLATINUM")
	private CardType cardType;

	// ================= CARD DETAILS =================
	@Schema(description = "Card format", example = "VIRTUAL")
	private CardFormat cardFormat;

	@Schema(description = "Card status", example = "ACTIVE")
	private CardStatus cardStatus;

	@Schema(description = "Issuance reason", example = "NEW_CARD")
	private CardIssuanceReason issuanceReason;

	@Schema(description = "Masked card number", example = "411111XXXXXX1234")
	private String maskedCardNumber;

	// ================= VALIDITY =================
	@Schema(description = "Expiry month", example = "3")
	private Integer expiryMonth;

	@Schema(description = "Expiry year", example = "2029")
	private Integer expiryYear;

	@Schema(description = "Formatted expiry (MM/YY)", example = "03/29")
	private String expiryFormatted;

	// ================= LIMITS =================
	@Schema(description = "ATM daily withdrawal limit")
	private BigDecimal atmDailyLimit;

	@Schema(description = "POS daily limit")
	private BigDecimal posDailyLimit;

	@Schema(description = "E-commerce daily limit")
	private BigDecimal ecommerceDailyLimit;

	// ================= FEATURES =================
	private Boolean contactlessEnabled;
	private Boolean internationalUsageAllowed;
	private Boolean onlineTransactionsAllowed;
	private Boolean atmWithdrawalAllowed;

	// ================= LIFECYCLE =================
	private Instant issuedAt;
	private Instant activatedAt;
	private Instant expiresAt;
	private Instant blockedAt;
	private Instant cancelledAt;

	// ================= AUDIT =================
	@Schema(description = "Who issued the card", example = "ADMIN")
	private String issuedBy;
}