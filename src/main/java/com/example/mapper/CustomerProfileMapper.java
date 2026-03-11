package com.example.mapper;

import com.example.dto.request.CustomerProfileUpdateRequest;
import com.example.dto.response.CustomerProfileResponse;
import com.example.entity.Customer;
import org.springframework.stereotype.Component;

@Component
public class CustomerProfileMapper {

    // Entity -> Response
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
    public void updateCustomer(Customer customer, CustomerProfileUpdateRequest request) {

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