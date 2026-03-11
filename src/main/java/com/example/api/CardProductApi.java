package com.example.api;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.dto.request.CardProductCreateRequest;
import com.example.dto.request.CardProductUpdateRequest;
import com.example.dto.response.ApiResponse;
import com.example.dto.response.CardProductResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

@Tag(name = "Card Products", description = "Card product management APIs")
@RequestMapping("/api/v1/card-products")
public interface CardProductApi {

    // =====================================================
    // CREATE PRODUCT
    // =====================================================
    @Operation(summary = "Create new card product")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Card product created",
                    content = @Content(
                            schema = @Schema(implementation = CardProductResponse.class)
                    )
            )
    })
    @PostMapping
    ResponseEntity<ApiResponse<CardProductResponse>> create(
            @Valid @RequestBody CardProductCreateRequest request
    );

    // =====================================================
    // GET ALL
    // =====================================================
    @Operation(summary = "Get all card products")
    @GetMapping
    ResponseEntity<ApiResponse<List<CardProductResponse>>> getAll();

    // =====================================================
    // GET ALL ACTIVE
    // =====================================================
    @Operation(summary = "Get all active card products")
    @GetMapping("/active")
    ResponseEntity<ApiResponse<List<CardProductResponse>>> getAllActive();

    // =====================================================
    // GET BY ID
    // =====================================================
    @Operation(summary = "Get card product by ID")
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
            @PathVariable UUID id
    );

    // =====================================================
    // GET BY CREDIT PRODUCT
    // =====================================================
    @Operation(summary = "Get card products by credit product ID")
    @GetMapping("/credit-product/{creditProductId}")
    ResponseEntity<ApiResponse<List<CardProductResponse>>> getByCreditProduct(
            @PathVariable Long creditProductId
    );

    // =====================================================
    // UPDATE
    // =====================================================
    @Operation(summary = "Update card product")
    @PutMapping("/{id}")
    ResponseEntity<ApiResponse<CardProductResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody CardProductUpdateRequest request
    );

    // =====================================================
    // DEACTIVATE
    // =====================================================
    @Operation(summary = "Deactivate card product")
    @PatchMapping("/{id}/deactivate")
    ResponseEntity<ApiResponse<String>> deactivate(
            @PathVariable UUID id
    );
}