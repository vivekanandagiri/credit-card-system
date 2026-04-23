package com.example.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;

import com.example.dto.request.CustomerProfileUpdateRequest;
import com.example.dto.response.CustomerProfileResponse;
import com.example.enums.UserRole;
import com.example.security.CustomUserPrincipal;
import com.example.service.CustomerProfileService;
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

import com.example.config.TimezoneInterceptor;
import com.example.config.WebConfig;
import com.example.security.JwtFilter;
import com.example.security.JwtUtil;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(
        controllers = CustomerProfileController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class,
        excludeFilters = {
                @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtFilter.class),
                @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = TimezoneInterceptor.class),
                @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = WebConfig.class)
        }
)
@AutoConfigureMockMvc(addFilters = false)
class CustomerProfileControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private CustomerProfileService service;
    @MockBean private JwtUtil jwtUtil;

    // ================= GET PROFILE =================

    @Nested
    @DisplayName("GET /profile")
    class GetProfileTests {

        @Test
        void shouldReturnProfile_whenValidUser() throws Exception {
            // GIVEN
            UUID userId = UUID.randomUUID();
            CustomerProfileResponse profile = TestFixtures.validCustomerProfileResponse(userId);

            when(service.getProfile(any())).thenReturn(profile);

            // WHEN + THEN
            performGet(userId)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Profile fetched successfully"))
                    .andExpect(jsonPath("$.data.firstName").value("Vishal"))
                    .andExpect(jsonPath("$.timestamp").exists());
        }
    }

    // ================= UPDATE PROFILE =================

    @Nested
    @DisplayName("PUT /profile")
    class UpdateProfileTests {

        @Test
        void shouldUpdateProfile_whenValidRequest() throws Exception {
            // GIVEN
            UUID userId = UUID.randomUUID();
            CustomerProfileUpdateRequest request = TestFixtures.validCustomerUpdateRequest();

            when(service.updateProfile(any(), any()))
                    .thenReturn("Profile updated");

            // WHEN + THEN
            performPut(userId, request)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message")
                            .value("Customer profile updated successfully"))
                    .andExpect(jsonPath("$.data").value("Profile updated"))
                    .andExpect(jsonPath("$.timestamp").exists());
        }
    }

    // ================= HELPERS =================

    private CustomUserPrincipal getPrincipal(UUID userId) {
        return new CustomUserPrincipal(userId, null, "test@test.com", null, UserRole.CUSTOMER);
    }

    private ResultActions performGet(UUID userId) throws Exception {
        return mockMvc.perform(get("/api/v1/customers/profile")
                .requestAttr("principal", getPrincipal(userId)));
    }

    private ResultActions performPut(UUID userId, Object body) throws Exception {
        return mockMvc.perform(put("/api/v1/customers/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))
                .requestAttr("principal", getPrincipal(userId)));
    }
}