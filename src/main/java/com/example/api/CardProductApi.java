package com.example.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.dto.request.CardProductCreateRequest;
import com.example.dto.request.CardProductUpdateRequest;
import com.example.dto.response.ApiResponse;
import com.example.dto.response.CardProductCreateResponse;
import com.example.dto.response.CardProductResponse;
import com.example.enums.ProductStatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

/**
 * API contract for managing Card Products.
 *
 * <p>This interface defines endpoints for:
 * <ul>
 *     <li>Creating card products</li>
 *     <li>Fetching all or specific products</li>
 *     <li>Updating product details</li>
 * </ul>
 *
 * <p>Base URL: <b>/api/v1/card-products</b>
 *
 * <p>All responses are wrapped in {@link ApiResponse}
 */
@Tag(name = "Card Products", description = "APIs for managing credit card products")
@RequestMapping("/api/v1/card-products")
public interface CardProductApi {

	/**
	 * Create a new card product.
	 *
	 * <p>This endpoint allows administrators to create a new credit card product
	 * with predefined limits and configuration.
	 *
	 * <p><b>Security:</b> Requires ADMIN role.
	 *
	 * @param request request payload containing product details
	 * @return created card product response wrapped in ApiResponse
	 */
	@Operation(
			summary = "Create a new card product",
			description = """
                Creates a new credit card product.

                🔒 Requires ADMIN role.

                Example:
                - Product Name: Platinum Card
                - Limit Range: 50,000 - 5,00,000
                - Status: ACTIVE
                """
	)
	@ApiResponses(value = {
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "201",
					description = "Card product created successfully",
					content = @Content(
							mediaType = "application/json",
							schema = @Schema(implementation = CardProductCreateResponse.class)
					)
			)
	})
	@PostMapping
	ResponseEntity<ApiResponse<CardProductCreateResponse>> create(
			@Valid @RequestBody CardProductCreateRequest request
	);

	/**
	 * Fetch all card products.
	 *
	 * <p>Optionally filter products by their status (ACTIVE / INACTIVE).
	 *
	 * @param status optional filter for product status
	 * @return list of card products
	 */
	@Operation(
			summary = "Get all card products",
			description = """
                Fetch all available card products.

                Optional filter:
                - status = ACTIVE / INACTIVE
                """
	)
	@GetMapping
	ResponseEntity<ApiResponse<List<CardProductResponse>>> getAll(
			@Parameter(
					description = "Filter by product status (optional)",
					example = "ACTIVE"
			)
			@RequestParam(required = false) ProductStatus status
	);

	/**
	 * Fetch a card product by its unique identifier.
	 *
	 * @param id UUID of the card product
	 * @return card product details
	 */
	@Operation(
			summary = "Get card product by ID",
			description = "Fetch a specific card product using its unique ID"
	)
	@ApiResponses(value = {
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "200",
					description = "Card product fetched"
			),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "404",
					description = "Card product not found"
			)
	})
	@GetMapping("/{id}")
	ResponseEntity<ApiResponse<CardProductResponse>> getById(
			@Parameter(
					description = "Unique identifier of the card product",
					required = true,
					example = "3fa85f64-5717-4562-b3fc-2c963f66afa6"
			)
			@PathVariable UUID id
	);

	/**
	 * Update an existing card product.
	 *
	 * <p>This endpoint supports partial updates. Only the provided fields
	 * will be updated.
	 *
	 * <p><b>Example:</b>
	 * <pre>
	 * {
	 *   "status": "INACTIVE"
	 * }
	 * </pre>
	 *
	 * <p><b>Security:</b> Requires ADMIN role.
	 *
	 * @param id      UUID of the card product
	 * @param request fields to update
	 * @return updated card product response
	 */
	@Operation(
			summary = "Update card product",
			description = """
                Updates an existing card product.

                ✔ You can send partial fields
                ✔ Example: only status update

                {
                  "status": "INACTIVE"
                }

                🔒 Requires ADMIN role.
                """
	)
	@PutMapping("/{id}")
	ResponseEntity<ApiResponse<CardProductResponse>> update(
			@Parameter(
					description = "Card product ID",
					required = true,
					example = "3fa85f64-5717-4562-b3fc-2c963f66afa6"
			)
			@PathVariable UUID id,

			@io.swagger.v3.oas.annotations.parameters.RequestBody(
					description = "Updated card product details",
					required = true,
					content = @Content(
							schema = @Schema(implementation = CardProductUpdateRequest.class)
					)
			)
			@Valid @RequestBody CardProductUpdateRequest request
	);
}