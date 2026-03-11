package com.example.service;

import com.example.dto.request.CreditProductCreateRequest;
import com.example.dto.request.CreditProductUpdateRequest;
import com.example.dto.response.ApiResponse;
import com.example.dto.response.CreditProductResponse;

import java.util.List;

public interface CreditProductService {

    ApiResponse<CreditProductResponse> create(CreditProductCreateRequest request);

    ApiResponse<CreditProductResponse> getById(Long id);

    ApiResponse<List<CreditProductResponse>> getAll();

    ApiResponse<List<CreditProductResponse>> getAllActive();

    ApiResponse<CreditProductResponse> update(Long id, CreditProductUpdateRequest request);

    ApiResponse<String> deactivate(Long id);
}