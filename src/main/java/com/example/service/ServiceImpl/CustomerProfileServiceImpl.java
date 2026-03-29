package com.example.service.ServiceImpl;

import com.example.dto.request.CustomerProfileUpdateRequest;
import com.example.dto.response.CustomerProfileResponse;

import com.example.entity.Customer;
import com.example.exception.BadRequestException;
import com.example.exception.ProfileNotCreatedException;
import com.example.mapper.CustomerProfileMapper;
import com.example.repository.CustomerRepository;
import com.example.service.CustomerProfileService;
import com.example.service.CustomerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Implementation of {@link CustomerProfileService}.
 * Orchestrates customer profile retrieval and partial updates.
 */
// SENIOR TWEAK: Defaulting to readOnly=true optimizes the Hibernate session 
// by bypassing dirty-checking and allowing the DB to use read replicas for fetch operations.
@Service
@Transactional(readOnly = true)
public class CustomerProfileServiceImpl implements CustomerProfileService {

    private final CustomerRepository customerRepository; 
    private final CustomerService customerService;             
    private final CustomerProfileMapper profileMapper;
 
    public CustomerProfileServiceImpl(
            CustomerRepository customerRepository,
            CustomerService customerService,
            CustomerProfileMapper profileMapper) {
        this.customerRepository = customerRepository;
        this.customerService = customerService;
        this.profileMapper = profileMapper;
    }

    @Override
    public CustomerProfileResponse getProfile(UUID userId) {
        
        // Fetch the entity via the domain service rather than the repository directly 
        // to ensure any standard customer retrieval business logic is applied.
        Customer customer = customerService.getCustomerByUserId(userId);

        if (customer == null) {
            throw new ProfileNotCreatedException(
                    "Customer profile not created for user " + userId);
        }

        return profileMapper.toResponse(customer);
    }

    // Override the class-level read-only setting because this method modifies data.
    @Override
    @Transactional
    public String updateProfile(UUID userId, CustomerProfileUpdateRequest request) {
    	
        // 1. Fail-fast: Ensure the PUT payload isn't entirely empty before querying the DB.
    	validateAtLeastOneFieldProvided(request);
        
    	Customer customer = customerService.getCustomerByUserId(userId);
        if (customer == null) {
            throw new ProfileNotCreatedException(
                    "Customer profile not created for user " + userId);
        }

        // 2. MapStruct overlays the incoming non-null request fields onto the existing entity.
        profileMapper.updateCustomer(customer, request);

        // 3. Save the updated entity back to the database.
        customerRepository.save(customer);

        return "Profile Updated Successfully";
    }
    
    // ── PRIVATE VALIDATION HELPERS ─────────────────────────

    /**
     * Validates the PUT request payload.
     * Ensures that the client has provided at least one valid field to update,
     * preventing unnecessary database transactions for empty requests.
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