package com.example.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.api.CreditCardApi;
import com.example.dto.request.CreditCardIssuanceRequest;
import com.example.dto.request.CreditCardStatusUpdateRequest;
import com.example.dto.response.ApiResponse;
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
            @PathVariable UUID accountId,
            @Valid @RequestBody CreditCardIssuanceRequest request) {

        CreditCardResponse response =
                UserRole.ADMIN.equals(principal.getRole())
                        ? cardService.issueCardByAdmin(accountId, request)
                        : cardService.issueCard(principal.getUserId(), accountId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED, "Card issued successfully", response));
    }

    @Override
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public ResponseEntity<ApiResponse<List<CreditCardResponse>>> getCardsByAccount(
    		@AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID accountId) {

        List<CreditCardResponse> result =
                UserRole.ADMIN.equals(principal.getRole())
                        ? cardService.getCardsByAccount(accountId)
                        : cardService.getCardsByAccount(principal.getUserId(), accountId);

        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK, "Cards fetched successfully", result)
        );
    }

    @Override
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public ResponseEntity<ApiResponse<CreditCardResponse>> getCardById(
    		@AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID accountId,
            @PathVariable UUID cardId) {

        CreditCardResponse result =
                UserRole.ADMIN.equals(principal.getRole())
                        ? cardService.getCardById(cardId)
                        : cardService.getCardById(principal.getUserId(), accountId, cardId);

        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK, "Card fetched successfully", result)
        );
    }

    @Override
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public ResponseEntity<ApiResponse<CreditCardIssuanceResponse>> updateCardStatus(
    		@AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID accountId,
            @PathVariable UUID cardId,
            @Valid @RequestBody CreditCardStatusUpdateRequest request) {

        CreditCardIssuanceResponse result =
                cardService.updateCardStatusForUser(principal, accountId, cardId, request);

        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK, "Card status updated successfully", result)
        );
    }

    @Override
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public ResponseEntity<ApiResponse<CreditCardResponse>> getCardDetailsById(
    		@AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID cardId) {

        CreditCardResponse response = cardService.getCardById(cardId);

        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK, "Card Detail fetched successfully", response)
        );
    }
}