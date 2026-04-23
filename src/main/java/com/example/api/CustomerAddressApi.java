package com.example.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.example.dto.request.AddressCreateRequest;
import com.example.dto.response.AddressResponse;
import com.example.dto.response.ApiResponse;
import com.example.security.CustomUserPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

/**
 * API contract for Customer Address management.
 *
 * <p>This API allows authenticated customers to:
 * <ul>
 *     <li>Add new addresses</li>
 *     <li>View all saved addresses</li>
 *     <li>Delete an address</li>
 * </ul>
 *
 * <p>Base URL: <b>/api/v1/customers/addresses</b>
 *
 * <p>All endpoints require authentication.
 * Authenticated user is injected via {@link CustomUserPrincipal}.
 *
 * <p>All responses are wrapped in {@link ApiResponse}
 */
@Tag(name = "05. Customer Address", description = "APIs for managing customer addresses")
@RequestMapping("/api/v1/customers/addresses")
public interface CustomerAddressApi {

    /**
     * Add a new address for the logged-in customer.
     *
     * <p>Customers can store multiple addresses such as:
     * <ul>
     *     <li>Home address</li>
     *     <li>Work address</li>
     *     <li>Billing address</li>
     * </ul>
     *
     * @param principal authenticated user
     * @param request address creation request payload
     * @return success message
     */
    @Operation(
            summary = "Add new address for logged-in customer",
            description = "Creates and stores a new address for the authenticated customer"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Address created successfully",
                    content = @Content(
                            schema = @Schema(implementation = String.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized"
            )
    })
    @PostMapping
    ResponseEntity<ApiResponse<String>> addAddress(

            @AuthenticationPrincipal
            CustomUserPrincipal principal,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Address creation request",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = AddressCreateRequest.class)
                    )
            )
            @Valid @RequestBody AddressCreateRequest request
    );

    /**
     * Fetch all addresses of the logged-in customer.
     *
     * @param principal authenticated user
     * @return list of customer addresses
     */
    @Operation(
            summary = "Get all addresses of logged-in customer",
            description = "Fetch all saved addresses for the authenticated user"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Addresses fetched successfully",
                    content = @Content(
                            schema = @Schema(implementation = AddressResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized"
            )
    })
    @GetMapping
    ResponseEntity<ApiResponse<List<AddressResponse>>> getAddresses(

            @AuthenticationPrincipal
            CustomUserPrincipal principal
    );

    /**
     * Delete an address by ID.
     *
     * <p>Customers can delete only their own addresses.
     *
     * @param principal authenticated user
     * @param addressId address ID
     * @return success message
     */
    @Operation(
            summary = "Delete address by ID",
            description = "Deletes a specific address belonging to the authenticated user"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Address deleted successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Address not found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Access denied"
            )
    })
    @DeleteMapping("/{addressId}")
    ResponseEntity<ApiResponse<String>> deleteAddress(

            @AuthenticationPrincipal
            CustomUserPrincipal principal,

            @Parameter(
                    description = "Address ID",
                    required = true,
                    example = "c12a3456-7890-4abc-9def-1234567890ab"
            )
            @PathVariable UUID addressId
    );
}