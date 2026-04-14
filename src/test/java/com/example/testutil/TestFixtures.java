package com.example.testutil;

import com.example.dto.request.AddressCreateRequest;
import com.example.dto.request.CustomerProfileUpdateRequest;
import com.example.dto.request.LoginRequest;
import com.example.dto.request.RegisterRequest;
import com.example.dto.response.CustomerProfileResponse;
import com.example.entity.Customer;
import com.example.entity.User;
import com.example.enums.Gender;
import com.example.enums.KycStatus;
import com.example.enums.UserRole;

import java.time.LocalDate;
import java.util.UUID;

public class TestFixtures {

    public static RegisterRequest validRegisterRequest() {
        return new RegisterRequest(
                "vivek@gmail.com",
                "9765432101",
                "Password@123",
                "Vivek",
                "Giri",
                LocalDate.of(2000, 8, 15),
                Gender.MALE,
                "ABCDE1234F",
                "RESIDENT",
                "India",
                "Asia/Kolkata"
        );
    }

    public static LoginRequest validLoginRequest() {
        LoginRequest req = new LoginRequest();
        req.setEmail("vivek@gmail.com");
        req.setPassword("Password@123");
        return req;
    }

    public static User validUser() {
        User user = new User();
        user.setUserId(UUID.randomUUID());
        user.setEmail("user_" + UUID.randomUUID() + "@test.com");
        user.setPasswordHash("encoded");
        user.setRole(UserRole.CUSTOMER);
        user.setActive(true);
        user.setLocked(false);
        return user;
    }

    public static Customer validCustomer() {
        Customer customer = new Customer();
        customer.setCustomerId(UUID.randomUUID());
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setDateOfBirth(LocalDate.of(1995, 8, 15));
        customer.setEmail("john_" + UUID.randomUUID() + "@example.com");  // 🔥 FIX
        customer.setPhone("98" + System.nanoTime());
        customer.setPanNumber("PAN" + System.nanoTime());
        customer.setResidencyStatus("RESIDENT");
        customer.setCitizenshipCountry("India");
        customer.setGender(Gender.MALE);
        customer.setTimezone("Asia/Kolkata"); 
        customer.setKycStatus(KycStatus.PENDING);
        return customer;
    }

    public static CustomerProfileResponse validCustomerProfileResponse(UUID userId) {
        return new CustomerProfileResponse(
                userId,
                "Vishal",
                "Das",
                LocalDate.of(1995, 8, 15),
                "vishal@gmail.com",
                "9876543210",
                "ABCDE****F",
                "RESIDENT",
                "India"
        );
    }

    public static CustomerProfileUpdateRequest validCustomerUpdateRequest() {
        CustomerProfileUpdateRequest request = new CustomerProfileUpdateRequest();
        request.setFirstName("Amit");
        request.setLastName("Sharma");
        request.setDateOfBirth(LocalDate.of(1995, 8, 15));
        request.setResidencyStatus("RESIDENT");
        request.setCitizenshipCountry("India");
        return request;
    }

    public static Customer validCustomerWithUser() {
        User user = new User();
        user.setEmail("user_" + UUID.randomUUID() + "@test.com");
        user.setMobileNumber("98" + System.nanoTime());     
        user.setPasswordHash("encoded");        
        user.setRole(UserRole.CUSTOMER);        
        user.setActive(true);
        user.setLocked(false);

        Customer customer = new Customer();

        customer.setUser(user);
        user.setCustomer(customer);

        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setDateOfBirth(LocalDate.of(1995, 8, 15));
        customer.setEmail("john_" + UUID.randomUUID() + "@example.com");  // 🔥 FIX
        customer.setPhone("98" + System.nanoTime());
        customer.setPanNumber("PAN" + System.nanoTime());
        customer.setResidencyStatus("RESIDENT");
        customer.setCitizenshipCountry("India");

        customer.setGender(Gender.MALE);
        customer.setTimezone("Asia/Kolkata");
        customer.setKycStatus(KycStatus.PENDING);

        return customer;
    }
    public static AddressCreateRequest validAddressRequest() {
        return new AddressCreateRequest(
                "HOME",
                "123 MG Road",
                "Near Metro",
                "Bangalore",
                "Karnataka",
                "560001",
                "India",
                true
        );
    }

    public static AddressCreateRequest invalidAddressRequest() {
        return new AddressCreateRequest(
                "", "", "Near Metro",
                "Bangalore", "Karnataka",
                "123", "India", false
        );
    }
}
