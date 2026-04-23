package com.example.api;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.dto.request.CreditProductCreateRequest;
import com.example.dto.request.CreditProductUpdateRequest;
import com.example.dto.response.ApiResponse;
import com.example.dto.response.CreditProductCreateResponse;
import com.example.dto.response.CreditProductResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

/**
 * API contract for Credit Product management.
 *
 * <p>This API allows administrators to:
 * <ul>
 *     <li>Create credit products</li>
 *     <li>View product details</li>
 *     <li>List all products</li>
 *     <li>Update product configuration</li>
 * </ul>
 *
 * <p>Base URL: <b>/api/v1/credit-products</b>
 *
 * <p><b>Security:</b> All endpoints require ADMIN role.
 *
 * <p>All responses are wrapped in {@link ApiResponse}
 */
@Tag(name = "2. Credit Products", description = "APIs for managing credit products")
@RequestMapping("/api/v1/credit-products")
public interface CreditProductApi {

	/**
	 * Create a new credit product.
	 *
	 * <p>Examples:
	 * <ul>
	 *     <li>Personal Loan</li>
	 *     <li>Home Loan</li>
	 *     <li>Auto Loan</li>
	 * </ul>
	 *
	 * @param request credit product creation request
	 * @return created product response
	 */
	@Operation(
			summary = "Create new credit product",
			description = """
                Creates a new credit product.

                🔒 Requires ADMIN role.

                Example:
                - Personal Loan
                - Interest Rate: 10%-18%
                - Status: ACTIVE
                """
	)
	@ApiResponses(value = {
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "201",
					description = "Product created successfully",
					content = @Content(
							schema = @Schema(implementation = CreditProductCreateResponse.class)
					)
			),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "400",
					description = "Invalid request"
			),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "403",
					description = "Access denied"
			)
	})
	@PostMapping
	ResponseEntity<ApiResponse<CreditProductCreateResponse>> create(

			@io.swagger.v3.oas.annotations.parameters.RequestBody(
					description = "Credit product creation request",
					required = true,
					content = @Content(
							schema = @Schema(implementation = CreditProductCreateRequest.class)
					)
			)
			@Valid @RequestBody CreditProductCreateRequest request
	);

	/**
	 * Fetch a credit product by ID.
	 *
	 * @param id credit product ID
	 * @return credit product details
	 */
	@Operation(
			summary = "Get credit product by ID",
			description = "Fetch a specific credit product using its unique identifier"
	)
	@ApiResponses(value = {
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "200",
					description = "Product fetched successfully"
			),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "404",
					description = "Product not found"
			)
	})
	@GetMapping("/{id}")
	ResponseEntity<ApiResponse<CreditProductResponse>> getById(

			@Parameter(
					description = "Unique ID of the credit product",
					example = "101",
					required = true
			)
			@PathVariable Long id
	);

	/**
	 * Fetch all credit products.
	 *
	 * <p>By default, active products are returned.
	 *
	 * @return list of credit products
	 */
	@Operation(
			summary = "Get all credit products",
			description = "Fetch all available credit products (default: ACTIVE)"
	)
	@ApiResponses(value = {
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "200",
					description = "Products fetched successfully"
			)
	})
	@GetMapping
	ResponseEntity<ApiResponse<List<CreditProductResponse>>> getAll();

	/**
	 * Update an existing credit product.
	 *
	 * <p>Supports partial updates.
	 *
	 * <p>Example:
	 * <pre>
	 * {
	 *   "status": "INACTIVE"
	 * }
	 * </pre>
	 *
	 * @param id credit product ID
	 * @param request update request payload
	 * @return updated product details
	 */
	@Operation(
			summary = "Update credit product",
			description = """
                Updates an existing credit product.

                ✔ Partial updates supported
                ✔ Example: only status update

                {
                  "status": "INACTIVE"
                }

                🔒 Requires ADMIN role.
                """
	)
	@ApiResponses(value = {
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "200",
					description = "Product updated successfully"
			),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(
					responseCode = "404",
					description = "Product not found"
			)
	})
	@PutMapping("/{id}")
	ResponseEntity<ApiResponse<CreditProductResponse>> update(

			@Parameter(
					description = "Credit product ID",
					example = "1",
					required = true
			)
			@PathVariable Long id,

			@io.swagger.v3.oas.annotations.parameters.RequestBody(
					description = "Updated credit product details",
					required = true,
					content = @Content(
							schema = @Schema(implementation = CreditProductUpdateRequest.class)
					)
			)
			@Valid @RequestBody CreditProductUpdateRequest request
	);
}