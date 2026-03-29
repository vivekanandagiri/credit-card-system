package com.example.controller;

import com.example.api.AuthApi;
import com.example.dto.request.LoginRequest;
import com.example.dto.request.RegisterRequest;
import com.example.dto.response.ApiResponse;
import com.example.dto.response.LoginResponse;
import com.example.dto.response.RegisterResponse;
import com.example.service.AuthService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController implements AuthApi {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public ResponseEntity<ApiResponse<RegisterResponse>> register(
            @RequestBody RegisterRequest request) {

        RegisterResponse result = authService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED,"User registered successfully", result));
    }

    @Override
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @RequestBody LoginRequest request) {

        LoginResponse result = authService.login(request);

        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK,"Login successful", result)
        );
    }
}