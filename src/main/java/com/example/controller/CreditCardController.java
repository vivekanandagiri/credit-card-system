package com.example.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;

import com.example.api.CreditCardApi;
import com.example.dto.request.CreditCardIssuanceRequest;
import com.example.dto.request.CreditCardStatusUpdateRequest;
import com.example.dto.response.ApiResponse;
import com.example.dto.response.CardProductResponse;
import com.example.dto.response.CreditCardIssuanceResponse;
import com.example.dto.response.CreditCardResponse;
import com.example.enums.UserRole;
import com.example.security.CustomUserPrincipal;
import com.example.service.CreditCardService;

import jakarta.validation.Valid;

@RestController
public class CreditCardController implements CreditCardApi {

    private final CreditCardService cardService;

    public CreditCardController(CreditCardService cardService) {
        this.cardService = cardService;
    }

    @Override
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public ResponseEntity<ApiResponse<CreditCardResponse>> issueCard(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            UUID accountId,
            @Valid CreditCardIssuanceRequest request) {

    	CreditCardResponse response =
                (principal.getRole() == UserRole.ADMIN)
                        ? cardService.issueCardByAdmin(accountId,request)
                        : cardService.issueCard(principal.getUserId(),accountId, request);

    	return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                		HttpStatus.CREATED,
                		"Card issued successfully",
                		response));
    }

    @Override
    public ResponseEntity<ApiResponse<List<CreditCardResponse>>> getCardsByAccount(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            UUID accountId) {

        List<CreditCardResponse> result =
                (principal.getRole() == UserRole.ADMIN)
                        ? cardService.getCardsByAccount(accountId)
                        : cardService.getCardsByAccount(principal.getUserId(), accountId);

        return ResponseEntity.ok(
                ApiResponse.success(
                		HttpStatus.OK,
                		"Cards fetched successfully", 
                		result)
        );
    }
    
    @Override
    public ResponseEntity<ApiResponse<CreditCardResponse>> getCardById(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            UUID accountId,
            UUID cardId) {

        CreditCardResponse result =
                (principal.getRole() == UserRole.ADMIN)
                        ? cardService.getCardById(cardId)
                        : cardService.getCardById(principal.getUserId(), accountId, cardId);

        return ResponseEntity.ok(
                ApiResponse.success(
                		HttpStatus.OK,
                		"Card fetched successfully",
                		result)
        );
    }

    @Override
    public ResponseEntity<ApiResponse<CreditCardIssuanceResponse>> updateCardStatus(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            UUID accountId,
            UUID cardId,
            @Valid CreditCardStatusUpdateRequest request) {

        CreditCardIssuanceResponse result =
                cardService.updateCardStatusForUser(principal, accountId, cardId, request);

        return ResponseEntity.ok(
                ApiResponse.success(
                		HttpStatus.OK,
                		"Card status updated successfully",
                		result)
        );
    }

    @Override
    public ResponseEntity<ApiResponse<List<CardProductResponse>>> getCardProducts(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            UUID accountId) {

        List<CardProductResponse> result =
                cardService.getAvailableCardProducts(principal.getUserId(), accountId);

        return ResponseEntity.ok(
                ApiResponse.success(
                		HttpStatus.OK,
                		"Card products fetched successfully", result)
        );
    }

//    @Override
//    @PreAuthorize("hasRole('ADMIN')")
//    public ResponseEntity<ApiResponse<List<CreditCardIssuanceResponse>>> getAllCards(
//            @AuthenticationPrincipal CustomUserPrincipal principal,
//            CardStatus status) {
//
//        if (status != null) {
//            return ResponseEntity.ok(cardService.getCardsByStatus(status));
//        }
//
//        return ResponseEntity.ok(cardService.getAllCards());
//    }

    

//    @Override
//    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
//    public ResponseEntity<ApiResponse<CreditCardIssuanceResponse>> activate(
//            @AuthenticationPrincipal CustomUserPrincipal principal,
//            UUID cardId) {
//
//        return ResponseEntity.ok(
//                cardService.activateCard(principal, cardId));
//    }
//
//    @Override
//    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
//    public ResponseEntity<ApiResponse<CreditCardIssuanceResponse>> block(
//            @AuthenticationPrincipal CustomUserPrincipal principal,
//            UUID cardId,
//            String reason) {
//
//        return ResponseEntity.ok(
//                cardService.blockCard(principal, cardId, reason));
//    }
//
//    @Override
//    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
//    public ResponseEntity<ApiResponse<CreditCardIssuanceResponse>> unblock(
//            @AuthenticationPrincipal CustomUserPrincipal principal,
//            UUID cardId) {
//
//        return ResponseEntity.ok(
//                cardService.unblockCard(principal, cardId));
//    }
//
//    @Override
//    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
//    public ResponseEntity<ApiResponse<CreditCardIssuanceResponse>> cancel(
//            @AuthenticationPrincipal CustomUserPrincipal principal,
//            UUID cardId,
//            String reason) {
//
//        return ResponseEntity.ok(
//                cardService.cancelCard(principal, cardId, reason));
//    }
}