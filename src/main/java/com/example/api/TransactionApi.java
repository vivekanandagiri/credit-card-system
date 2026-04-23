package com.example.api;

import com.example.dto.request.TransactionRequest;
import com.example.dto.response.ApiResponse;
import com.example.dto.response.TransactionDetailResponse;
import com.example.dto.response.TransactionSummaryResponse;
import com.example.enums.TransactionStatus;
import com.example.enums.TransactionType;
import com.example.security.CustomUserPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * API contract for Transaction operations.
 *
 * <p>This API supports:
 * <ul>
 *     <li>Creating card-based transactions</li>
 *     <li>Fetching transactions with filters and pagination</li>
 *     <li>Retrieving transaction details</li>
 * </ul>
 *
 * <p>Base URL: <b>/api/v1</b>
 *
 * <p>All responses are wrapped in {@link ApiResponse}
 */
@RequestMapping("/api/v1")
@Tag(name = "10. Transaction API", description = "APIs for managing credit card transactions")
public interface TransactionApi {

	/**
	 * Create a new transaction using a credit card.
	 *
	 * <p>This endpoint is used when a card is used for:
	 * <ul>
	 *     <li>Purchases</li>
	 *     <li>Online payments</li>
	 *     <li>POS transactions</li>
	 * </ul>
	 *
	 * @param cardId credit card ID
	 * @param request transaction request payload
	 * @param principal authenticated user
	 * @return transaction summary
	 */
	@Operation(
			summary = "Create transaction (Card based)",
			description = "Creates a transaction using a credit card"
	)
	@ApiResponses(value = {
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "200",
					description = "Transaction created successfully",
					content = @Content(
							schema = @Schema(implementation = TransactionSummaryResponse.class)
					)
			),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "400",
					description = "Invalid transaction request"
			)
	})
	@PostMapping("/cards/{cardId}/transactions")
	ResponseEntity<ApiResponse<TransactionSummaryResponse>> createTransaction(

			@Parameter(
					description = "Credit card ID",
					required = true,
					example = "e7b8c9d0-1234-4a56-b789-abcdef123456"
			)
			@PathVariable UUID cardId,

			@Valid @RequestBody TransactionRequest request,

			@AuthenticationPrincipal
			CustomUserPrincipal principal
	);

	/**
	 * Fetch transactions for an account with filters and pagination.
	 *
	 * <p>Supports filtering by:
	 * <ul>
	 *     <li>Transaction status</li>
	 *     <li>Transaction type</li>
	 *     <li>Specific card ID</li>
	 * </ul>
	 *
	 * @param accountId account ID
	 * @param status optional transaction status filter
	 * @param type optional transaction type filter
	 * @param cardId optional card ID filter
	 * @param principal authenticated user
	 * @param page page number (default = 0)
	 * @param size page size (default = 10)
	 * @return paginated transaction summaries
	 */
	@Operation(
			summary = "Get account transactions with filters",
			description = "Fetch paginated transactions with optional filters (status, type, cardId)"
	)
	@ApiResponses(value = {
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "200",
					description = "Transactions fetched successfully",
					content = @Content(
							schema = @Schema(implementation = TransactionSummaryResponse.class)
					)
			),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "403",
					description = "Access denied"
			)
	})
	@GetMapping("/accounts/{accountId}/transactions")
	ResponseEntity<ApiResponse<Page<TransactionSummaryResponse>>> getAccountTransactions(

			@Parameter(
					description = "Account ID",
					required = true,
					example = "a12b34c5-6789-4def-9012-abcdef345678"
			)
			@PathVariable UUID accountId,

			@Parameter(
					description = "Filter by transaction status",
					example = "SUCCESS"
			)
			@RequestParam(required = false) TransactionStatus status,

			@Parameter(
					description = "Filter by transaction type",
					example = "PURCHASE"
			)
			@RequestParam(required = false) TransactionType type,

			@Parameter(
					description = "Filter by specific card ID",
					example = "e7b8c9d0-1234-4a56-b789-abcdef123456"
			)
			@RequestParam(required = false) UUID cardId,

			@AuthenticationPrincipal
			CustomUserPrincipal principal,

			@Parameter(description = "Page number (default = 0)", example = "0")
			@RequestParam(defaultValue = "0") int page,

			@Parameter(description = "Page size (default = 10)", example = "10")
			@RequestParam(defaultValue = "10") int size
	);

	/**
	 * Fetch transaction details by ID.
	 *
	 * <p>Returns detailed information including:
	 * <ul>
	 *     <li>Amount</li>
	 *     <li>Merchant details</li>
	 *     <li>Status</li>
	 *     <li>Timestamps</li>
	 * </ul>
	 *
	 * @param accountId account ID
	 * @param transactionId transaction ID
	 * @param principal authenticated user
	 * @return transaction details
	 */
	@Operation(
			summary = "Get transaction by ID for account",
			description = "Fetch detailed transaction information by transaction ID"
	)
	@ApiResponses(value = {
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "200",
					description = "Transaction found",
					content = @Content(
							schema = @Schema(implementation = TransactionDetailResponse.class)
					)
			),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "404",
					description = "Transaction not found"
			),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "403",
					description = "Access denied"
			)
	})
	@GetMapping("/accounts/{accountId}/transactions/{transactionId}")
	ResponseEntity<ApiResponse<TransactionDetailResponse>> getAccountTransactionById(

			@Parameter(
					description = "Account ID",
					required = true,
					example = "a12b34c5-6789-4def-9012-abcdef345678"
			)
			@PathVariable UUID accountId,

			@Parameter(
					description = "Transaction ID",
					required = true,
					example = "f98c1234-5678-4abc-9012-fedcba987654"
			)
			@PathVariable UUID transactionId,

			@AuthenticationPrincipal
			CustomUserPrincipal principal
	);
}