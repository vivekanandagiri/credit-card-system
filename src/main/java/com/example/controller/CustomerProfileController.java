package com.example.controller;

import com.example.api.CustomerProfileApi;
import com.example.dto.request.CustomerProfileUpdateRequest;
import com.example.dto.response.ApiResponse;
import com.example.dto.response.CustomerProfileResponse;
import com.example.security.CustomUserPrincipal;
import com.example.service.CustomerProfileService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class CustomerProfileController implements CustomerProfileApi {

    private final CustomerProfileService service;

    public CustomerProfileController(CustomerProfileService service) {
        this.service = service;
    }

    @Override
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> getProfile(
            CustomUserPrincipal principal) {

        ApiResponse<CustomerProfileResponse> response =
                service.getProfile(principal.getUserId());

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ApiResponse<String>> updateProfile(
            CustomUserPrincipal principal,
            CustomerProfileUpdateRequest request) {

        ApiResponse<String> response =
                service.updateProfile(principal.getUserId(), request);

        return ResponseEntity.ok(response);
    }
}