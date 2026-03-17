package com.example.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.dto.request.CardProductCreateRequest;
import com.example.dto.request.CardProductUpdateRequest;
import com.example.dto.response.ApiResponse;
import com.example.dto.response.CardProductCreateResponse;
import com.example.dto.response.CardProductResponse;
import com.example.entity.CreditCardProduct;
import com.example.entity.CreditProduct;
import com.example.enums.CardType;
import com.example.enums.NetworkType;
import com.example.enums.ProductStatus;
import com.example.exception.BadRequestException;
import com.example.exception.ConflictException;
import com.example.exception.ResourceNotFoundException;
import com.example.mapper.CardProductMapper;
import com.example.repository.CreditCardProductRepository;
import com.example.repository.CreditProductRepository;
import com.example.service.ServiceImpl.CardProductServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CardProductServiceImplTest {

    @Mock
    private CreditCardProductRepository cardProductRepository;

    @Mock
    private CreditProductRepository creditProductRepository;

    @Mock
    private CardProductMapper mapper;

    @InjectMocks
    private CardProductServiceImpl service;

    private UUID cardProductId;
    private CreditProduct creditProduct;
    private CreditCardProduct cardProduct;
    private CardProductResponse response;
    private CardProductCreateResponse createResponse;

    @BeforeEach
    void setup() {

        cardProductId = UUID.randomUUID();

        creditProduct = new CreditProduct();
        creditProduct.setCreditProductId(1L);
        creditProduct.setStatus(ProductStatus.ACTIVE);

        cardProduct = new CreditCardProduct();
        cardProduct.setCardProductId(cardProductId);
        cardProduct.setStatus(ProductStatus.ACTIVE);

        response = new CardProductResponse(
                cardProductId,
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
                new BigDecimal("3.5"),
                "Premium card",
                ProductStatus.ACTIVE
        );

        createResponse = new CardProductCreateResponse(
                cardProductId,
                "Gold Visa Card",
                NetworkType.VISA,
                ProductStatus.ACTIVE
        );
    }

    // CREATE TESTS
    @Nested
    @DisplayName("Create Card Product Tests")
    class CreateTests {

        @Test
        void create_success() {

            CardProductCreateRequest request = new CardProductCreateRequest();
            request.setCreditProductId(1L);
            request.setStatementCycleDay(15);

            when(creditProductRepository.findById(1L))
                    .thenReturn(Optional.of(creditProduct));

            when(mapper.toEntity(request, creditProduct))
                    .thenReturn(cardProduct);

            when(cardProductRepository.save(cardProduct))
                    .thenReturn(cardProduct);

            when(mapper.toCreateResponse(cardProduct))
            .thenReturn(createResponse);

            ApiResponse<CardProductCreateResponse> result = service.create(request);

            assertEquals(201, result.getStatus());
            assertEquals("Card product created successfully", result.getMessage());

            verify(cardProductRepository).save(cardProduct);
        }

        @Test
        void create_credit_product_not_found() {

            CardProductCreateRequest request = new CardProductCreateRequest();
            request.setCreditProductId(99L);

            when(creditProductRepository.findById(99L))
                    .thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> service.create(request));
        }

        @Test
        void create_credit_product_inactive() {

            creditProduct.setStatus(ProductStatus.INACTIVE);

            CardProductCreateRequest request = new CardProductCreateRequest();
            request.setCreditProductId(1L);

            when(creditProductRepository.findById(1L))
                    .thenReturn(Optional.of(creditProduct));

            assertThrows(BadRequestException.class,
                    () -> service.create(request));
        }

        @Test
        void create_invalid_statement_cycle() {

            CardProductCreateRequest request = new CardProductCreateRequest();
            request.setCreditProductId(1L);
            request.setStatementCycleDay(30);

            when(creditProductRepository.findById(1L))
                    .thenReturn(Optional.of(creditProduct));

            assertThrows(BadRequestException.class,
                    () -> service.create(request));
        }
        
        @Test
        void create_duplicate_product_name() {

            CardProductCreateRequest request = new CardProductCreateRequest();
            request.setCreditProductId(1L);
            request.setProductName("Gold Card");

            when(creditProductRepository.findById(1L))
                    .thenReturn(Optional.of(creditProduct));

            when(cardProductRepository
                    .existsByProductNameAndCreditProductCreditProductId(
                            "Gold Card", 1L))
                    .thenReturn(true);

            assertThrows(ConflictException.class,
                    () -> service.create(request));
        }
        
        @Test
        void create_invalid_atm_rule() {

            CardProductCreateRequest request = new CardProductCreateRequest();
            request.setCreditProductId(1L);
            request.setAtmWithdrawalAllowed(false);
            request.setAtmDailyLimit(new BigDecimal("5000"));

            when(creditProductRepository.findById(1L))
                    .thenReturn(Optional.of(creditProduct));

            assertThrows(BadRequestException.class,
                    () -> service.create(request));
        }
        
        @Test
        void create_invalid_ecommerce_rule() {

            CardProductCreateRequest request = new CardProductCreateRequest();
            request.setCreditProductId(1L);
            request.setOnlineTransactionsAllowed(false);
            request.setEcommerceDailyLimit(new BigDecimal("5000"));

            when(creditProductRepository.findById(1L))
                    .thenReturn(Optional.of(creditProduct));

            assertThrows(BadRequestException.class,
                    () -> service.create(request));
        }
        
        @Test
        void create_negative_annual_fee() {

            CardProductCreateRequest request = new CardProductCreateRequest();
            request.setCreditProductId(1L);
            request.setAnnualFee(new BigDecimal("-100"));

            when(creditProductRepository.findById(1L))
                    .thenReturn(Optional.of(creditProduct));

            assertThrows(BadRequestException.class,
                    () -> service.create(request));
        }
        
        @Test
        void create_invalid_forex_markup() {

            CardProductCreateRequest request = new CardProductCreateRequest();
            request.setCreditProductId(1L);
            request.setForexMarkupPercent(new BigDecimal("150"));

            when(creditProductRepository.findById(1L))
                    .thenReturn(Optional.of(creditProduct));

            assertThrows(BadRequestException.class,
                    () -> service.create(request));
        }
    }

    // GET TESTS
    @Nested
    @DisplayName("Get Card Product Tests")
    class GetTests {

        @Test
        void get_by_id_success() {

            when(cardProductRepository.findById(cardProductId))
                    .thenReturn(Optional.of(cardProduct));

            when(mapper.toResponse(cardProduct))
                    .thenReturn(response);

            ApiResponse<CardProductResponse> result =
                    service.getById(cardProductId);

            assertEquals(200, result.getStatus());
            assertEquals(cardProductId, result.getData().getCardProductId());
        }

        @Test
        void get_by_id_not_found() {

            when(cardProductRepository.findById(cardProductId))
                    .thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> service.getById(cardProductId));
        }

        @Test
        void get_all_success() {

            when(cardProductRepository.findAll())
                    .thenReturn(List.of(cardProduct));

            when(mapper.toResponse(cardProduct))
                    .thenReturn(response);

            ApiResponse<List<CardProductResponse>> result =
                    service.getAll();

            assertEquals(200, result.getStatus());
            assertEquals(1, result.getData().size());
        }

        @Test
        void get_all_active_success() {

            when(cardProductRepository.findAllByStatus(ProductStatus.ACTIVE))
                    .thenReturn(List.of(cardProduct));

            when(mapper.toResponse(cardProduct))
                    .thenReturn(response);

            ApiResponse<List<CardProductResponse>> result =
                    service.getAllActive();

            assertEquals(1, result.getData().size());
        }

        @Test
        void get_by_credit_product_success() {

            when(cardProductRepository
                    .findAllByCreditProductCreditProductId(1L))
                    .thenReturn(List.of(cardProduct));

            when(mapper.toResponse(cardProduct))
                    .thenReturn(response);

            ApiResponse<List<CardProductResponse>> result =
                    service.getByCreditProduct(1L);

            assertEquals(1, result.getData().size());
        }
    }

    // UPDATE TESTS
    @Nested
    @DisplayName("Update Card Product Tests")
    class UpdateTests {

        @Test
        void update_success() {

            CardProductUpdateRequest request =
                    new CardProductUpdateRequest();

            request.setProductName("Updated Gold Card");

            when(cardProductRepository.findById(cardProductId))
                    .thenReturn(Optional.of(cardProduct));

            when(cardProductRepository.save(cardProduct))
                    .thenReturn(cardProduct);

            when(mapper.toResponse(cardProduct))
                    .thenReturn(response);

            ApiResponse<CardProductResponse> result =
                    service.update(cardProductId, request);

            assertEquals(200, result.getStatus());

            verify(cardProductRepository).save(cardProduct);
        }

        @Test
        void update_inactive_product() {

            cardProduct.setStatus(ProductStatus.INACTIVE);

            when(cardProductRepository.findById(cardProductId))
                    .thenReturn(Optional.of(cardProduct));

            assertThrows(BadRequestException.class,
                    () -> service.update(cardProductId,
                            new CardProductUpdateRequest()));
        }

        @Test
        void update_invalid_statement_cycle() {

            CardProductUpdateRequest request =
                    new CardProductUpdateRequest();

            request.setStatementCycleDay(35);

            when(cardProductRepository.findById(cardProductId))
                    .thenReturn(Optional.of(cardProduct));

            assertThrows(BadRequestException.class,
                    () -> service.update(cardProductId, request));
        }
        
        @Test
        void update_empty_request() {

            CardProductUpdateRequest request =
                    new CardProductUpdateRequest();

            when(cardProductRepository.findById(cardProductId))
                    .thenReturn(Optional.of(cardProduct));

            assertThrows(BadRequestException.class,
                    () -> service.update(cardProductId, request));
        }
    }

 // UPDATE STATUS TESTS
    @Nested
    @DisplayName("Update Card Product Status Tests")
    class UpdateStatusTests {

        @Test
        void deactivate_success() {

            when(cardProductRepository.findById(cardProductId))
                    .thenReturn(Optional.of(cardProduct));

            ApiResponse<String> result =
                    service.updateStatus(cardProductId, ProductStatus.INACTIVE);

            assertEquals(200, result.getStatus());

            verify(cardProductRepository).save(cardProduct);
        }

        @Test
        void deactivate_already_inactive() {

            cardProduct.setStatus(ProductStatus.INACTIVE);

            when(cardProductRepository.findById(cardProductId))
                    .thenReturn(Optional.of(cardProduct));

            assertThrows(BadRequestException.class,
                    () -> service.updateStatus(cardProductId, ProductStatus.INACTIVE));
        }

        @Test
        void updateStatus_product_not_found() {

            when(cardProductRepository.findById(cardProductId))
                    .thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> service.updateStatus(cardProductId, ProductStatus.INACTIVE));
        }
        
        @Test
        void activate_success() {

            cardProduct.setStatus(ProductStatus.INACTIVE);

            when(cardProductRepository.findById(cardProductId))
                    .thenReturn(Optional.of(cardProduct));

            ApiResponse<String> result =
                    service.updateStatus(cardProductId, ProductStatus.ACTIVE);

            assertEquals(200, result.getStatus());

            verify(cardProductRepository).save(cardProduct);
        }
    }
}