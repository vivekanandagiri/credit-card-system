package com.example.mapper;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.example.dto.request.RegisterRequest;
import com.example.dto.response.UserInfo;
import com.example.entity.Customer;
import com.example.entity.User;
import com.example.enums.KycStatus;
import com.example.enums.UserRole;

@Component
public class AuthMapper {

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

        customer.setKycStatus(KycStatus.PENDING);

        return customer;
    }


    public User toUser(RegisterRequest request, Customer customer, String encodedPassword) {

        User user = new User();

        user.setEmail(request.getEmail());
        user.setMobileNumber(request.getMobileNumber());
        user.setPasswordHash(encodedPassword);
        user.setRole(UserRole.CUSTOMER);
        user.setCustomer(customer);

        return user;
    }


    public UserInfo toUserInfo(User user) {

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