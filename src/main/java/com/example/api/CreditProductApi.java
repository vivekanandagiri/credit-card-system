package com.example.api;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.dto.request.CreditProductCreateRequest;
import com.example.dto.request.CreditProductUpdateRequest;
import com.example.dto.response.ApiResponse;
import com.example.dto.response.CreditProductCreateResponse;
import com.example.dto.response.CreditProductResponse;
import com.example.enums.ProductStatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

@Tag(name = "Credit Products", description = "Admin Credit Product Management APIs")
@RequestMapping("/api/v1/credit-products")
@PreAuthorize("hasRole('ADMIN')")
public interface CreditProductApi {


    // CREATE PRODUCT
    @Operation(summary = "Create new credit product(Admin)")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Product created successfully",
                    content = @Content(
                            schema = @Schema(implementation = CreditProductResponse.class)
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
            @Valid @RequestBody CreditProductCreateRequest request
    );

    // =====================================================
    // GET SPECIFIC PRODUCT
    // =====================================================
    @Operation(summary = "Get product by ID")
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
            @PathVariable Long id
    );

    // GET ALL ACTIVE PRODUCTS
    @Operation(summary = "Get all active credit products")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Products fetched successfully"
            )
    })
    @GetMapping
    ResponseEntity<ApiResponse<List<CreditProductResponse>>> getAll();


    // UPDATE PRODUCT
    @Operation(summary = "Update credit product(Admin)")
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
            @PathVariable Long id,
            @Valid @RequestBody CreditProductUpdateRequest request
    );
    
    
    // Update Status
    @Operation(summary = "Activate or deactivate credit product(Admin)")
    @PatchMapping("/{id}/status")
    ResponseEntity<ApiResponse<String>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestParam ProductStatus status
    );
}

