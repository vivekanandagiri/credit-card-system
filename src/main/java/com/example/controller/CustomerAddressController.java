package com.example.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.example.api.CustomerAddressApi;
import com.example.dto.request.AddressCreateRequest;
import com.example.dto.response.AddressResponse;
import com.example.dto.response.ApiResponse;
import com.example.security.CustomUserPrincipal;
import com.example.service.CustomerAddressService;

import lombok.RequiredArgsConstructor;

/**
 * REST Controller for Customer Address Management.
 * Implements Contract-First design via CustomerAddressApi.
 */
@RestController
@RequiredArgsConstructor
public class CustomerAddressController implements CustomerAddressApi {

    private final CustomerAddressService service;

    @Override
    public ResponseEntity<ApiResponse<String>> addAddress(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            AddressCreateRequest request) {

        String result = service.addAddress(principal.getUserId(), request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Address added successfully", result));
    }

    @Override
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getAddresses(
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        List<AddressResponse> result = service.getAddresses(principal.getUserId());

        return ResponseEntity.ok(
                ApiResponse.success("Addresses fetched successfully", result)
        );
    }

    @Override
    public ResponseEntity<ApiResponse<String>> deleteAddress(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            java.util.UUID addressId) {

        String result = service.deleteAddress(principal.getUserId(), addressId);

        return ResponseEntity.ok(
                ApiResponse.success("Address deleted successfully", result)
        );
    }
}