package com.example.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import com.example.api.CreditProductApi;
import com.example.dto.request.CreditProductCreateRequest;
import com.example.dto.request.CreditProductUpdateRequest;
import com.example.dto.response.ApiResponse;
import com.example.dto.response.CreditProductResponse;
import com.example.service.CreditProductService;

@RestController
public class CreditProductController implements CreditProductApi {

    private final CreditProductService creditProductService;

    public CreditProductController(CreditProductService creditProductService) {
        this.creditProductService = creditProductService;
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CreditProductResponse>> create(
            CreditProductCreateRequest request) {

        ApiResponse<CreditProductResponse> response =
                creditProductService.create(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<ApiResponse<CreditProductResponse>> getById(Long id) {

        ApiResponse<CreditProductResponse> response =
                creditProductService.getById(id);

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ApiResponse<List<CreditProductResponse>>> getAll() {

        ApiResponse<List<CreditProductResponse>> response =
                creditProductService.getAll();

        return ResponseEntity.ok(response);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CreditProductResponse>> update(
            Long id,
            CreditProductUpdateRequest request) {

        ApiResponse<CreditProductResponse> response =
                creditProductService.update(id, request);

        return ResponseEntity.ok(response);
    }
}