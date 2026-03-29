package com.example.service;

import java.util.List;
import java.util.UUID;

import com.example.dto.request.AddressCreateRequest;
import com.example.dto.response.AddressResponse;

/**
 * Defines the business operations for managing customer addresses.
 */
public interface CustomerAddressService {

    /**
     * Provisions a new address record tied to a specific customer.
     */
    String addAddress(UUID customerId, AddressCreateRequest request);

    /**
     * Retrieves all active addresses belonging to the specified customer.
     */
    List<AddressResponse> getAddresses(UUID customerId);

    /**
     * Deletes a specific address. 
     * * @param customerId the authenticated user requesting the deletion (used for authorization)
     * @param addressId the target address to delete
     * @throws AccessDeniedException if the customer does not own the address
     */
    String deleteAddress(UUID customerId, UUID addressId);
}