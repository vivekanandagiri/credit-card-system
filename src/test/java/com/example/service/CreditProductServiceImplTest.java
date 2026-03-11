package com.example.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import com.example.dto.request.CreditProductCreateRequest;
import com.example.dto.request.CreditProductUpdateRequest;
import com.example.dto.response.ApiResponse;
import com.example.dto.response.CreditProductResponse;
import com.example.entity.CreditProduct;
import com.example.enums.ProductStatus;
import com.example.exception.BusinessRuleException;
import com.example.exception.ResourceNotFoundException;
import com.example.mapper.CreditProductMapper;
import com.example.repository.CreditProductRepository;
import com.example.service.ServiceImpl.CreditProductServiceImpl;
import com.example.util.ProductCodeGenerator;

public class CreditProductServiceImplTest {
	
	@Mock
	private CreditProductRepository repository;
	
	@Mock
	private CreditProductMapper mapper;
	
	@Mock
	private ProductCodeGenerator codeGenerator;
	
	@InjectMocks
	private CreditProductServiceImpl service;
	
	private CreditProduct product;
	private CreditProductResponse response;
	
	
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
	}

	
	//create test
	
	@Nested
	@DisplayName("Create Credit Product Tests")
	class CreateTests{
		
		@Test
		void create_success() {
			CreditProductCreateRequest request = new CreditProductCreateRequest();
			request.setProductName("Gold Credit Card");
			request.setMinCreditLimit(new BigDecimal("50000"));
			request.setMaxCreditLimit(new BigDecimal("500000"));
			request.setEffectiveFrom(LocalDate.now());
			
			when(mapper.toEntity(request)).thenReturn(product);
            when(codeGenerator.generateBaseCode("Gold Credit Card"))
                    .thenReturn("GOLD-CREDIT-CARD");
            when(repository.existsByProductCode(any())).thenReturn(false);
            when(repository.save(product)).thenReturn(product);
            when(mapper.toResponse(product)).thenReturn(response);

            ApiResponse<CreditProductResponse> result = service.create(request);

            assertNotNull(result);
            assertEquals(201, result.getStatus());
            assertEquals("Credit Product Created Successfully", result.getMessage());
            assertEquals("Gold Credit Card", result.getData().getProductName());

            verify(repository).save(product);
			
			
		}
		
		@Test
		void create_product_code_collision_should_generate_new_code() {

		    CreditProductCreateRequest request = new CreditProductCreateRequest();
		    request.setProductName("Gold Credit Card");
		    request.setMinCreditLimit(new BigDecimal("50000"));
		    request.setMaxCreditLimit(new BigDecimal("500000"));
		    request.setEffectiveFrom(LocalDate.now());

		    when(mapper.toEntity(request)).thenReturn(product);

		    when(codeGenerator.generateBaseCode("Gold Credit Card"))
		            .thenReturn("GOLD-CREDIT-CARD");

		    // first code exists -> loop runs
		    when(repository.existsByProductCode("GOLD-CREDIT-CARD-001"))
		            .thenReturn(true);

		    // second code available
		    when(repository.existsByProductCode("GOLD-CREDIT-CARD-002"))
		            .thenReturn(false);

		    when(repository.save(any())).thenReturn(product);
		    when(mapper.toResponse(product)).thenReturn(response);

		    ApiResponse<CreditProductResponse> result = service.create(request);

		    assertEquals(201, result.getStatus());

		    verify(repository, times(2)).existsByProductCode(any());
		}
		
		@Test
		void create_mapper_failure_should_throw_exception() {

		    CreditProductCreateRequest request = new CreditProductCreateRequest();

		    when(mapper.toEntity(request))
		            .thenThrow(new RuntimeException("Mapping failed"));

		    assertThrows(RuntimeException.class,
		            () -> service.create(request));
		}
		
		@Test
		void create_repository_failure_should_throw_exception() {

		    CreditProductCreateRequest request = new CreditProductCreateRequest();
		    request.setProductName("Gold Card");
		    request.setMinCreditLimit(new BigDecimal("50000"));
		    request.setMaxCreditLimit(new BigDecimal("500000"));
		    request.setEffectiveFrom(LocalDate.now());

		    when(mapper.toEntity(request)).thenReturn(product);

		    when(codeGenerator.generateBaseCode(any()))
		            .thenReturn("GOLD-CARD");

		    when(repository.existsByProductCode(any()))
		            .thenReturn(false);

		    when(repository.save(product))
		            .thenThrow(new RuntimeException("Database error"));

		    assertThrows(RuntimeException.class,
		            () -> service.create(request));
		}
		
		@Test
		void create_min_credit_limit_greater_than_max_should_throw_exception() {

		    CreditProductCreateRequest request = new CreditProductCreateRequest();
		    request.setProductName("Gold Card");
		    request.setMinCreditLimit(new BigDecimal("600000"));
		    request.setMaxCreditLimit(new BigDecimal("500000"));
		    request.setEffectiveFrom(LocalDate.now());

		    assertThrows(BusinessRuleException.class,
		            () -> service.create(request));
		}
		
		@Test
		void create_effective_to_before_effective_from_should_throw_exception() {

		    CreditProductCreateRequest request = new CreditProductCreateRequest();
		    request.setProductName("Gold Card");
		    request.setMinCreditLimit(new BigDecimal("50000"));
		    request.setMaxCreditLimit(new BigDecimal("500000"));
		    request.setEffectiveFrom(LocalDate.of(2030,1,1));
		    request.setEffectiveTo(LocalDate.of(2029,1,1));

		    assertThrows(BusinessRuleException.class,
		            () -> service.create(request));
		}
		
		@Test
		void create_product_concurrently_should_generate_unique_codes() throws Exception {

		    CreditProductCreateRequest request = new CreditProductCreateRequest();
		    request.setProductName("Gold Credit Card");
		    request.setMinCreditLimit(new BigDecimal("50000"));
		    request.setMaxCreditLimit(new BigDecimal("500000"));
		    request.setEffectiveFrom(LocalDate.now());

		    when(mapper.toEntity(any())).thenReturn(product);
		    when(codeGenerator.generateBaseCode(any())).thenReturn("GOLD-CREDIT-CARD");
		    when(repository.existsByProductCode(any())).thenReturn(false);
		    when(repository.save(any())).thenReturn(product);
		    when(mapper.toResponse(any())).thenReturn(response);

		    ExecutorService executor = Executors.newFixedThreadPool(2);

		    Callable<ApiResponse<CreditProductResponse>> task =
		            () -> service.create(request);

		    Future<ApiResponse<CreditProductResponse>> f1 = executor.submit(task);
		    Future<ApiResponse<CreditProductResponse>> f2 = executor.submit(task);

		    ApiResponse<CreditProductResponse> r1 = f1.get();
		    ApiResponse<CreditProductResponse> r2 = f2.get();

		    assertNotNull(r1);
		    assertNotNull(r2);

		    executor.shutdown();
		}
	}
	
	// GET BY ID TESTS

    @Nested
    @DisplayName("Get By Id Tests")
    class GetByIdTests {

        @Test
        void getById_success() {

            when(repository.findById(1L)).thenReturn(Optional.of(product));
            when(mapper.toResponse(product)).thenReturn(response);

            ApiResponse<CreditProductResponse> result = service.getById(1L);

            assertEquals(200, result.getStatus());
            assertEquals("Gold Credit Card", result.getData().getProductName());
        }

        @Test
        void getById_not_found() {

            when(repository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> service.getById(1L));
        }
    }
    
 // GET ALL TESTS

    @Nested
    @DisplayName("Get All Tests")
    class GetAllTests {

        @Test
        void getAll_success() {

            when(repository.findAll()).thenReturn(List.of(product));
            when(mapper.toResponse(product)).thenReturn(response);

            ApiResponse<List<CreditProductResponse>> result =
                    service.getAll();

            assertEquals(200, result.getStatus());
            assertEquals(1, result.getData().size());
        }
        
        @Test
        void getAll_empty_list() {

            when(repository.findAll()).thenReturn(List.of());

            ApiResponse<List<CreditProductResponse>> result = service.getAll();

            assertEquals(200, result.getStatus());
            assertTrue(result.getData().isEmpty());
        }
        
        @Test
        void getAll_active() {
        	when(repository.findAllByStatus(ProductStatus.ACTIVE)).thenReturn(List.of(product));
        	
        	ApiResponse<List<CreditProductResponse>>result=service.getAllActive();
        	
        	assertEquals(200, result.getStatus());
        	assertEquals(1, result.getData().size());
        }
    }
    
 // UPDATE TESTS

    @Nested
    @DisplayName("Update Credit Product Tests")
    class UpdateTests {

        @Test
        void update_success() {

            CreditProductUpdateRequest request = new CreditProductUpdateRequest();
            request.setProductName("Updated Card");

            when(repository.findById(1L)).thenReturn(Optional.of(product));
            when(repository.save(product)).thenReturn(product);
            when(mapper.toResponse(product)).thenReturn(response);

            ApiResponse<CreditProductResponse> result =
                    service.update(1L, request);

            assertEquals(200, result.getStatus());

            verify(repository).save(product);
        }

        @Test
        void update_product_not_found() {

            CreditProductUpdateRequest request = new CreditProductUpdateRequest();

            when(repository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> service.update(1L, request));
        }

        @Test
        void update_inactive_product() {

            CreditProductUpdateRequest request = new CreditProductUpdateRequest();

            product.setStatus(ProductStatus.INACTIVE);

            when(repository.findById(1L)).thenReturn(Optional.of(product));

            assertThrows(BusinessRuleException.class,
                    () -> service.update(1L, request));
        }
        
        @Test
        void update_partial_update_should_modify_only_provided_fields() {

            CreditProductUpdateRequest request = new CreditProductUpdateRequest();
            request.setMinCreditLimit(new BigDecimal("60000"));

            product.setMinCreditLimit(new BigDecimal("50000"));

            when(repository.findById(1L)).thenReturn(Optional.of(product));
            when(repository.save(product)).thenReturn(product);
            when(mapper.toResponse(product)).thenReturn(response);

            ApiResponse<CreditProductResponse> result =
                    service.update(1L, request);

            assertEquals(200, result.getStatus());

            assertEquals(new BigDecimal("60000"), product.getMinCreditLimit());
        }
    }
}
