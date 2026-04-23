package com.example.api;

import com.example.dto.request.CreditAccountStatusUpdateRequest;
import com.example.dto.response.CreditAccountResponse;
import com.example.enums.AccountStatus;
import com.example.dto.response.ApiResponse;
import com.example.security.CustomUserPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * API contract for Credit Account operations.
 *
 * <p>This API supports:
 * <ul>
 *     <li>Fetching credit accounts (Admin: all, Customer: own)</li>
 *     <li>Fetching account details by ID</li>
 *     <li>Updating account status (Admin only)</li>
 * </ul>
 *
 * <p>Base URL: <b>/api/v1/accounts</b>
 *
 * <p>All responses are wrapped in {@link ApiResponse}
 */
@Tag(name = "08. Credit Account Management", description = "APIs for managing credit accounts")
@RequestMapping("/api/v1/accounts")
public interface CreditAccountApi {

    /**
     * Fetch credit accounts.
     *
     * <p>Behavior:
     * <ul>
     *     <li><b>Customer:</b> Gets only their own accounts</li>
     *     <li><b>Admin:</b> Gets all accounts</li>
     * </ul>
     *
     * <p>Optional filtering by account status.
     *
     * @param principal authenticated user
     * @param status optional account status filter (ACTIVE, INACTIVE, BLOCKED, etc.)
     * @return list of credit accounts
     */
    @Operation(
            summary = "Get accounts (Customer: own, Admin: all)",
            description = "Fetch credit accounts with optional status filtering"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Accounts fetched successfully",
                    content = @Content(
                            schema = @Schema(implementation = CreditAccountResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Access denied"
            )
    })
    @GetMapping
    ResponseEntity<ApiResponse<List<CreditAccountResponse>>> getAccounts(

            @AuthenticationPrincipal
            CustomUserPrincipal principal,

            @Parameter(
                    description = "Filter accounts by status (optional)",
                    example = "ACTIVE"
            )
            @RequestParam(required = false) AccountStatus status
    );

    /**
     * Fetch a credit account by its ID.
     *
     * <p>Customers can only access their own accounts.
     * Admins can access any account.
     *
     * @param principal authenticated user
     * @param accountId account ID
     * @return credit account details
     */
    @Operation(
            summary = "Get account by ID",
            description = "Fetch a specific credit account using its ID"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Account found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Account not found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Access denied"
            )
    })
    @GetMapping("/{accountId}")
    ResponseEntity<ApiResponse<CreditAccountResponse>> getAccountById(

            @AuthenticationPrincipal
            CustomUserPrincipal principal,

            @Parameter(
                    description = "Unique account ID",
                    required = true,
                    example = "9f47ac10-b123-4372-a567-0e02b2c3d479"
            )
            @PathVariable UUID accountId
    );

    /**
     * Update credit account status.
     *
     * <p>This endpoint allows administrators to update account status such as:
     * <ul>
     *     <li>ACTIVATE</li>
     *     <li>BLOCK</li>
     *     <li>CLOSE</li>
     * </ul>
     *
     * <p><b>Security:</b> Admin only.
     *
     * @param accountId account ID
     * @param request status update payload
     * @return updated account details
     */
    @Operation(
            summary = "Update account status (Admin)",
            description = "Update credit account status such as ACTIVE, BLOCKED, CLOSED"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Account status updated successfully",
                    content = @Content(
                            schema = @Schema(implementation = CreditAccountResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Account not found"
            )
    })
    @PatchMapping("/{accountId}")
    ResponseEntity<ApiResponse<CreditAccountResponse>> updateAccountStatus(

            @Parameter(
                    description = "Account ID",
                    required = true,
                    example = "9f47ac10-b123-4372-a567-0e02b2c3d479"
            )
            @PathVariable UUID accountId,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Account status update request",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = CreditAccountStatusUpdateRequest.class)
                    )
            )
            @Valid @RequestBody CreditAccountStatusUpdateRequest request
    );
}