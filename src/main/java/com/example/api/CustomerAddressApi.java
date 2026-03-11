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
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

@Tag(name = "Customer Address", description = "Customer address management APIs")
@RequestMapping("/api/v1/customers/addresses")
public interface CustomerAddressApi {

    // =========================
    // ADD ADDRESS
    // =========================
    @Operation(summary = "Add new address for logged-in customer")
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
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody AddressCreateRequest request
    );

    // =========================
    // GET ADDRESSES
    // =========================
    @Operation(summary = "Get all addresses of logged-in customer")
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
            @AuthenticationPrincipal CustomUserPrincipal principal
    );

    // =========================
    // DELETE ADDRESS
    // =========================
    @Operation(summary = "Delete address by ID")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Address deleted successfully"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Address not found"
            )
    })
    @DeleteMapping("/{addressId}")
    ResponseEntity<ApiResponse<String>> deleteAddress(
            @PathVariable UUID addressId
    );
}