package com.example.service;

import com.example.dto.request.LoginRequest;
import com.example.dto.request.RegisterRequest;
import com.example.dto.response.ApiResponse;
import com.example.dto.response.LoginResponse;
import com.example.dto.response.RegisterResponse;

public interface AuthService {
	ApiResponse<RegisterResponse> register(RegisterRequest request);
	ApiResponse<LoginResponse> login(LoginRequest request);
}
