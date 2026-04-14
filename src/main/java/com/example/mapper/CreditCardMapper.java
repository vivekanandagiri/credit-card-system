package com.example.mapper;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.springframework.stereotype.Component;

import com.example.dto.response.CreditCardIssuanceResponse;
import com.example.dto.response.CreditCardResponse;
import com.example.entity.CreditCard;
/**
 * Projects the CreditCard domain entity into safe, immutable DTOs for the API layer.
 * <p>
 * This mapper relies on deep object graph traversal (e.g., Card -> Account -> Customer).
 */
@Component
public class CreditCardMapper {

	/**
     * Maps a comprehensive view of the credit card, including inherited limits and lifecycle data.
     * Used for detailed account views and back-office portals.
     */
    public CreditCardResponse toResponse(CreditCard creditCard) {

    	if (creditCard.getExpiresAt() == null) {
    	    throw new IllegalStateException("Card expiry not set");
    	}

    	
    	LocalDateTime expiry = LocalDateTime.ofInstant(
    	        creditCard.getExpiresAt(), ZoneOffset.UTC);

    	int month = expiry.getMonthValue();
    	int year = expiry.getYear();

    	String expiryFormatted = String.format("%02d/%02d",
    	        month,
    	        year % 100);
    	
        return CreditCardResponse.builder()
                .cardId(creditCard.getCardId())

                // ACCOUNT
                .accountId(creditCard.getCreditAccount().getAccountId())
                .accountNumber(creditCard.getCreditAccount().getAccountNumber())

                // CUSTOMER
                .customerId(creditCard.getCreditAccount().getCustomer().getCustomerId())
                .customerName(creditCard.getCreditAccount().getCustomer().getFirstName()+ " " +creditCard.getCreditAccount().getCustomer().getLastName())

                // PRODUCT
                .cardProductId(creditCard.getCardProduct().getCardProductId())
                .cardProductName(creditCard.getCardProduct().getProductName())
                .networkType(creditCard.getCardProduct().getNetworkType())
                .cardType(creditCard.getCardProduct().getCardType())

                // CARD
                .cardFormat(creditCard.getCardFormat())
                .cardStatus(creditCard.getCardStatus())
                .issuanceReason(creditCard.getIssuanceReason())
                .maskedCardNumber(creditCard.getMaskedCardNumber())

                // VALIDITY
                .expiryMonth(creditCard.getExpiryMonth())
                .expiryYear(creditCard.getExpiryYear())
                .expiryFormatted(expiryFormatted)

                // LIMITS
                .atmDailyLimit(creditCard.getCardProduct().getAtmDailyLimit())
                .posDailyLimit(creditCard.getCardProduct().getPosDailyLimit())
                .ecommerceDailyLimit(creditCard.getCardProduct().getEcommerceDailyLimit())

                // FEATURES
                .contactlessEnabled(creditCard.getCardProduct().getContactlessEnabled())
                .internationalUsageAllowed(creditCard.getCardProduct().getInternationalUsageAllowed())
                .onlineTransactionsAllowed(creditCard.getCardProduct().getOnlineTransactionsAllowed())
                .atmWithdrawalAllowed(creditCard.getCardProduct().getAtmWithdrawalAllowed())

                // LIFECYCLE
                .issuedAt(creditCard.getIssuedAt())
                .activatedAt(creditCard.getActivatedAt())
                .expiresAt(creditCard.getExpiresAt())
                .blockedAt(creditCard.getBlockedAt())
                .cancelledAt(creditCard.getCancelledAt())

                // AUDIT
                .issuedBy(creditCard.getIssuedBy())

                .build();
    }

    /**
     * Maps a lightweight summary for the immediate response after a successful card issuance.
     */
    public CreditCardIssuanceResponse toIssueResponse(CreditCard creditCard) {
    	if (creditCard.getExpiresAt() == null) {
    	    throw new IllegalStateException("Card expiry not set");
    	}

    	LocalDateTime expiry = LocalDateTime.ofInstant(
    	        creditCard.getExpiresAt(), ZoneOffset.UTC);

    	String expiryFormatted = String.format("%02d/%02d",
    	        expiry.getMonthValue(),
    	        expiry.getYear() % 100);
        return CreditCardIssuanceResponse.builder()
                .cardId(creditCard.getCardId())
                .accountId(creditCard.getCreditAccount().getAccountId())
                .cardStatus(creditCard.getCardStatus()) 
                .customerName(
                        creditCard.getCreditAccount().getCustomer().getFirstName() + " " +
                        creditCard.getCreditAccount().getCustomer().getLastName()
                )
                .maskedCardNumber(creditCard.getMaskedCardNumber())
                .networkType(creditCard.getCardProduct().getNetworkType())
                .expiryFormatted(expiryFormatted)
                .build();
    }
}