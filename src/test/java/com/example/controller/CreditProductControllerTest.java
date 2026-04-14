package com.example.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.config.TimezoneInterceptor;
import com.example.config.WebConfig;
import com.example.dto.request.CreditProductCreateRequest;
import com.example.dto.request.CreditProductUpdateRequest;
import com.example.dto.response.CreditProductCreateResponse;
import com.example.dto.response.CreditProductResponse;
import com.example.enums.ProductStatus;
import com.example.exception.ResourceNotFoundException;
import com.example.security.JwtFilter;
import com.example.security.JwtUtil;
import com.example.service.CreditProductService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@WebMvcTest(
        controllers = CreditProductController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class,
        excludeFilters = { 
				@Filter(type = FilterType.ASSIGNABLE_TYPE,classes = JwtFilter.class),
				@Filter(type = FilterType.ASSIGNABLE_TYPE,classes = TimezoneInterceptor.class),
				@Filter(type = FilterType.ASSIGNABLE_TYPE,classes = WebConfig.class)
	}
)
@AutoConfigureMockMvc(addFilters = false)
class CreditProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreditProductService creditProductService;

    @MockBean
    private JwtFilter jwtFilter;

    @MockBean
    private JwtUtil jwtUtil;

    @Autowired
    private ObjectMapper objectMapper;

    private CreditProductResponse buildResponse() {

        CreditProductResponse response = new CreditProductResponse();
        response.setCreditProductId(1L);
        response.setProductCode("GOLD-CREDIT-CARD-001");
        response.setProductName("Gold Credit Card");
        response.setMinCreditLimit(new BigDecimal("50000"));
        response.setMaxCreditLimit(new BigDecimal("500000"));
        response.setStatus(ProductStatus.ACTIVE);

        return response;
    }
    
    

    // CREATE TESTS
    @Nested
    @DisplayName("Create Credit Product Tests")
    class CreateTests {
    	
    	

        @Test
        void create_success() throws Exception {

        	CreditProductCreateRequest request = new CreditProductCreateRequest();
        	request.setProductName("Gold Credit Card");
        	request.setMinCreditLimit(new BigDecimal("50000"));
        	request.setMaxCreditLimit(new BigDecimal("500000"));
        	request.setEffectiveFrom(LocalDate.now());

        	CreditProductCreateResponse product =
        	        new CreditProductCreateResponse(
        	                1L,
        	                "GOLD-CREDIT-CARD-001",
        	                "Gold Credit Card",
        	                ProductStatus.ACTIVE
        	        );

        	when(creditProductService.create(any()))
        	        .thenReturn(product);

            mockMvc.perform(post("/api/v1/credit-products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.message")
                            .value("Credit product created successfully"))
                    .andExpect(jsonPath("$.data.productName")
                            .value("Gold Credit Card"))
                    .andExpect(jsonPath("$.timestamp").exists());

            verify(creditProductService, times(1)).create(any());
        }
    }

    // GET BY ID TESTS
    @Nested
    @DisplayName("Get Credit Product By Id Tests")
    class GetByIdTests {

        @Test
        void get_by_id_success() throws Exception {

            CreditProductResponse product = buildResponse();

           
            when(creditProductService.getById(1L))
                    .thenReturn(product);

            mockMvc.perform(get("/api/v1/credit-products/{id}", 1))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message")
                            .value("Credit product fetched successfully"))
                    .andExpect(jsonPath("$.data.productName")
                            .value("Gold Credit Card"))
                    .andExpect(jsonPath("$.timestamp").exists());

            verify(creditProductService, times(1)).getById(1L);
        }
    }

    // GET ALL TESTS
    @Nested
    @DisplayName("Get All Credit Products Tests")
    class GetAllTests {

        @Test
        void get_all_success() throws Exception {

            CreditProductResponse product = buildResponse();

            when(creditProductService.getAll())
                    .thenReturn(List.of(product));

            mockMvc.perform(get("/api/v1/credit-products"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message")
                            .value("Credit products fetched successfully"))
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data[0].productName")
                            .value("Gold Credit Card"))
                    .andExpect(jsonPath("$.timestamp").exists());

            verify(creditProductService, times(1)).getAll();
        }
    }

    // UPDATE TESTS
    @Nested
    @DisplayName("Update Credit Product Tests")
    class UpdateTests {

        @Test
        void update_success() throws Exception {

            CreditProductUpdateRequest request = new CreditProductUpdateRequest();
            request.setProductName("Updated Gold Card");

            CreditProductResponse product = buildResponse();
            product.setProductName("Updated Gold Card");


            when(creditProductService.update(eq(1L), any()))
                    .thenReturn(product);

            mockMvc.perform(put("/api/v1/credit-products/{id}", 1)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message")
                            .value("Credit product updated successfully"))
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data.productName")
                            .value("Updated Gold Card"))
                    .andExpect(jsonPath("$.timestamp").exists());

            verify(creditProductService, times(1))
                    .update(eq(1L), any());
        }
    }

    // EXCEPTION TESTS
    @Nested
    @DisplayName("Credit Product Exception Tests")
    class ExceptionTests {

        @Test
        void get_by_id_not_found() throws Exception {

            when(creditProductService.getById(99L))
                    .thenThrow(new ResourceNotFoundException("Product not found"));

            mockMvc.perform(get("/api/v1/credit-products/{id}", 99))
                    .andExpect(status().isNotFound());

            verify(creditProductService, times(1)).getById(99L);
        }
    }
}