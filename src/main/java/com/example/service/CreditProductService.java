package com.example.service;

import com.example.dto.request.CreditProductCreateRequest;
import com.example.dto.request.CreditProductUpdateRequest;
import com.example.dto.response.ApiResponse;
import com.example.dto.response.CreditProductCreateResponse;
import com.example.dto.response.CreditProductResponse;
import com.example.enums.ProductStatus;

import java.util.List;

public interface CreditProductService {

    ApiResponse<CreditProductCreateResponse> create(CreditProductCreateRequest request);

    ApiResponse<CreditProductResponse> getById(Long id);

    ApiResponse<List<CreditProductResponse>> getAll();

    ApiResponse<List<CreditProductResponse>> getAllActive();

    ApiResponse<CreditProductResponse> update(Long id, CreditProductUpdateRequest request);

    ApiResponse<String> updateStatus(Long id,ProductStatus status);
}