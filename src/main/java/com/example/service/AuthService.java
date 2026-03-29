package com.example.service;

import com.example.dto.request.LoginRequest;
import com.example.dto.request.RegisterRequest;
import com.example.dto.response.LoginResponse;
import com.example.dto.response.RegisterResponse;

/**
 * Defines the contract for core user identity and access management.
 * Provides operations for provisioning new accounts and authenticating credentials.
 */
public interface AuthService {

    /**
     * Provisions a new user account in the system.
     * * This method handles the validation of user input, securely hashing the 
     * provided password, and persisting the new user record.
     * * @param request the payload containing the new user's details (e.g., email, raw password,Profile details)
     * @return a response containing the newly generated user ID and creation status
     * @throws UserAlreadyExistsException if an account with the provided email/phone is already registered
     * @throws IllegalArgumentException if the provided request payload fails business validation rules
     */
    RegisterResponse register(RegisterRequest request);

    /**
     * Authenticates a user's credentials and issues an access token.
     * * Verifies the provided credentials against the stored records. Upon success, 
     * it generates a security token (e.g., JWT) to be used for authorization 
     * in subsequent API requests.
     * * @param request the payload containing the user's login credentials
     * @return a response containing the authentication token and basic user claims
     * @throws BadCredentialsException if the password does not match or the user does not exist
     * @throws AccountLockedException if the user account is locked or disabled
     */
    LoginResponse login(LoginRequest request);
}