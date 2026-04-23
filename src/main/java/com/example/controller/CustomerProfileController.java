package com.example.controller;

import com.example.api.CustomerProfileApi;
import com.example.dto.request.CustomerProfileUpdateRequest;
import com.example.dto.response.ApiResponse;
import com.example.dto.response.CustomerProfileResponse;
import com.example.security.CustomUserPrincipal;
import com.example.service.CustomerProfileService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for managing Customer Profiles.
 * <p>
 * This class implements {@link CustomerProfileApi} to adhere to a Contract-First API design.
 * All OpenAPI/Swagger documentation (@Operation, @ApiResponse) should be maintained 
 * on the interface to keep this implementation class clean and focused on HTTP routing.
 */
@RestController
public class CustomerProfileController implements CustomerProfileApi {

    private final CustomerProfileService service;

    public CustomerProfileController(CustomerProfileService service) {
        this.service = service;
    }

    @Override
    public ResponseEntity<ApiResponse<CustomerProfileResponse>> getProfile(
            CustomUserPrincipal principal) {

        // SECURITY ARCHITECTURE: 
        // By deriving the userId from the Spring Security Context (principal) rather than a @PathVariable (e.g., /profile/{id}), 
        // we completely eliminate Insecure Direct Object Reference (IDOR) vulnerabilities at the routing level.
        // A user is physically incapable of querying another user's profile.
        CustomerProfileResponse result = service.getProfile(principal.getUserId());

        return ResponseEntity.ok(
                ApiResponse.success(
                		HttpStatus.OK,
                		"Profile fetched successfully", result)
        );
    }

    @Override
    public ResponseEntity<ApiResponse<String>> updateProfile(
            CustomUserPrincipal principal,
            @RequestBody CustomerProfileUpdateRequest request) {

        // The controller's sole responsibility is protocol translation (HTTP -> Java -> HTTP).
        // All business validation (like checking for empty PUT payloads) and persistence logic 
        // is strictly delegated to the service layer.
        String result = service.updateProfile(principal.getUserId(), request);

        return ResponseEntity.ok(
                ApiResponse.success(
                		HttpStatus.OK,
                		"Customer profile updated successfully", result)
        );
    }
}