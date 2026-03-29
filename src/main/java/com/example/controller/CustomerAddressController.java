package com.example.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.api.CustomerAddressApi;
import com.example.dto.request.AddressCreateRequest;
import com.example.dto.response.AddressResponse;
import com.example.dto.response.ApiResponse;
import com.example.security.CustomUserPrincipal;
import com.example.service.CustomerAddressService;

/**
 * REST Controller for Customer Address Management.
 * Implements Contract-First design via CustomerAddressApi.
 */
@RestController
public class CustomerAddressController implements CustomerAddressApi {

    private final CustomerAddressService service;

    public CustomerAddressController(CustomerAddressService service) {
        this.service = service;
    }

    @Override
    public ResponseEntity<ApiResponse<String>> addAddress(
            CustomUserPrincipal principal,
            AddressCreateRequest request) {

        String result = service.addAddress(principal.getUserId(), request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                		HttpStatus.CREATED,
                		"Address added successfully", result));
    }

    @Override
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getAddresses(
            CustomUserPrincipal principal) {

        List<AddressResponse> result = service.getAddresses(principal.getUserId());
        
        return ResponseEntity.ok(
                ApiResponse.success(
                		HttpStatus.OK,
                		"Addresses fetched successfully", result)
        );
    }

    @Override
    public ResponseEntity<ApiResponse<String>> deleteAddress(
            CustomUserPrincipal principal, 
            @PathVariable UUID addressId) {

        // SECURITY: We pass the principal's ID down to the service layer 
        // to guarantee the user actually owns the address they are trying to delete.
        String result = service.deleteAddress(principal.getUserId(), addressId);

        return ResponseEntity.ok(
                ApiResponse.success(
                		HttpStatus.OK,
                		"Address deleted successfully", result)
        );
    }
}