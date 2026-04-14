package com.example.mapper;

import com.example.dto.request.CustomerProfileUpdateRequest;
import com.example.dto.response.CustomerProfileResponse;
import com.example.entity.Customer;
import org.springframework.stereotype.Component;

/**
 * Structural mapper for the Customer Profile domain.
 * <p>
 * Architecture Note: Manual mapping is used here instead of MapStruct to maintain 
 * explicit, granular control over PII (Personally Identifiable Information) updates 
 * and to prevent accidental overwriting of sensitive domain fields.
 */
@Component
public class CustomerProfileMapper {

	/**
     * Projects the rich Customer domain entity into a flat, safe DTO for API consumption.
     * * @param customer the attached database entity
     * @return the profile response payload
     */
    public CustomerProfileResponse toResponse(Customer customer) {

        return new CustomerProfileResponse(
                customer.getCustomerId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getDateOfBirth(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getPanNumber(),
                customer.getResidencyStatus(),
                customer.getCitizenshipCountry()
        );
    }

    // Update entity from request
    /**
     * Only non-null fields from the request will mutate the entity state.
     * @param customer the managed JPA entity to be updated
     * @param request the payload containing the requested changes
     */
    public void updateCustomer(Customer customer, CustomerProfileUpdateRequest request) {

    	// COMPLIANCE WARNING: 
        // In a regulated financial system, altering core PII (Name, DOB, Citizenship, PAN) 
        // MUST trigger a KYC re-verification process. 
        // TODO: Ensure the CustomerProfileService intercepts these specific changes 
        // and downgrades the customer's KycStatus to RESUBMIT_REQUIRED or PENDING.
    	
        if (request.getFirstName() != null)
            customer.setFirstName(request.getFirstName());

        if (request.getLastName() != null)
            customer.setLastName(request.getLastName());

        if (request.getDateOfBirth() != null)
            customer.setDateOfBirth(request.getDateOfBirth());

        if (request.getResidencyStatus() != null)
            customer.setResidencyStatus(request.getResidencyStatus());

        if (request.getCitizenshipCountry() != null)
            customer.setCitizenshipCountry(request.getCitizenshipCountry());
    }
}