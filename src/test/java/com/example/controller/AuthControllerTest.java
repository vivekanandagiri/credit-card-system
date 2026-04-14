package com.example.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;

import com.example.dto.request.LoginRequest;
import com.example.dto.request.RegisterRequest;
import com.example.dto.response.LoginResponse;
import com.example.dto.response.RegisterResponse;
import com.example.dto.response.UserInfo;
import com.example.exception.ConflictException;
import com.example.exception.InvalidCredentialsException;
import com.example.security.JwtFilter;
import com.example.config.TimezoneInterceptor;
import com.example.config.WebConfig;
import com.example.service.AuthService;
import com.example.testutil.TestFixtures;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(
        controllers = AuthController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class,
        excludeFilters = {
                @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtFilter.class),
                @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = TimezoneInterceptor.class),
                @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = WebConfig.class)
        }
)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AuthService authService;

    // ================= REGISTER =================

    @Nested
    @DisplayName("Register API Tests")
    class RegisterTests {

        @Test
        void shouldReturnCreated_whenValidRequest() throws Exception {
            // GIVEN
            RegisterRequest request = TestFixtures.validRegisterRequest();
            UUID userId = UUID.randomUUID();

            when(authService.register(any()))
                    .thenReturn(new RegisterResponse(userId));

            // WHEN + THEN
            performPost("/api/v1/auth/register", request)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.userId").value(userId.toString()))
                    .andExpect(jsonPath("$.message").value("User registered successfully"));
        }

        @Test
        void shouldReturnBadRequest_whenInvalidEmail() throws Exception {
            RegisterRequest request = TestFixtures.validRegisterRequest();
            request.setEmail("invalid");

            performPost("/api/v1/auth/register", request)
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldReturnConflict_whenEmailExists() throws Exception {
            RegisterRequest request = TestFixtures.validRegisterRequest();

            when(authService.register(any()))
                    .thenThrow(new ConflictException("Email exists"));

            performPost("/api/v1/auth/register", request)
                    .andExpect(status().isConflict());
        }
    }

    // ================= LOGIN =================

    @Nested
    @DisplayName("Login API Tests")
    class LoginTests {

        @Test
        void shouldReturnOk_whenValidCredentials() throws Exception {
            // GIVEN
            LoginRequest request = TestFixtures.validLoginRequest();

            UUID userId = UUID.randomUUID();
            UUID customerId = UUID.randomUUID();

            LoginResponse response = new LoginResponse(
                    "jwt-token",
                    "Bearer",
                    3600,
                    new UserInfo(userId, "CUSTOMER", customerId)
            );

            when(authService.login(any())).thenReturn(response);

            // WHEN + THEN
            performPost("/api/v1/auth/login", request)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.accessToken").value("jwt-token"))
                    .andExpect(jsonPath("$.data.user.userId").value(userId.toString()));
        }

        @Test
        void shouldReturnUnauthorized_whenInvalidPassword() throws Exception {
            LoginRequest request = TestFixtures.validLoginRequest();

            when(authService.login(any()))
                    .thenThrow(new InvalidCredentialsException("Invalid"));

            performPost("/api/v1/auth/login", request)
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void shouldReturnBadRequest_whenEmailEmpty() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setEmail("");
            request.setPassword("Password@123");

            performPost("/api/v1/auth/login", request)
                    .andExpect(status().isBadRequest());
        }
    }

    // ================= HELPER =================

    private ResultActions performPost(String url, Object body) throws Exception {
        return mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }
}