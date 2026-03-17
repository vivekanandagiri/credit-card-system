package com.example.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
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

import com.example.dto.request.AddressCreateRequest;
import com.example.dto.response.AddressResponse;
import com.example.dto.response.ApiResponse;
import com.example.security.JwtFilter;
import com.example.service.CustomerAddressService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(
        controllers = CustomerAddressController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class,
        excludeFilters = @Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
class CustomerAddressControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CustomerAddressService service;


    // ADD ADDRESS TESTS

    @Nested
    @DisplayName("Add Address API Tests")
    class AddAddressTests {

        @Test
        void add_address_success() throws Exception {

            AddressCreateRequest request = new AddressCreateRequest(
                    "HOME",
                    "123 MG Road",
                    "Near Metro",
                    "Bangalore",
                    "Karnataka",
                    "560001",
                    "India",
                    true
            );

            ApiResponse<String> response =
                    ApiResponse.success(201, "Address added successfully", "Address created");

            when(service.addAddress(any(), any(AddressCreateRequest.class)))
                    .thenReturn(response);

            mockMvc.perform(post("/api/v1/customers/addresses")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value(201))
                    .andExpect(jsonPath("$.message").value("Address added successfully"))
                    .andExpect(jsonPath("$.data").value("Address created"))
                    .andExpect(jsonPath("$.timestamp").exists());

            verify(service, times(1))
                    .addAddress(any(), any(AddressCreateRequest.class));
        }

        @Test
        void add_address_missing_required_field() throws Exception {

            AddressCreateRequest request = new AddressCreateRequest(
                    "",
                    "",
                    "Near Metro",
                    "Bangalore",
                    "Karnataka",
                    "560001",
                    "India",
                    false
            );

            mockMvc.perform(post("/api/v1/customers/addresses")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void add_address_invalid_postal_code() throws Exception {

            AddressCreateRequest request = new AddressCreateRequest(
                    "HOME",
                    "123 MG Road",
                    "Near Metro",
                    "Bangalore",
                    "Karnataka",
                    "123",
                    "India",
                    false
            );

            mockMvc.perform(post("/api/v1/customers/addresses")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }


    // GET ADDRESSES TESTS
    @Nested
    @DisplayName("Get Addresses API Tests")
    class GetAddressesTests {

        @Test
        void get_addresses_success() throws Exception {

            UUID addressId = UUID.randomUUID();

            AddressResponse address =
                    new AddressResponse(
                            addressId,
                            "123 MG Road",
                            "Bangalore",
                            "Karnataka",
                            "560001",
                            "India"
                    );

            ApiResponse<List<AddressResponse>> response =
                    ApiResponse.success(200, "Addresses fetched successfully", List.of(address));

            when(service.getAddresses(any()))
                    .thenReturn(response);

            mockMvc.perform(get("/api/v1/customers/addresses"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.message").value("Addresses fetched successfully"))
                    .andExpect(jsonPath("$.data[0].addressId").value(addressId.toString()))
                    .andExpect(jsonPath("$.data[0].city").value("Bangalore"))
                    .andExpect(jsonPath("$.timestamp").exists());

            verify(service, times(1)).getAddresses(any());
        }
    }


    // DELETE ADDRESS TESTS
    @Nested
    @DisplayName("Delete Address API Tests")
    class DeleteAddressTests {

        @Test
        void delete_address_success() throws Exception {

            UUID addressId = UUID.randomUUID();

            ApiResponse<String> response =
                    ApiResponse.success(200, "Address deleted successfully", "Deleted");

            when(service.deleteAddress(addressId))
                    .thenReturn(response);

            mockMvc.perform(delete("/api/v1/customers/addresses/{id}", addressId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.message").value("Address deleted successfully"))
                    .andExpect(jsonPath("$.data").value("Deleted"))
                    .andExpect(jsonPath("$.timestamp").exists());

            verify(service, times(1)).deleteAddress(addressId);
        }
    }
}