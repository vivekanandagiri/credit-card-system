package com.example.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import com.example.api.CardProductApi;
import com.example.dto.request.CardProductCreateRequest;
import com.example.dto.request.CardProductUpdateRequest;
import com.example.dto.response.CardProductCreateResponse;
import com.example.dto.response.ApiResponse;
import com.example.dto.response.CardProductResponse;
import com.example.enums.ProductStatus;
import com.example.service.CardProductService;

@RestController
public class CardProductController implements CardProductApi {

    private final CardProductService cardProductService;

    public CardProductController(CardProductService cardProductService) {
        this.cardProductService = cardProductService;
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CardProductCreateResponse>> create(
            CardProductCreateRequest request) {

        ApiResponse<CardProductCreateResponse> response =
                cardProductService.create(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<ApiResponse<List<CardProductResponse>>> getAll() {
        return ResponseEntity.ok(cardProductService.getAll());
    }

    @Override
    public ResponseEntity<ApiResponse<List<CardProductResponse>>> getAllActive() {
        return ResponseEntity.ok(cardProductService.getAllActive());
    }

    @Override
    public ResponseEntity<ApiResponse<CardProductResponse>> getById(UUID id) {
        return ResponseEntity.ok(cardProductService.getById(id));
    }

    @Override
    public ResponseEntity<ApiResponse<List<CardProductResponse>>> getByCreditProduct(
            Long creditProductId) {

        return ResponseEntity.ok(
                cardProductService.getByCreditProduct(creditProductId)
        );
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CardProductResponse>> update(
            UUID id,
            CardProductUpdateRequest request) {

        return ResponseEntity.ok(
                cardProductService.update(id, request)
        );
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> updateStatus(
            UUID id,
            ProductStatus status) {

        return ResponseEntity.ok(cardProductService.updateStatus(id, status));
    }
}