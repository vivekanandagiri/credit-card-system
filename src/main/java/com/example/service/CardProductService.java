package com.example.service;

import java.util.List;
import java.util.UUID;

import com.example.dto.request.CardProductCreateRequest;
import com.example.dto.request.CardProductUpdateRequest;
import com.example.dto.response.ApiResponse;
import com.example.dto.response.CardProductCreateResponse;
import com.example.dto.response.CardProductResponse;
import com.example.enums.ProductStatus;

public interface CardProductService {

    ApiResponse<CardProductCreateResponse> create(CardProductCreateRequest request);

    ApiResponse<CardProductResponse> getById(UUID id);

    ApiResponse<List<CardProductResponse>> getAll();

    ApiResponse<List<CardProductResponse>> getAllActive();

    ApiResponse<List<CardProductResponse>> getByCreditProduct(Long creditProductId);

    ApiResponse<CardProductResponse> update(UUID id, CardProductUpdateRequest request);

    ApiResponse<String> updateStatus(UUID id,ProductStatus status);
}