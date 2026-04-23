package com.example.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.UUID;

import com.example.dto.request.AddressCreateRequest;
import com.example.dto.response.AddressResponse;
import com.example.enums.UserRole;
import com.example.security.CustomUserPrincipal;
import com.example.service.CustomerAddressService;
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

import com.example.config.TimezoneInterceptor;
import com.example.config.WebConfig;
import com.example.security.JwtFilter;

@WebMvcTest(
        controllers = CustomerAddressController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class,
        excludeFilters = {
                @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtFilter.class),
                @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = TimezoneInterceptor.class),
                @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = WebConfig.class)
        }
)
@AutoConfigureMockMvc(addFilters = false)
class CustomerAddressControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private CustomerAddressService service;

    // ================= ADD ADDRESS =================

    @Nested
    @DisplayName("POST /addresses")
    class AddAddressTests {

        @Test
        void shouldAddAddress_whenValidRequest() throws Exception {
            // GIVEN
            UUID userId = UUID.randomUUID();
            AddressCreateRequest request = TestFixtures.validAddressRequest();

            when(service.addAddress(any(), any()))
                    .thenReturn("Address created");

            // WHEN + THEN
            performPost(userId, request)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message").value("Address added successfully"))
                    .andExpect(jsonPath("$.data").value("Address created"))
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        void shouldReturnBadRequest_whenInvalidInput() throws Exception {
            AddressCreateRequest request = TestFixtures.invalidAddressRequest();

            performPost(UUID.randomUUID(), request)
                    .andExpect(status().isBadRequest());
        }
    }

    // ================= GET ADDRESSES =================

    @Nested
    @DisplayName("GET /addresses")
    class GetAddressesTests {

        @Test
        void shouldReturnAddresses_whenAvailable() throws Exception {
            // GIVEN
            UUID userId = UUID.randomUUID();
            UUID addressId = UUID.randomUUID();

            AddressResponse response = new AddressResponse(
                    addressId, "123 MG Road", "Bangalore",
                    "Karnataka", "560001", "India"
            );

            when(service.getAddresses(any()))
                    .thenReturn(List.of(response));

            // WHEN + THEN
            performGet(userId)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].addressId")
                            .value(addressId.toString()))
                    .andExpect(jsonPath("$.data[0].city")
                            .value("Bangalore"));
        }
    }

    // ================= DELETE ADDRESS =================

    @Nested
    @DisplayName("DELETE /addresses/{id}")
    class DeleteAddressTests {

        @Test
        void shouldDeleteAddress_whenValidRequest() throws Exception {
            // GIVEN
            UUID userId = UUID.randomUUID();
            UUID addressId = UUID.randomUUID();

            when(service.deleteAddress(any(), eq(addressId)))
                    .thenReturn("Address Deleted");

            // WHEN + THEN
            performDelete(userId, addressId)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message")
                            .value("Address deleted successfully"))
                    .andExpect(jsonPath("$.data")
                            .value("Address Deleted"));
        }
    }

    // ================= HELPERS =================

    private CustomUserPrincipal getPrincipal(UUID userId) {
        return new CustomUserPrincipal(userId, null, "test@test.com", null, UserRole.CUSTOMER);
    }

    private ResultActions performPost(UUID userId, Object body) throws Exception {
        return mockMvc.perform(post("/api/v1/customers/addresses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body))
                .requestAttr("principal", getPrincipal(userId)));
    }

    private ResultActions performGet(UUID userId) throws Exception {
        return mockMvc.perform(get("/api/v1/customers/addresses")
                .requestAttr("principal", getPrincipal(userId)));
    }

    private ResultActions performDelete(UUID userId, UUID id) throws Exception {
        return mockMvc.perform(delete("/api/v1/customers/addresses/{id}", id)
                .requestAttr("principal", getPrincipal(userId)));
    }
}