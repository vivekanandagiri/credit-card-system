package com.example.service;

import com.example.dto.request.CustomerProfileUpdateRequest;
import com.example.dto.response.CustomerProfileResponse;

import java.util.UUID;

/**
 * Defines the contract for managing customer-specific profile data.
 * This service handles operations related to the business domain of a customer,
 * separate from core authentication or security credentials.
 */
public interface CustomerProfileService {

    /**
     * Retrieves the complete profile details for a specific customer.
     * * @param userId the unique identifier of the authenticated user
     * @return the customer's profile data
     * @throws ProfileNotCreatedException if the user exists but has no associated customer record
     */
    CustomerProfileResponse getProfile(UUID userId);

    /**
     * Updates the customer's profile data (mapped to a PUT request).
     * <p>
     * Note: While mapped as a PUT, this operation accepts a partial payload. 
     * Any omitted or null fields in the request will remain unchanged in the database.
     * * @param userId the unique identifier of the authenticated user
     * @param request the payload containing the fields to be updated
     * @return a success confirmation message
     * @throws BadRequestException if the request payload is completely empty
     * @throws ProfileNotCreatedException if the customer record cannot be found
     */
    String updateProfile(UUID userId, CustomerProfileUpdateRequest request);
}