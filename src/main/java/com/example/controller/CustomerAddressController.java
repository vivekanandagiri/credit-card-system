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

        ApiResponse<String> response =
                service.addAddress(principal.getCustomerId(), request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getAddresses(
            CustomUserPrincipal principal) {

        ApiResponse<List<AddressResponse>> response =
                service.getAddresses(principal.getCustomerId());

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ApiResponse<String>> deleteAddress(UUID addressId) {

        ApiResponse<String> response =
                service.deleteAddress(addressId);

        return ResponseEntity.ok(response);
    }
}