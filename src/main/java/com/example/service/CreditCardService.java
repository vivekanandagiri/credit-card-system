package com.example.service;


import com.example.dto.request.CreditCardIssuanceRequest;
import com.example.dto.request.CreditCardStatusUpdateRequest;
import com.example.dto.response.ApiResponse;
import com.example.dto.response.CardProductResponse;
import com.example.dto.response.CreditCardIssuanceResponse;
import com.example.dto.response.CreditCardResponse;
import com.example.enums.CardStatus;
import com.example.security.CustomUserPrincipal;

import java.util.List;
import java.util.UUID;

public interface CreditCardService {

    // ── Customer ──

    // Browse card products available under their account's credit product
	ApiResponse<List<CardProductResponse>> getAvailableCardProducts(UUID userId, UUID accountId);

    // Issue a card (customer self-serve)
    ApiResponse<CreditCardResponse> issueCard(UUID userId, CreditCardIssuanceRequest request);

    // View my cards (all accounts)
    ApiResponse<List<CreditCardIssuanceResponse>> getMyCards(UUID userId);

    // View cards for a specific account
    ApiResponse<List<CreditCardResponse>> getMyCardsByAccount(UUID userId, UUID accountId);

    // View a specific card
    ApiResponse<CreditCardResponse> getMyCardById(UUID userId, UUID cardId);


    // ── Admin ──

    // Issue a card on behalf of customer
    ApiResponse<CreditCardResponse> issueCardByAdmin(CreditCardIssuanceRequest request);

    // View all cards
    ApiResponse<List<CreditCardIssuanceResponse>> getAllCards();

    // View cards by status
    ApiResponse<List<CreditCardIssuanceResponse>> getCardsByStatus(CardStatus status);

    // View a specific card by ID
    ApiResponse<CreditCardResponse> getCardById(UUID cardId);
    
    //View cards By account 
    public ApiResponse<List<CreditCardResponse>> getCardsByAccount(UUID accountId);

 //  SHARED APIs
    //update status of user
    public ApiResponse<CreditCardIssuanceResponse> updateCardStatusForUser(
	        CustomUserPrincipal principal,
	        UUID cardId,
	        CreditCardStatusUpdateRequest request);

    //View Cards by status
    public ApiResponse<List<CreditCardIssuanceResponse>> getCardsByStatusForUser(
	        CustomUserPrincipal principal,
	        CardStatus status);

	ApiResponse<CreditCardIssuanceResponse> activateCard(CustomUserPrincipal principal, UUID cardId);

	ApiResponse<CreditCardIssuanceResponse> blockCard(CustomUserPrincipal principal, UUID cardId, String reason);

	ApiResponse<CreditCardIssuanceResponse> unblockCard(CustomUserPrincipal principal, UUID cardId);

	ApiResponse<CreditCardIssuanceResponse> cancelCard(CustomUserPrincipal principal, UUID cardId, String reason);
}