package com.example.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.dto.request.LoginRequest;
import com.example.dto.request.RegisterRequest;
import com.example.dto.response.ApiResponse;
import com.example.dto.response.LoginResponse;
import com.example.dto.response.RegisterResponse;
import com.example.dto.response.UserInfo;
import com.example.enums.Gender;
import com.example.exception.ConflictException;
import com.example.exception.InvalidCredentialsException;
import com.example.security.JwtFilter;
import com.example.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(
        controllers = AuthController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class,
        excludeFilters = @Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    // REGISTER TESTS 
    @Nested
    @DisplayName("Register API Tests")
    class RegisterTests {

        @Test
        void register_success() throws Exception {

            RegisterRequest request = new RegisterRequest(
                    "vivek@gmail.com",
                    "9765432101",
                    "Vivek@123",
                    "Vivek",
                    "Giri",
                    LocalDate.of(2000, 8, 15),
                    Gender.MALE,
                    "ABCDE1234F",
                    "RESIDENT",
                    "India"
            );

            UUID userId = UUID.randomUUID();

            RegisterResponse registerResponse =
                    new RegisterResponse("User registered successfully", userId);

            ApiResponse<RegisterResponse> apiResponse =
                    ApiResponse.success(201, "User registered successfully", registerResponse);

            when(authService.register(any(RegisterRequest.class)))
                    .thenReturn(apiResponse);

            mockMvc.perform(post("/api/v1/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value(201))
                    .andExpect(jsonPath("$.message").value("User registered successfully"))
                    .andExpect(jsonPath("$.data.userId").value(userId.toString()))
                    .andExpect(jsonPath("$.timestamp").exists());

            verify(authService, times(1)).register(any(RegisterRequest.class));
        }
        
        @Test
        void register_invalid_email() throws Exception {
            RegisterRequest request = new RegisterRequest(
                    "mymail",
                    "9765432101",
                    "Vivek@123",
                    "Vivek",
                    "Giri",
                    LocalDate.of(2000, 8, 15),
                    Gender.MALE,
                    "ABCDE1234F",
                    "RESIDENT",
                    "India"
            );

            mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
        
        @Test
        void register_weak_password() throws Exception {
            RegisterRequest request = new RegisterRequest(
                    "vivek@gmail.com",
                    "9765432101",
                    "vivek",
                    "Vivek",
                    "Giri",
                    LocalDate.of(2000, 8, 15),
                    Gender.MALE,
                    "ABCDE1234F",
                    "RESIDENT",
                    "India"
            );

            mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
        
        
        @Test
        void register_missing_required_field() throws Exception {
            RegisterRequest request = new RegisterRequest(
                    "null",
                    "null",
                    "null",
                    "Vivek",
                    "Giri",
                    LocalDate.of(2000, 8, 15),
                    Gender.MALE,
                    "ABCDE1234F",
                    "RESIDENT",
                    "India"
            );

            mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
        
        
        @Test
        void register_invalid_pan_format() throws Exception {
        	RegisterRequest request = new RegisterRequest(
                    "vivek@gmail.com",
                    "9765432101",
                    "Vivek@123",
                    "Vivek",
                    "Giri",
                    LocalDate.of(2000, 8, 15),
                    Gender.MALE,
                    "ABCDEFGHIJF",
                    "RESIDENT",
                    "India"
            );

            mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }  
        
        @Test
        void register_duplicate_email() throws Exception {

            RegisterRequest request = new RegisterRequest(
                    "vivek@gmail.com",
                    "9765432101",
                    "Vivek@123",
                    "Vivek",
                    "Giri",
                    LocalDate.of(2000, 8, 15),
                    Gender.MALE,
                    "ABCDE1234F",
                    "RESIDENT",
                    "India"
            );

            when(authService.register(any(RegisterRequest.class)))
                    .thenThrow(new ConflictException("Email already exists"));

            mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }
        
        @Test
        void register_duplicate_phone() throws Exception {

            RegisterRequest request = new RegisterRequest(
                    "vivek2@gmail.com",
                    "9765432101",
                    "Vivek@123",
                    "Vivek",
                    "Giri",
                    LocalDate.of(2000, 8, 15),
                    Gender.MALE,
                    "ABCDE1234F",
                    "RESIDENT",
                    "India"
            );

            when(authService.register(any(RegisterRequest.class)))
                    .thenThrow(new ConflictException("Phone already exists"));

            mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }
        
        @Test
        void register_invalid_phone_number() throws Exception {

            RegisterRequest request = new RegisterRequest(
                    "vivek@gmail.com",
                    "123",
                    "Vivek@123",
                    "Vivek",
                    "Giri",
                    LocalDate.of(2000, 8, 15),
                    Gender.MALE,
                    "ABCDE1234F",
                    "RESIDENT",
                    "India"
            );

            mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
        
        @Test
        void register_invalid_dob() throws Exception {

            RegisterRequest request = new RegisterRequest(
                    "vivek@gmail.com",
                    "9765432101",
                    "Vivek@123",
                    "Vivek",
                    "Giri",
                    LocalDate.now().plusDays(1),
                    Gender.MALE,
                    "ABCDE1234F",
                    "RESIDENT",
                    "India"
            );

            mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
        
    }

    // LOGIN TESTS

    @Nested
    @DisplayName("Login API Tests")
    class LoginTests {

        @Test
        void login_success() throws Exception {

            LoginRequest request = new LoginRequest();
            request.setEmail("vivek@gmail.com");
            request.setPassword("Password@123");

            UUID userId = UUID.randomUUID();
            UUID customerId = UUID.randomUUID();

            UserInfo userInfo =
                    new UserInfo(userId, "CUSTOMER", customerId);

            LoginResponse loginResponse =
                    new LoginResponse("jwt-token", "Bearer", 3600, userInfo);

            ApiResponse<LoginResponse> apiResponse =
                    ApiResponse.success(200, "Login successful", loginResponse);

            when(authService.login(any(LoginRequest.class)))
                    .thenReturn(apiResponse);

            mockMvc.perform(post("/api/v1/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.message").value("Login successful"))
                    .andExpect(jsonPath("$.data.accessToken").value("jwt-token"))
                    .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.data.expiresIn").value(3600))
                    .andExpect(jsonPath("$.data.user.userId").value(userId.toString()))
                    .andExpect(jsonPath("$.data.user.customerId").value(customerId.toString()))
                    .andExpect(jsonPath("$.timestamp").exists());

            verify(authService, times(1)).login(any(LoginRequest.class));
        }
        
        
        @Test
        void login_invalid_password() throws Exception {

            LoginRequest request = new LoginRequest();
            request.setEmail("vivek@gmail.com");
            request.setPassword("WrongPassword");

            when(authService.login(any()))
            .thenThrow(new InvalidCredentialsException("Invalid email or password"));

            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
        
        @Test
        void login_user_not_found() throws Exception {

            LoginRequest request = new LoginRequest();
            request.setEmail("unknown@gmail.com");
            request.setPassword("Password@123");

            when(authService.login(any(LoginRequest.class)))
                    .thenThrow(new InvalidCredentialsException("User not found"));

            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }
        @Test
        void login_empty_email() throws Exception {

            LoginRequest request = new LoginRequest();
            request.setEmail("");
            request.setPassword("Password@123");

            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
        @Test
        void login_empty_password() throws Exception {

            LoginRequest request = new LoginRequest();
            request.setEmail("vivek@gmail.com");
            request.setPassword("");

            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
        
        
        
    }
}