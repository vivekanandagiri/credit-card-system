package com.example.api;

import com.example.dto.request.PaymentRequest;
import com.example.dto.response.ApiResponse;
import com.example.dto.response.PaymentResponse;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

/**
 * API contract for Credit Card Bill Payments.
 *
 * <p>This API supports:
 * <ul>
 *     <li>Making credit card bill payments</li>
 *     <li>Fetching payment history</li>
 *     <li>Retrieving payment details</li>
 * </ul>
 *
 * <p>Base URL: <b>/api/v1/accounts/{accountId}/payments</b>
 *
 * <p><b>Note:</b> These are public APIs — authentication may not be required
 * depending on system design (e.g., third-party payment gateways).
 */
@Tag(name = "12. Bill Payment", description = "Public APIs for credit card bill payments")
@RequestMapping("/api/v1/accounts/{accountId}/payments")
public interface PaymentApi {

	/**
	 * Make a credit card bill payment.
	 *
	 * <p>This endpoint allows any user (authenticated)
	 * to pay a credit card bill.
	 *
	 * <p>Supported payment types:
	 * <ul>
	 *     <li>Full payment</li>
	 *     <li>Partial payment</li>
	 *     <li>Overpayment</li>
	 * </ul>
	 *
	 * @param accountId credit account ID
	 * @param request payment request payload
	 * @return payment response
	 */
	@Operation(
			summary = "Make bill payment",
			description = "Allows any user to pay a credit card bill. Supports partial, full, and overpayments."
	)
	@ApiResponses(value = {
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "200",
					description = "Payment processed successfully",
					content = @Content(
							schema = @Schema(implementation = PaymentResponse.class)
					)
			),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "400",
					description = "Invalid payment request"
			)
	})
	@PostMapping
	public ResponseEntity<ApiResponse<PaymentResponse>> makePayment(

			@Parameter(
					description = "Credit account ID",
					required = true,
					example = "a1b2c3d4-5678-4abc-9012-abcdef123456"
			)
			@PathVariable UUID accountId,

			@Valid @RequestBody PaymentRequest request
	);

	/**
	 * Fetch payment history for a credit account.
	 *
	 * <p>Supports pagination for large datasets.
	 *
	 * @param accountId credit account ID
	 * @param page page number (default = 0)
	 * @param size page size (default = 10)
	 * @return paginated list of payments
	 */
	@Operation(
			summary = "Get bill payments",
			description = "Fetch all payment transactions for a credit account"
	)
	@ApiResponses(value = {
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "200",
					description = "Payments fetched successfully",
					content = @Content(
							schema = @Schema(implementation = PaymentResponse.class)
					)
			)
	})
	@GetMapping
	ResponseEntity<Page<PaymentResponse>> getPayments(

			@Parameter(
					description = "Credit account ID",
					required = true
			)
			@PathVariable UUID accountId,

			@Parameter(description = "Page number (default = 0)", example = "0")
			@RequestParam(defaultValue = "0") int page,

			@Parameter(description = "Page size (default = 10)", example = "10")
			@RequestParam(defaultValue = "10") int size
	);

	/**
	 * Fetch a specific payment by ID.
	 *
	 * @param accountId credit account ID
	 * @param paymentId payment ID
	 * @return payment details
	 */
	@Operation(
			summary = "Get a particular bill payment",
			description = "Fetch details of a specific payment using payment ID"
	)
	@ApiResponses(value = {
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "200",
					description = "Payment found",
					content = @Content(
							schema = @Schema(implementation = PaymentResponse.class)
					)
			),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "404",
					description = "Payment not found"
			)
	})
	@GetMapping("/{paymentId}")
	ResponseEntity<PaymentResponse> getPaymentById(

			@Parameter(description = "Credit account ID", required = true)
			@PathVariable UUID accountId,

			@Parameter(
					description = "Payment ID",
					required = true,
					example = "b7c8d9e0-1234-4f56-8901-abcdef654321"
			)
			@PathVariable UUID paymentId
	);
}