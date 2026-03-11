package com.example.service;

import com.example.dto.request.CustomerProfileUpdateRequest;
import com.example.dto.response.ApiResponse;
import com.example.dto.response.CustomerProfileResponse;

import java.util.UUID;

public interface CustomerProfileService {


    ApiResponse<CustomerProfileResponse> getProfile(UUID userId);

    ApiResponse<String> updateProfile(UUID userId, CustomerProfileUpdateRequest request);
}