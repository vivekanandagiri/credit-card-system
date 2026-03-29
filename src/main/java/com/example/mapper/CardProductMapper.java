package com.example.mapper;

import com.example.dto.request.CardProductCreateRequest;
import com.example.dto.request.CardProductUpdateRequest;
import com.example.dto.response.CardProductCreateResponse;
import com.example.dto.response.CardProductResponse;
import com.example.entity.CreditCardProduct;
import com.example.enums.ProductStatus;

import java.util.Optional;

import org.springframework.stereotype.Component;

@Component
public class CardProductMapper {

    public CreditCardProduct toEntity(CardProductCreateRequest request) {

        CreditCardProduct card = new CreditCardProduct();

        card.setProductName(request.getProductName());
        card.setNetworkType(request.getNetworkType());
        card.setCardType(request.getCardType());
        card.setAnnualFee(request.getAnnualFee());
        card.setCardValidityYears(request.getCardValidityYears());
        card.setContactlessEnabled(request.getContactlessEnabled());
        card.setInternationalUsageAllowed(request.getInternationalUsageAllowed());
        card.setOnlineTransactionsAllowed(request.getOnlineTransactionsAllowed());
        card.setAtmWithdrawalAllowed(request.getAtmWithdrawalAllowed());
        card.setAtmDailyLimit(request.getAtmDailyLimit());
        card.setPosDailyLimit(request.getPosDailyLimit());
        card.setEcommerceDailyLimit(request.getEcommerceDailyLimit());
        card.setStatementCycleDay(request.getStatementCycleDay());
        card.setForexMarkupPercent(request.getForexMarkupPercent());
        card.setProductDescription(request.getProductDescription());

        // Status and audit set by service
        card.setStatus(ProductStatus.ACTIVE);

        return card;
    }


    public CardProductResponse toResponse(CreditCardProduct card) {

        return new CardProductResponse(
                card.getCardProductId(),
                card.getProductName(),
                card.getNetworkType(),
                card.getCardType(),
                card.getAnnualFee(),
                card.getCardValidityYears(),
                card.getContactlessEnabled(),
                card.getInternationalUsageAllowed(),
                card.getOnlineTransactionsAllowed(),
                card.getAtmWithdrawalAllowed(),
                card.getAtmDailyLimit(),
                card.getPosDailyLimit(),
                card.getEcommerceDailyLimit(),
                card.getStatementCycleDay(),
                card.getForexMarkupPercent(),
                card.getProductDescription(),
                card.getStatus()
        );
    }
    //Create Response mapper
    public CardProductCreateResponse toCardProductSummaryResponse(CreditCardProduct card) {

        return new CardProductCreateResponse(
                card.getCardProductId(),
                card.getProductName(),
                card.getNetworkType(),
                card.getStatus()
        );
    }
    
    //UPDATE MAPPER
    public void updateEntity(CardProductUpdateRequest request, CreditCardProduct cardProduct) {

        Optional.ofNullable(request.getProductName())
                .ifPresent(cardProduct::setProductName);

        Optional.ofNullable(request.getNetworkType())
                .ifPresent(cardProduct::setNetworkType);

        Optional.ofNullable(request.getCardType())
                .ifPresent(cardProduct::setCardType);

        Optional.ofNullable(request.getAnnualFee())
                .ifPresent(cardProduct::setAnnualFee);

        Optional.ofNullable(request.getCardValidityYears())
                .ifPresent(cardProduct::setCardValidityYears);

        Optional.ofNullable(request.getContactlessEnabled())
                .ifPresent(cardProduct::setContactlessEnabled);

        Optional.ofNullable(request.getInternationalUsageAllowed())
                .ifPresent(cardProduct::setInternationalUsageAllowed);

        Optional.ofNullable(request.getOnlineTransactionsAllowed())
                .ifPresent(cardProduct::setOnlineTransactionsAllowed);

        Optional.ofNullable(request.getAtmWithdrawalAllowed())
                .ifPresent(cardProduct::setAtmWithdrawalAllowed);

        Optional.ofNullable(request.getAtmDailyLimit())
                .ifPresent(cardProduct::setAtmDailyLimit);

        Optional.ofNullable(request.getPosDailyLimit())
                .ifPresent(cardProduct::setPosDailyLimit);

        Optional.ofNullable(request.getEcommerceDailyLimit())
                .ifPresent(cardProduct::setEcommerceDailyLimit);

        Optional.ofNullable(request.getStatementCycleDay())
                .ifPresent(cardProduct::setStatementCycleDay);

        Optional.ofNullable(request.getForexMarkupPercent())
                .ifPresent(cardProduct::setForexMarkupPercent);

        Optional.ofNullable(request.getProductDescription())
                .ifPresent(cardProduct::setProductDescription);
    }
}