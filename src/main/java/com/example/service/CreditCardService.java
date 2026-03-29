package com.example.service;


import com.example.dto.request.CreditCardIssuanceRequest;
import com.example.dto.request.CreditCardStatusUpdateRequest;
import com.example.dto.response.CardProductResponse;
import com.example.dto.response.CreditCardIssuanceResponse;
import com.example.dto.response.CreditCardResponse;
import com.example.entity.CreditCard;
import com.example.enums.CardStatus;
import com.example.security.CustomUserPrincipal;

import java.util.List;
import java.util.UUID;

public interface CreditCardService {

    // ── Customer ──

    // Browse card products available under their account's credit product
    List<CardProductResponse> getAvailableCardProducts(UUID userId, UUID accountId);

    // Issue a card (customer self-serve)
    CreditCardResponse issueCard(UUID userId, UUID accountId, CreditCardIssuanceRequest request);

    // Issue a card on behalf of customer
    CreditCardResponse issueCardByAdmin(UUID accountId, CreditCardIssuanceRequest request);
    
    // View cards for a specific account
    List<CreditCardResponse> getCardsByAccount(UUID userId, UUID accountId);
    //View cards By account 
    List<CreditCardResponse> getCardsByAccount(UUID accountId);
    // View a specific card
    CreditCardResponse getCardById(UUID userId, UUID accountId, UUID cardId);
    //View a specific card
    CreditCardResponse getCardById(UUID cardId);
    /*
     * Admin
     * View all cards
     * Not Reuired
     * ApiResponse<List<CreditCardIssuanceResponse>> getAllCards();
     */

    // View cards by status
    List<CreditCardIssuanceResponse> getCardsByStatus(CardStatus status);
    
 //  SHARED APIs
    //update status of user
    CreditCardIssuanceResponse updateCardStatusForUser(
            CustomUserPrincipal principal,
            UUID accountId,
            UUID cardId,
            CreditCardStatusUpdateRequest request
    );

    //View Cards by status
    List<CreditCardIssuanceResponse> getCardsByStatusForUser(CustomUserPrincipal principal, CardStatus status);


//	ApiResponse<CreditCardIssuanceResponse> activateCard(CustomUserPrincipal principal, UUID cardId);
//
//	ApiResponse<CreditCardIssuanceResponse> blockCard(CustomUserPrincipal principal, UUID cardId, String reason);
//
//	ApiResponse<CreditCardIssuanceResponse> unblockCard(CustomUserPrincipal principal, UUID cardId);
//
//	ApiResponse<CreditCardIssuanceResponse> cancelCard(CustomUserPrincipal principal, UUID cardId, String reason);

    /**
     * Returns the raw {@link CreditCard} entity for internal service-to-service use.
     * Used by {@link TransactionService} to resolve the card without touching the card repository.
     *
     * @throws com.example.exception.ResourceNotFoundException if no card with this ID exists
     */
	CreditCard getCardEntity(UUID cardId);
}