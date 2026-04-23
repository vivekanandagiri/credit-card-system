package com.example.service.ServiceImpl;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.dto.request.CustomerProfileUpdateRequest;
import com.example.dto.response.CustomerProfileResponse;
import com.example.entity.Customer;
import com.example.exception.BadRequestException;
import com.example.exception.ProfileNotCreatedException;
import com.example.mapper.CustomerProfileMapper;
import com.example.service.CustomerProfileService;
import com.example.service.CustomerService;

import lombok.RequiredArgsConstructor;

/**
 * Implementation of {@link CustomerProfileService} responsible for
 * managing customer profile data.
 *
 * <p><b>Responsibilities:</b></p>
 * <ul>
 *     <li>Retrieve customer profile details</li>
 *     <li>Handle partial updates to customer profile</li>
 * </ul>
 *
 * <p><b>Design Principles:</b></p>
 * <ul>
 *     <li>Delegates customer lookup to {@link CustomerService}</li>
 *     <li>Uses mapper for DTO ↔ entity transformations</li>
 *     <li>Ensures validation before performing updates</li>
 * </ul>
 *
 * <p><b>Update Behavior:</b></p>
 * <ul>
 *     <li>Supports partial updates (PATCH-like behavior)</li>
 *     <li>Only non-null fields are applied</li>
 *     <li>Rejects empty update requests</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CustomerProfileServiceImpl implements CustomerProfileService {

    private final CustomerService customerService;             
    private final CustomerProfileMapper profileMapper;
 
   
    /**
     * Retrieves the customer profile for a given user.
     *
     * @param userId user identifier
     * @return {@link CustomerProfileResponse}
     * @throws ProfileNotCreatedException if profile does not exist
     */
    @Override
    public CustomerProfileResponse getProfile(UUID userId) {
        
        Customer customer = customerService.getCustomerByUserId(userId);
        return profileMapper.toResponse(customer);
    }

    /**
     * Updates the customer profile with provided fields.
     *
     * <p>Process:</p>
     * <ol>
     *     <li>Validate that at least one field is provided</li>
     *     <li>Fetch customer entity</li>
     *     <li>Apply partial updates via mapper</li>
     *     <li>Persist updated entity</li>
     * </ol>
     *
     * @param userId  user identifier
     * @param request update payload (partial fields allowed)
     * @return confirmation message
     *
     * @throws BadRequestException if request is empty
     * @throws ProfileNotCreatedException if profile does not exist
     */
    @Override
    @Transactional
    public String updateProfile(UUID userId, CustomerProfileUpdateRequest request) {
    	
    	// 1. Fail-fast validation
    	validateAtLeastOneFieldProvided(request);
        
    	Customer customer = customerService.getCustomerByUserId(userId);

        // 2. Apply partial updates (mapper handles null checks)
        profileMapper.updateCustomer(customer, request);

        // 3. Persist changes
        customerService.saveCustomer(customer);
        
        return "Profile Updated Successfully";
    }
    
    // ── PRIVATE VALIDATION HELPERS ─────────────────────────

    /**
     * Validates that at least one field is provided in the update request.
     *
     * <p>Prevents unnecessary database operations and enforces meaningful updates.</p>
     *
     * @param request update payload
     * @throws BadRequestException if all fields are null
     */
    private void validateAtLeastOneFieldProvided(CustomerProfileUpdateRequest request) {
        boolean hasField = request.getFirstName() != null
                || request.getLastName() != null
                || request.getDateOfBirth() != null
                || request.getResidencyStatus() != null
                || request.getCitizenshipCountry() != null;
 
        if (!hasField) {
            throw new BadRequestException("At least one field must be provided for update");
        }
    }
}