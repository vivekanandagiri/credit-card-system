package com.example.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.example.dto.request.CreditCardIssuanceRequest;
import com.example.dto.request.CreditCardStatusUpdateRequest;
import com.example.dto.response.ApiResponse;
import com.example.dto.response.CreditCardIssuanceResponse;
import com.example.dto.response.CreditCardResponse;
import com.example.security.CustomUserPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

/**
 * API contract for Credit Card operations.
 *
 * <p>This API provides endpoints for:
 * <ul>
 *     <li>Issuing new credit cards</li>
 *     <li>Fetching cards by account</li>
 *     <li>Retrieving card details</li>
 *     <li>Updating card status (block/unblock, etc.)</li>
 * </ul>
 *
 * <p>Base URL: <b>/api/v1</b>
 *
 * <p>All endpoints require authentication.
 * Authenticated user is injected via {@link CustomUserPrincipal}.
 */
@RequestMapping("/api/v1")
@Tag(name = "9. Credit Card API", description = "APIs for managing credit cards")
public interface CreditCardApi {

    /**
     * Issue a new credit card for a given account.
     *
     * <p>This endpoint creates and assigns a credit card to the specified account.
     *
     * @param principal authenticated user details
     * @param accountId account ID to which the card will be issued
     * @param request   card issuance request payload
     * @return issued credit card details
     */
    @Operation(
            summary = "Issue card",
            description = "Issues a new credit card for a given account"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Card issued successfully",
                    content = @Content(
                            schema = @Schema(implementation = CreditCardResponse.class)
                    )
            )
    })
    @PostMapping("accounts/{accountId}/cards")
    ResponseEntity<ApiResponse<CreditCardResponse>> issueCard(
            @AuthenticationPrincipal
            CustomUserPrincipal principal,

            @Parameter(
                    description = "Account ID for which card is issued",
                    required = true,
                    example = "c1a7c6c2-1234-4d7a-9b91-abc123456789"
            )
            @PathVariable UUID accountId,

            @Valid @RequestBody CreditCardIssuanceRequest request
    );

    /**
     * Get all credit cards associated with an account.
     *
     * @param principal authenticated user
     * @param accountId account ID
     * @return list of credit cards
     */
    @Operation(
            summary = "Get cards by account",
            description = "Fetch all credit cards linked to a specific account"
    )
    @GetMapping("accounts/{accountId}/cards")
    ResponseEntity<ApiResponse<List<CreditCardResponse>>> getCardsByAccount(
            @AuthenticationPrincipal
            CustomUserPrincipal principal,

            @Parameter(
                    description = "Account ID",
                    required = true
            )
            @PathVariable UUID accountId
    );

    /**
     * Fetch a specific credit card by account ID and card ID.
     *
     * @param principal authenticated user
     * @param accountId account ID
     * @param cardId    card ID
     * @return credit card details
     */
    @Operation(
            summary = "Get card by ID",
            description = "Fetch a specific card using account ID and card ID"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Card found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Card not found"
            )
    })
    @GetMapping("accounts/{accountId}/cards/{cardId}")
    ResponseEntity<ApiResponse<CreditCardResponse>> getCardById(
            @AuthenticationPrincipal
            CustomUserPrincipal principal,

            @Parameter(description = "Account ID", required = true)
            @PathVariable UUID accountId,

            @Parameter(description = "Card ID", required = true)
            @PathVariable UUID cardId
    );

    /**
     * Update credit card status.
     *
     * <p>This can be used to:
     * <ul>
     *     <li>Block card</li>
     *     <li>Unblock card</li>
     *     <li>Deactivate card</li>
     * </ul>
     *
     * @param principal authenticated user
     * @param accountId account ID
     * @param cardId    card ID
     * @param request   status update request
     * @return updated card status response
     */
    @Operation(
            summary = "Update card status",
            description = "Update the status of a credit card (BLOCK, UNBLOCK, etc.)"
    )
    @PatchMapping("accounts/{accountId}/cards/{cardId}")
    ResponseEntity<ApiResponse<CreditCardIssuanceResponse>> updateCardStatus(
            @AuthenticationPrincipal
            CustomUserPrincipal principal,

            @Parameter(description = "Account ID", required = true)
            @PathVariable UUID accountId,

            @Parameter(description = "Card ID", required = true)
            @PathVariable UUID cardId,

            @Valid @RequestBody CreditCardStatusUpdateRequest request
    );

    /**
     * Fetch card details using only card ID.
     *
     * <p>This endpoint is useful when account context is not required.
     *
     * @param principal authenticated user
     * @param cardId    card ID
     * @return card details
     */
    @Operation(
            summary = "Get Card details by Id",
            description = "Fetch credit card details using only card ID"
    )
    @GetMapping("/{cardId}")
    ResponseEntity<ApiResponse<CreditCardResponse>> getCardDetailsById(
            @AuthenticationPrincipal
            CustomUserPrincipal principal,

            @Parameter(
                    description = "Unique card ID",
                    required = true,
                    example = "f47ac10b-58cc-4372-a567-0e02b2c3d479"
            )
            @PathVariable UUID cardId
    );
}