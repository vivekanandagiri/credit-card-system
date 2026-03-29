package com.example.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.mockito.*;

import com.example.dto.request.CreditProductCreateRequest;
import com.example.dto.request.CreditProductUpdateRequest;
import com.example.dto.response.CreditProductCreateResponse;
import com.example.dto.response.CreditProductResponse;
import com.example.entity.CreditProduct;
import com.example.enums.ProductStatus;
import com.example.exception.BusinessRuleException;
import com.example.exception.ResourceNotFoundException;
import com.example.mapper.CreditProductMapper;
import com.example.repository.CreditProductRepository;
import com.example.service.ServiceImpl.CreditProductServiceImpl;
import com.example.util.ProductCodeGenerator;

class CreditProductServiceImplTest {

    @Mock private CreditProductRepository repository;
    @Mock private CreditProductMapper mapper;
    @Mock private ProductCodeGenerator codeGenerator;

    @InjectMocks
    private CreditProductServiceImpl service;

    private CreditProduct product;
    private CreditProductResponse response;
    private CreditProductCreateResponse createResponse;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        product = new CreditProduct();
        product.setCreditProductId(1L);
        product.setProductName("Gold Credit Card");
        product.setProductCode("GOLD-CREDIT-CARD-001");
        product.setStatus(ProductStatus.ACTIVE);

        response = new CreditProductResponse();
        response.setCreditProductId(1L);
        response.setProductName("Gold Credit Card");

        createResponse = new CreditProductCreateResponse(
                1L,
                "GOLD-CREDIT-CARD-001",
                "Gold Credit Card",
                ProductStatus.ACTIVE
        );
    }

    // ================= CREATE =================

    @Nested
    class CreateTests {

        @Test
        void create_success() {

            CreditProductCreateRequest request = new CreditProductCreateRequest();
            request.setProductName("Gold Credit Card");
            request.setMinCreditLimit(new BigDecimal("50000"));
            request.setMaxCreditLimit(new BigDecimal("500000"));
            request.setEffectiveFrom(LocalDate.now().plusDays(1));

            when(mapper.toEntity(request)).thenReturn(product);
            when(codeGenerator.generateBaseCode(any())).thenReturn("GOLD-CREDIT-CARD");
            when(repository.existsByProductCode(any())).thenReturn(false);
            when(repository.save(product)).thenReturn(product);
            when(mapper.toCreateResponse(product)).thenReturn(createResponse);

            CreditProductCreateResponse result = service.create(request);

            assertNotNull(result);
            assertEquals("Gold Credit Card", result.getProductName());
            verify(repository).save(product);
        }

        @Test
        void create_product_code_collision_should_generate_new_code() {

            CreditProductCreateRequest request = new CreditProductCreateRequest();
            request.setProductName("Gold Credit Card");

            when(mapper.toEntity(request)).thenReturn(product);
            when(codeGenerator.generateBaseCode(any())).thenReturn("CODE");

            when(repository.existsByProductCode("CODE-001")).thenReturn(true);
            when(repository.existsByProductCode("CODE-002")).thenReturn(false);

            when(repository.save(any())).thenReturn(product);
            when(mapper.toCreateResponse(product)).thenReturn(createResponse);

            CreditProductCreateResponse result = service.create(request);

            assertNotNull(result);
            verify(repository, times(2)).existsByProductCode(any());
        }

        @Test
        void create_min_credit_limit_greater_than_max_should_throw() {

            CreditProductCreateRequest request = new CreditProductCreateRequest();
            request.setMinCreditLimit(new BigDecimal("600000"));
            request.setMaxCreditLimit(new BigDecimal("500000"));

            assertThrows(BusinessRuleException.class,
                    () -> service.create(request));
        }
    }

    // ================= GET BY ID =================

    @Nested
    class GetByIdTests {

        @Test
        void getById_success() {

            when(repository.findById(1L)).thenReturn(Optional.of(product));
            when(mapper.toResponse(product)).thenReturn(response);

            CreditProductResponse result = service.getById(1L);

            assertEquals("Gold Credit Card", result.getProductName());
        }

        @Test
        void getById_not_found() {

            when(repository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> service.getById(1L));
        }
    }

    // ================= GET ALL =================

    @Nested
    class GetAllTests {

        @Test
        void getAll_success() {

            when(repository.findAll()).thenReturn(List.of(product));
            when(mapper.toResponse(product)).thenReturn(response);

            List<CreditProductResponse> result = service.getAll();

            assertEquals(1, result.size());
        }

        @Test
        void getAll_empty() {

            when(repository.findAll()).thenReturn(List.of());

            List<CreditProductResponse> result = service.getAll();

            assertTrue(result.isEmpty());
        }
    }

    // ================= UPDATE =================

    @Nested
    class UpdateTests {

        @Test
        void update_success() {

            CreditProductUpdateRequest request = new CreditProductUpdateRequest();
            request.setProductName("Updated Card");

            when(repository.findById(1L)).thenReturn(Optional.of(product));
            when(repository.save(product)).thenReturn(product);
            when(mapper.toResponse(product)).thenReturn(response);

            CreditProductResponse result = service.update(1L, request);

            assertNotNull(result);
            verify(repository).save(product);
        }

        @Test
        void update_not_found() {

            when(repository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> service.update(1L, new CreditProductUpdateRequest()));
        }

        @Test
        void update_inactive_should_throw() {

            product.setStatus(ProductStatus.INACTIVE);
            when(repository.findById(1L)).thenReturn(Optional.of(product));

            assertThrows(BusinessRuleException.class,
                    () -> service.update(1L, new CreditProductUpdateRequest()));
        }
    }
}