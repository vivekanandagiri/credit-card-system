package com.example.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.example.dto.request.RegisterRequest;
import com.example.dto.response.UserInfo;
import com.example.entity.Customer;
import com.example.entity.User;
import com.example.enums.KycStatus;
import com.example.enums.UserRole;

/**
 * Structural mapper responsible for translating between Authentication/Registration DTOs 
 * and internal Domain Entities.
 * <p>
 * Architecture Note: While MapStruct is used in other domains, manual mapping is utilized here 
 * to strictly control the injection of default security roles and compliance statuses 
 * during the critical account provisioning phase.
 */
@Component
public class AuthMapper {

	/**
     * Translates a registration payload into a foundational Customer domain entity.
     * * @param request the unvalidated registration payload
     * @return an unsaved Customer entity with initial compliance defaults applied
     */
    public Customer toCustomer(RegisterRequest request) {

        Customer customer = new Customer();

        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setDateOfBirth(request.getDateOfBirth());
        customer.setGender(request.getGender());
        customer.setPanNumber(request.getPanNumber());
        customer.setResidencyStatus(request.getResidencyStatus());
        customer.setCitizenshipCountry(request.getCitizenshipCountry());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getMobileNumber());
        customer.setTimezone(request.getTimezone());
        
        // DOMAIN DEFAULT: All newly provisioned customers must start with a PENDING 
        // KYC status to ensure they cannot access financial features until verified.
        customer.setKycStatus(KycStatus.PENDING);

        return customer;
    }

    /**
     * Constructs the core security/identity record for the system.
     *
     * @param request the registration payload containing contact identifiers
     * @param customer the parent domain entity this user credential belongs to
     * @param encodedPassword the pre-hashed password (raw passwords MUST NOT enter this layer)
     * @return an unsaved User entity with default RBAC (Role-Based Access Control)
     */
    
    public User toUser(RegisterRequest request, Customer customer, String encodedPassword) {

        User user = new User();

        user.setEmail(request.getEmail());
        user.setMobileNumber(request.getMobileNumber());
        // Security: Accepting the encoded string ensures the Mapper is never responsible 
        // for cryptographic operations or handling raw passwords.
        user.setPasswordHash(encodedPassword);
        
        // RBAC DEFAULT: Force the lowest privilege level for new public registrations.
        user.setRole(UserRole.CUSTOMER);
        user.setCustomer(customer);

        return user;
    }

    /**
     * Projects a User entity into a safe, lightweight DTO for JWT token generation 
     * or frontend consumption.
     */
    public UserInfo toUserInfo(User user) {
    	
    	// DEFENSIVE CHECK: While all CUSTOMER roles should have a linked Customer entity,
        // ADMIN or SYSTEM roles might not. This prevents NullPointerExceptions during login.
        UUID customerId = user.getCustomer() != null
                ? user.getCustomer().getCustomerId()
                : null;

        return new UserInfo(
                user.getUserId(),
                user.getRole().name(),
                customerId
        );
    }
}