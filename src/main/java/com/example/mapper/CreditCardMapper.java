package com.example.mapper;

import org.springframework.stereotype.Component;

import com.example.dto.response.CreditCardIssuanceResponse;
import com.example.dto.response.CreditCardResponse;
import com.example.entity.CreditCard;

@Component
public class CreditCardMapper {

    public CreditCardResponse toResponse(CreditCard creditCard) {

        String expiryFormatted = String.format("%02d/%02d",
                creditCard.getExpiryMonth(),
                creditCard.getExpiryYear() % 100);

        return CreditCardResponse.builder()
                .cardId(creditCard.getCardId())

                // ACCOUNT
                .accountId(creditCard.getCreditAccount().getAccountId())
                .accountNumber(creditCard.getCreditAccount().getAccountNumber())

                // CUSTOMER
                .customerId(creditCard.getCustomer().getCustomerId())
                .customerName(
                        creditCard.getCustomer().getFirstName() + " " +
                        creditCard.getCustomer().getLastName()
                )

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

    public CreditCardIssuanceResponse toIssueResponse(CreditCard creditCard) {
    	String expiryFormatted = String.format("%02d/%02d",
                creditCard.getExpiryMonth(),
                creditCard.getExpiryYear() % 100);
    	
        return CreditCardIssuanceResponse.builder()
                .cardId(creditCard.getCardId())
                .accountId(creditCard.getCreditAccount().getAccountId())
                .cardStatus(creditCard.getCardStatus()) 
                .customerName(
                        creditCard.getCustomer().getFirstName() + " " +
                        creditCard.getCustomer().getLastName()
                )
                .maskedCardNumber(creditCard.getMaskedCardNumber())
                .networkType(creditCard.getCardProduct().getNetworkType())
                .expiryFormatted(expiryFormatted)
                .build();
    }
}