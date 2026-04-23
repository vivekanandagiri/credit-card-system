package com.example.api;

import com.example.dto.response.ApiResponse;
import com.example.dto.response.BillingStatementResponse;
import com.example.security.CustomUserPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * API contract for Billing Statement operations.
 *
 * <p>This API provides endpoints for:
 * <ul>
 *     <li>Manual generation of billing statements (Admin only)</li>
 *     <li>Fetching billing statements for an account</li>
 * </ul>
 *
 * <p>Base URL: <b>/api/v1/accounts</b>
 *
 * <p>All responses are wrapped in {@link ApiResponse}
 */
@Tag(name = "11. Billing Statement", description = "APIs for managing billing statements")
@RequestMapping("/api/v1/accounts")
public interface BillingStatementApi {

    /**
     * Generate a billing statement manually.
     *
     * <p>This endpoint allows administrators to trigger billing statement generation
     * for a specific account outside the scheduled cycle.
     *
     * <p><b>Security:</b> Admin access required.
     *
     * @param accountId account ID for which the statement will be generated
     * @return generated billing statement details
     */
    @Operation(
            summary = "Generate statement manually (Admin)",
            description = "Triggers manual generation of a billing statement for a given account"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Billing statement generated successfully",
                    content = @Content(
                            schema = @Schema(implementation = BillingStatementResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Account not found"
            )
    })
    @PostMapping("/{accountId}/statements")
    ResponseEntity<ApiResponse<BillingStatementResponse>> generateStatementManually(

            @Parameter(
                    description = "Account ID for which statement is generated",
                    required = true,
                    example = "a12f4c56-7890-4d3b-9abc-1234567890ab"
            )
            @PathVariable UUID accountId
    );

    /**
     * Fetch billing statements for a given account.
     *
     * <p>This endpoint allows both Admins and Customers to retrieve billing statements
     * associated with an account.
     *
     * @param principal authenticated user
     * @param accountId account ID
     * @return list of billing statements
     */
    @Operation(
            summary = "Get Billing statements (Admin, Customer)",
            description = "Fetch all billing statements associated with an account"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Billing statements fetched successfully",
                    content = @Content(
                            schema = @Schema(implementation = BillingStatementResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Access denied"
            )
    })
    @GetMapping("/{accountId}/statements")
    ResponseEntity<ApiResponse<List<BillingStatementResponse>>> getStatements(

            @AuthenticationPrincipal
            CustomUserPrincipal principal,

            @Parameter(
                    description = "Account ID",
                    required = true,
                    example = "a12f4c56-7890-4d3b-9abc-1234567890ab"
            )
            @PathVariable UUID accountId
    );
}