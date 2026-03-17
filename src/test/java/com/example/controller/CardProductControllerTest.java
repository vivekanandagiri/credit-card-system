package com.example.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
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

import com.example.dto.request.CardProductCreateRequest;
import com.example.dto.request.CardProductUpdateRequest;
import com.example.dto.response.ApiResponse;
import com.example.dto.response.CardProductCreateResponse;
import com.example.dto.response.CardProductResponse;
import com.example.enums.CardType;
import com.example.enums.NetworkType;
import com.example.enums.ProductStatus;
import com.example.security.JwtFilter;
import com.example.service.CardProductService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(
        controllers = CardProductController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class,
        excludeFilters = @Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtFilter.class
        )
)
@AutoConfigureMockMvc(addFilters = false)
class CardProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CardProductService cardProductService;

    // CREATE TESTS
    @Nested
    @DisplayName("Create Card Product Tests")
    class CreateTests {

        @Test
        void create_success() throws Exception {

            CardProductCreateRequest request = new CardProductCreateRequest();
            request.setCreditProductId(1L);
            request.setProductName("Gold Visa Card");
            request.setNetworkType(NetworkType.VISA);
            request.setCardType(CardType.PHYSICAL);
            request.setAnnualFee(new BigDecimal("1999"));
            request.setCardValidityYears(5);

            UUID id = UUID.randomUUID();


            CardProductCreateResponse response =
                    new CardProductCreateResponse(
                            id,
                            "Gold Visa Card",
                            NetworkType.VISA,
                            ProductStatus.ACTIVE
                    );

            ApiResponse<CardProductCreateResponse> apiResponse =
                    ApiResponse.success(201, "Card product created", response);

            when(cardProductService.create(any(CardProductCreateRequest.class)))
                    .thenReturn(apiResponse);

            mockMvc.perform(post("/api/v1/card-products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value(201))
                    .andExpect(jsonPath("$.data.cardProductId").value(id.toString()))
                    .andExpect(jsonPath("$.data.productName").value("Gold Visa Card"))
                    .andExpect(jsonPath("$.timestamp").exists());

            verify(cardProductService, times(1)).create(any(CardProductCreateRequest.class));
        }

        @Test
        void create_missing_required_fields() throws Exception {

            CardProductCreateRequest request = new CardProductCreateRequest();

            mockMvc.perform(post("/api/v1/card-products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void create_invalid_annual_fee() throws Exception {

            CardProductCreateRequest request = new CardProductCreateRequest();
            request.setCreditProductId(1L);
            request.setProductName("Gold Visa Card");
            request.setNetworkType(NetworkType.VISA);
            request.setCardType(CardType.PHYSICAL);
            request.setAnnualFee(new BigDecimal("-100"));

            mockMvc.perform(post("/api/v1/card-products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    // GET ALL
    @Nested
    @DisplayName("Get All Card Products Tests")
    class GetAllTests {

        @Test
        void get_all_success() throws Exception {

            CardProductResponse response = new CardProductResponse(
                    UUID.randomUUID(),
                    1L,
                    "Gold Credit Product",
                    "Gold Visa Card",
                    NetworkType.VISA,
                    CardType.PHYSICAL,
                    new BigDecimal("1999"),
                    5,
                    true,
                    true,
                    true,
                    true,
                    new BigDecimal("25000"),
                    new BigDecimal("100000"),
                    new BigDecimal("75000"),
                    15,
                    new BigDecimal("3.50"),
                    "Premium gold card",
                    ProductStatus.ACTIVE
            );

            ApiResponse<List<CardProductResponse>> apiResponse =
                    ApiResponse.success(200, "Success", List.of(response));

            when(cardProductService.getAll()).thenReturn(apiResponse);

            mockMvc.perform(get("/api/v1/card-products"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.data[0].productName").value("Gold Visa Card"));

            verify(cardProductService, times(1)).getAll();
        }
    }

    // GET ACTIVE
    @Nested
    @DisplayName("Get Active Card Products Tests")
    class GetActiveTests {

        @Test
        void get_all_active_success() throws Exception {

            ApiResponse<List<CardProductResponse>> apiResponse =
                    ApiResponse.success(200, "Success", List.of());

            when(cardProductService.getAllActive()).thenReturn(apiResponse);

            mockMvc.perform(get("/api/v1/card-products/active"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200));

            verify(cardProductService, times(1)).getAllActive();
        }
    }

    // GET BY ID
    @Nested
    @DisplayName("Get Card Product By Id Tests")
    class GetByIdTests {

        @Test
        void get_by_id_success() throws Exception {

            UUID id = UUID.randomUUID();

            CardProductResponse response = new CardProductResponse(
                    id,
                    1L,
                    "Gold Credit Product",
                    "Gold Visa Card",
                    NetworkType.VISA,
                    CardType.PHYSICAL,
                    new BigDecimal("1999"),
                    5,
                    true,
                    true,
                    true,
                    true,
                    new BigDecimal("25000"),
                    new BigDecimal("100000"),
                    new BigDecimal("75000"),
                    15,
                    new BigDecimal("3.50"),
                    "Premium gold card",
                    ProductStatus.ACTIVE
            );

            ApiResponse<CardProductResponse> apiResponse =
                    ApiResponse.success(200, "Success", response);

            when(cardProductService.getById(any(UUID.class)))
                    .thenReturn(apiResponse);

            mockMvc.perform(get("/api/v1/card-products/{id}", id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.cardProductId").value(id.toString()));

            verify(cardProductService, times(1)).getById(any(UUID.class));
        }
    }

    // GET BY CREDIT PRODUCT
    @Nested
    @DisplayName("Get By Credit Product Tests")
    class GetByCreditProductTests {

        @Test
        void get_by_credit_product_success() throws Exception {

            ApiResponse<List<CardProductResponse>> apiResponse =
                    ApiResponse.success(200, "Success", List.of());

            when(cardProductService.getByCreditProduct(anyLong()))
                    .thenReturn(apiResponse);

            mockMvc.perform(get("/api/v1/card-products/credit-product/{creditProductId}", 1))
                    .andExpect(status().isOk());

            verify(cardProductService, times(1)).getByCreditProduct(anyLong());
        }
    }

    // UPDATE
    @Nested
    @DisplayName("Update Card Product Tests")
    class UpdateTests {

        @Test
        void update_success() throws Exception {

            UUID id = UUID.randomUUID();

            CardProductUpdateRequest request = new CardProductUpdateRequest();

            ApiResponse<CardProductResponse> apiResponse =
                    ApiResponse.success(200, "Updated successfully", null);

            when(cardProductService.update(any(UUID.class), any(CardProductUpdateRequest.class)))
                    .thenReturn(apiResponse);

            mockMvc.perform(put("/api/v1/card-products/{id}", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            verify(cardProductService, times(1))
                    .update(any(UUID.class), any(CardProductUpdateRequest.class));
        }
    }

 // UPDATE STATUS
    @Nested
    @DisplayName("Update Card Product Status Tests")
    class UpdateStatusTests {
        @Test
        void updateStatus_deactivate_success() throws Exception {

            UUID id = UUID.randomUUID();

            ApiResponse<String> apiResponse =
                    ApiResponse.success(200, "Card product deactivated successfully", "INACTIVE");

            when(cardProductService.updateStatus(any(UUID.class), any(ProductStatus.class)))
                    .thenReturn(apiResponse);

            mockMvc.perform(
                    patch("/api/v1/card-products/{id}/status", id)
                            .param("status", "INACTIVE")
            )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message")
                            .value("Card product deactivated successfully"));

            verify(cardProductService, times(1))
                    .updateStatus(any(UUID.class), eq(ProductStatus.INACTIVE));
        }
    }
}