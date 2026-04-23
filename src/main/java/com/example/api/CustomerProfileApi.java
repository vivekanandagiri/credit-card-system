package com.example.api;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.example.dto.request.CustomerProfileUpdateRequest;
import com.example.dto.response.ApiResponse;
import com.example.dto.response.CustomerProfileResponse;
import com.example.security.CustomUserPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

/**
 * API contract for Customer Profile management.
 *
 * <p>This API allows authenticated customers to:
 * <ul>
 *     <li>View their profile details</li>
 *     <li>Update their profile information</li>
 * </ul>
 *
 * <p>Base URL: <b>/api/v1/customers/profile</b>
 *
 * <p>All endpoints require authentication.
 * Authenticated user is injected via {@link CustomUserPrincipal}.
 *
 * <p>All responses are wrapped in {@link ApiResponse}
 */
@Tag(name = "04. Customer Profile", description = "APIs for managing customer profile")
@RequestMapping("/api/v1/customers/profile")
public interface CustomerProfileApi {

    /**
     * Get logged-in customer profile.
     *
     * <p>Fetches the profile details of the currently authenticated user.
     *
     * @param principal authenticated user
     * @return customer profile details
     */
    @Operation(
            summary = "Get logged-in customer profile",
            description = "Fetch profile details of the currently authenticated customer"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Profile fetched successfully",
                    content = @Content(
                            schema = @Schema(implementation = CustomerProfileResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized"
            )
    })
    @GetMapping
    ResponseEntity<ApiResponse<CustomerProfileResponse>> getProfile(

            @AuthenticationPrincipal
            CustomUserPrincipal principal
    );

    /**
     * Update logged-in customer profile.
     *
     * <p>Allows the authenticated user to update their profile information
     * such as name, contact details, or address.
     *
     * <p>Only provided fields will be updated (partial update behavior).
     *
     * @param principal authenticated user
     * @param request updated profile data
     * @return success message
     */
    @Operation(
            summary = "Update logged-in customer profile",
            description = "Update profile details of the authenticated customer"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Profile updated successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request data"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized"
            )
    })
    @PutMapping
    ResponseEntity<ApiResponse<String>> updateProfile(

            @AuthenticationPrincipal
            CustomUserPrincipal principal,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Updated customer profile data",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = CustomerProfileUpdateRequest.class)
                    )
            )
            @Valid @RequestBody CustomerProfileUpdateRequest request
    );
}