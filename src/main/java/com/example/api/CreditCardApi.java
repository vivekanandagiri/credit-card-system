package com.example.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.example.dto.request.CreditCardIssuanceRequest;
import com.example.dto.request.CreditCardStatusUpdateRequest;
import com.example.dto.response.ApiResponse;
import com.example.dto.response.CardProductResponse;
import com.example.dto.response.CreditCardIssuanceResponse;
import com.example.dto.response.CreditCardResponse;
import com.example.security.CustomUserPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

@RequestMapping("/api/v1/accounts/{accountId}")
@Tag(name = "Credit Card API")
public interface CreditCardApi {

    // ================= CUSTOMER =================

    @Operation(summary = "Issue card")
    @PostMapping("/cards")
    ResponseEntity<ApiResponse<CreditCardResponse>> issueCard(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID accountId,
            @Valid @RequestBody CreditCardIssuanceRequest request);

    @Operation(summary = "Get cards by account")
    @GetMapping("/cards")
    ResponseEntity<ApiResponse<List<CreditCardResponse>>> getCardsByAccount(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID accountId);

    @Operation(summary = "Get card by ID")
    @GetMapping("/cards/{cardId}")
    ResponseEntity<ApiResponse<CreditCardResponse>> getCardById(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID accountId,
            @PathVariable UUID cardId);

    @Operation(summary = "Update card status")
    @PatchMapping("/cards/{cardId}")
    ResponseEntity<ApiResponse<CreditCardIssuanceResponse>> updateCardStatus(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID accountId,
            @PathVariable UUID cardId,
            @Valid @RequestBody CreditCardStatusUpdateRequest request);

    // ================= Available CARD PRODUCTS =================

    @Operation(summary = "Get card products")
    @GetMapping("/card-products")
    ResponseEntity<ApiResponse<List<CardProductResponse>>> getCardProducts(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID accountId);

    // ================= ADMIN =================

//    @Operation(summary = "Get all cards (Admin)")
//    @GetMapping("/cards")
//    ResponseEntity<ApiResponse<List<CreditCardIssuanceResponse>>> getAllCards(
//            @AuthenticationPrincipal CustomUserPrincipal principal,
//            @RequestParam(required = false) CardStatus status);
}