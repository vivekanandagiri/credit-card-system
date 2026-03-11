package com.example.service;

import java.util.List;
import java.util.UUID;

import com.example.dto.request.AddressCreateRequest;
import com.example.dto.response.AddressResponse;
import com.example.dto.response.ApiResponse;

public interface CustomerAddressService {

    ApiResponse<String> addAddress(UUID customerId, AddressCreateRequest request);

    ApiResponse<List<AddressResponse>> getAddresses(UUID customerId);

    ApiResponse<String> deleteAddress(UUID addressId);
}