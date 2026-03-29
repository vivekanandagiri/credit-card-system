package com.example.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.dto.request.CustomerProfileUpdateRequest;
import com.example.dto.response.CustomerProfileResponse;
import com.example.enums.UserRole;
import com.example.security.CustomUserPrincipal;
import com.example.security.JwtFilter;
import com.example.security.JwtUtil;
import com.example.service.CustomerProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(
        controllers = CustomerProfileController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class,
        excludeFilters = @Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
class CustomerProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomerProfileService service;

    @MockBean
    private JwtUtil jwtUtil;  
    
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldGetProfile() throws Exception {

        UUID userId = UUID.randomUUID();

        CustomerProfileResponse profile = new CustomerProfileResponse(
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

       
        Mockito.when(service.getProfile(Mockito.any())).thenReturn(profile);

        CustomUserPrincipal principal =
                new CustomUserPrincipal(userId, null, "test@test.com", UserRole.CUSTOMER);

        mockMvc.perform(get("/api/v1/customers/profile")
                .requestAttr("principal", principal))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.message").value("Profile fetched successfully"))
        .andExpect(jsonPath("$.data.firstName").value("Vishal"))
        .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldUpdateProfile() throws Exception {

        UUID userId = UUID.randomUUID();

        CustomerProfileUpdateRequest request = new CustomerProfileUpdateRequest();
        request.setFirstName("Amit");
        request.setLastName("Sharma");
        request.setDateOfBirth(LocalDate.of(1995,8,15));
        request.setResidencyStatus("RESIDENT");
        request.setCitizenshipCountry("India");

        Mockito.when(service.updateProfile(Mockito.any(), Mockito.any()))
                .thenReturn("Profile updated");

        CustomUserPrincipal principal =
                new CustomUserPrincipal(userId, null, "test@test.com", UserRole.CUSTOMER);

        mockMvc.perform(put("/api/v1/customers/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .requestAttr("principal", principal))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("Customer profile updated successfully"))
                .andExpect(jsonPath("$.data").value("Profile updated"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}