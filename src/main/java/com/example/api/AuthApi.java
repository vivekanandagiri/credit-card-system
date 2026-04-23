package com.example.api;

import com.example.dto.request.LoginRequest;
import com.example.dto.request.RegisterRequest;
import com.example.dto.response.ApiResponse;
import com.example.dto.response.LoginResponse;
import com.example.dto.response.RegisterResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

/**
 * API contract for Authentication and User Management.
 *
 * <p>This API provides endpoints for:
 * <ul>
 *     <li>User registration</li>
 *     <li>User authentication (login)</li>
 * </ul>
 *
 * <p>Authentication mechanism:
 * <ul>
 *     <li>JWT-based authentication</li>
 * </ul>
 *
 * <p>All responses are wrapped in {@link ApiResponse}
 */
@Tag(name = "01. Authentication", description = "APIs for user registration and authentication")
public interface AuthApi {

    /**
     * Register a new user.
     *
     * <p>This endpoint creates a new user account using email and password.
     *
     * <p>Typical flow:
     * <ul>
     *     <li>User provides email, password, and basic details</li>
     *     <li>System validates input</li>
     *     <li>User account is created</li>
     * </ul>
     *
     * @param request registration request payload
     * @return registered user details
     */
    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account with email and password"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "User registered successfully",
                    content = @Content(
                            schema = @Schema(implementation = RegisterResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "User already exists"
            )
    })
    @PostMapping("/register")
    ResponseEntity<ApiResponse<RegisterResponse>> register(

            @Parameter(description = "Registration request payload")
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "User registration request",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = RegisterRequest.class)
                    )
            )
            @Valid @RequestBody RegisterRequest request
    );

    /**
     * Authenticate user and generate JWT token.
     *
     * <p>This endpoint validates user credentials and returns a JWT token
     * for authenticated access to protected APIs.
     *
     * <p>Typical flow:
     * <ul>
     *     <li>User submits email and password</li>
     *     <li>System validates credentials</li>
     *     <li>JWT token is generated and returned</li>
     * </ul>
     *
     * @param request login request payload
     * @return login response containing JWT token
     */
    @Operation(
            summary = "User login",
            description = "Authenticates user and returns JWT token"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Login successful",
                    content = @Content(
                            schema = @Schema(implementation = LoginResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Invalid credentials"
            )
    })
    @PostMapping("/login")
    ResponseEntity<ApiResponse<LoginResponse>> login(

            @Parameter(description = "Login credentials")
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "User login request",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = LoginRequest.class)
                    )
            )
            @Valid @RequestBody LoginRequest request
    );
}