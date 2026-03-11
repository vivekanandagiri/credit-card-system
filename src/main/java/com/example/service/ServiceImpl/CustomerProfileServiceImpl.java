package com.example.service.ServiceImpl;

import com.example.dto.request.CustomerProfileUpdateRequest;
import com.example.dto.response.ApiResponse;
import com.example.dto.response.CustomerProfileResponse;

import com.example.entity.Customer;
import com.example.entity.User;

import com.example.exception.ProfileNotCreatedException;
import com.example.exception.UserNotFoundException;
import com.example.mapper.CustomerProfileMapper;
import com.example.repository.CustomerRepository;
import com.example.repository.UserRepository;
import com.example.service.CustomerProfileService;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class CustomerProfileServiceImpl implements CustomerProfileService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final CustomerProfileMapper profileMapper;

    public CustomerProfileServiceImpl(UserRepository userRepository,
                                      CustomerRepository customerRepository, CustomerProfileMapper profileMapper) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
		this.profileMapper = profileMapper;
    }

  
    // GET PROFILE
    @Override
    public ApiResponse<CustomerProfileResponse> getProfile(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new UserNotFoundException("User with id " + userId + " not found"));

        Customer customer = user.getCustomer();

        if (customer == null) {
            throw new ProfileNotCreatedException(
                    "Customer profile not created for user " + userId);
        }

        CustomerProfileResponse response = profileMapper.toResponse(customer);

        return new ApiResponse<>(
                Instant.now(),
                HttpStatus.OK.value(),
                "Profile fetched successfully",
                response
        );
    }

    // UPDATE PROFILE
    @Override
    public ApiResponse<String> updateProfile(UUID userId, CustomerProfileUpdateRequest request) {
    	
    	if (request.getFirstName() == null &&
    	        request.getLastName() == null &&
    	        request.getDateOfBirth() == null &&
    	        request.getResidencyStatus() == null &&
    	        request.getCitizenshipCountry() == null) {

    	        throw new IllegalArgumentException("At least one field must be provided for update");
    	    }

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new UserNotFoundException("User with id " + userId + " not found"));

        Customer customer = user.getCustomer();

        if (customer == null) {
            throw new ProfileNotCreatedException(
                    "Customer profile not created for user " + userId);
        }

        profileMapper.updateCustomer(customer, request);

        customerRepository.save(customer);

        return new ApiResponse<>(
                Instant.now(),
                HttpStatus.OK.value(),
                "Customer profile updated successfully",
                "Profile updated"
        );
    }
}