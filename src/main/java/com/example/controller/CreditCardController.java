package com.example.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.example.dto.request.CreditCardIssuanceRequest;
import com.example.dto.request.CreditCardStatusUpdateRequest;
import com.example.dto.response.ApiResponse;
import com.example.dto.response.CardProductResponse;
import com.example.dto.response.CreditCardIssuanceResponse;
import com.example.dto.response.CreditCardResponse;
import com.example.enums.CardStatus;
import com.example.enums.UserRole;
import com.example.security.CustomUserPrincipal;
import com.example.service.CreditCardService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/cards")
public class CreditCardController {

    private final CreditCardService cardService;

    public CreditCardController(CreditCardService cardService) {
        this.cardService = cardService;
    }

    // GET CARDS (Customer → own, Admin → all)
    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public ResponseEntity<ApiResponse<List<CreditCardIssuanceResponse>>> getCards(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestParam(required = false) CardStatus status) {

    	if (status != null) {
            return ResponseEntity.ok(
                    cardService.getCardsByStatusForUser(principal, status)
            );
        }

        if (principal.getRole() == UserRole.ADMIN) {
            return ResponseEntity.ok(cardService.getAllCards());
        }

        return ResponseEntity.ok(cardService.getMyCards(principal.getUserId()));
    }

    // CREATE CARD (Customer / Admin)
    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public ResponseEntity<ApiResponse<CreditCardResponse>> issueCard(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody CreditCardIssuanceRequest request) {

        ApiResponse<CreditCardResponse> response;

        if (principal.getRole().name().equals("ADMIN")) {
            response = cardService.issueCardByAdmin(request);
        } else {
            response = cardService.issueCard(principal.getUserId(), request);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // GET CARD BY ID (secured in service)
    @GetMapping("/{cardId}")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public ResponseEntity<ApiResponse<CreditCardResponse>> getCardById(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID cardId) {

        // Service should internally handle access
        if (principal.getRole().name().equals("ADMIN")) {
            return ResponseEntity.ok(cardService.getCardById(cardId));
        }

        return ResponseEntity.ok(
                cardService.getMyCardById(principal.getUserId(), cardId));
    }

    // UPDATE CARD STATUS (Unified)
    @PatchMapping("/{cardId}")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public ResponseEntity<ApiResponse<CreditCardIssuanceResponse>> updateCardStatus(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID cardId,
            @Valid @RequestBody CreditCardStatusUpdateRequest request) {

        return ResponseEntity.ok(
                cardService.updateCardStatusForUser(principal, cardId, request));
    }
    
    @GetMapping("/{accountId}/card-products")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<List<CardProductResponse>>> getCardProducts(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID accountId) {

        return ResponseEntity.ok(
                cardService.getAvailableCardProducts(principal.getUserId(), accountId)
        );
    }
    
    @GetMapping("/{accountId}/cards")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public ResponseEntity<ApiResponse<List<CreditCardResponse>>> getCardsByAccount(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID accountId) {

        // ADMIN → can access any account
        if (principal.getRole() == UserRole.ADMIN) {
            return ResponseEntity.ok(
                    cardService.getCardsByAccount(accountId) // ❌ MISSING
            );
        }

        // CUSTOMER → only own account
        return ResponseEntity.ok(
                cardService.getMyCardsByAccount(principal.getUserId(), accountId)
        );
    }
    @PostMapping("/{cardId}/activate")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public ResponseEntity<ApiResponse<CreditCardIssuanceResponse>> activate(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID cardId) {

        return ResponseEntity.ok(cardService.activateCard(principal, cardId));
    }
    
    @PostMapping("/{cardId}/block")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public ResponseEntity<ApiResponse<CreditCardIssuanceResponse>> block(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID cardId,
            @RequestParam(required = false) String reason) {

        return ResponseEntity.ok(cardService.blockCard(principal, cardId, reason));
    }
    @PostMapping("/{cardId}/unblock")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public ResponseEntity<ApiResponse<CreditCardIssuanceResponse>> unblock(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID cardId) {

        return ResponseEntity.ok(cardService.unblockCard(principal, cardId));
    }
    
    @PostMapping("/{cardId}/cancel")
    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public ResponseEntity<ApiResponse<CreditCardIssuanceResponse>> cancel(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID cardId,
            @RequestParam(required = false) String reason) {

        return ResponseEntity.ok(cardService.cancelCard(principal, cardId, reason));
    }
}