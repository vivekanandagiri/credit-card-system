package com.example.service;

import com.example.dto.request.CardProductCreateRequest;
import com.example.dto.request.CardProductUpdateRequest;
import com.example.dto.response.CardProductCreateResponse;
import com.example.dto.response.CardProductResponse;
import com.example.entity.CreditCardProduct;
import com.example.enums.ProductStatus;
import com.example.exception.BadRequestException;
import com.example.exception.ResourceNotFoundException;
import com.example.mapper.CardProductMapper;
import com.example.repository.CreditCardProductRepository;
import com.example.service.ServiceImpl.CardProductServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardProductServiceImplTest {

    @Mock
    private CreditCardProductRepository repository;

    @Mock
    private CardProductMapper mapper;

    @InjectMocks
    private CardProductServiceImpl service;

    private UUID id;
    private CreditCardProduct entity;
    private CardProductCreateRequest createRequest;
    private CardProductUpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();

        entity = new CreditCardProduct();
        entity.setStatus(ProductStatus.ACTIVE);

        createRequest = new CardProductCreateRequest();
        createRequest.setStatementCycleDay(10);

        updateRequest = new CardProductUpdateRequest();
        updateRequest.setProductName("Updated Name");
    }

    // ---------------- CREATE ----------------
    @Nested
    class CreateTests {

        @Test
        void shouldCreateCardProductSuccessfully() {
            when(mapper.toEntity(createRequest)).thenReturn(entity);
            when(repository.save(entity)).thenReturn(entity);
            when(mapper.toCardProductSummaryResponse(entity))
                    .thenReturn(new CardProductCreateResponse());

            CardProductCreateResponse result = service.create(createRequest);

            assertThat(result).isNotNull();
            verify(repository).save(entity);
        }

        @Test
        void shouldThrowException_whenStatementCycleInvalid() {
            createRequest.setStatementCycleDay(40);

            assertThrows(BadRequestException.class,
                    () -> service.create(createRequest));
        }
    }

    // ---------------- GET BY ID ----------------
    @Nested
    class GetByIdTests {

        @Test
        void shouldReturnProduct_whenFound() {
            when(repository.findById(id)).thenReturn(Optional.of(entity));
            when(mapper.toResponse(entity)).thenReturn(new CardProductResponse());

            CardProductResponse result = service.getById(id);

            assertThat(result).isNotNull();
        }

        @Test
        void shouldThrowException_whenNotFound() {
            when(repository.findById(id)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> service.getById(id));
        }
    }

    // ---------------- GET ALL ----------------
    @Nested
    class GetAllTests {

        @Test
        void shouldReturnAllProducts_whenStatusNull() {
            when(repository.findAll()).thenReturn(List.of(entity));
            when(mapper.toResponse(entity)).thenReturn(new CardProductResponse());

            List<CardProductResponse> result = service.getAll(null);

            assertThat(result).hasSize(1);
        }

        @Test
        void shouldReturnFilteredProducts_whenStatusProvided() {
            when(repository.findAllByStatus(ProductStatus.ACTIVE))
                    .thenReturn(List.of(entity));
            when(mapper.toResponse(entity)).thenReturn(new CardProductResponse());

            List<CardProductResponse> result =
                    service.getAll(ProductStatus.ACTIVE);

            assertThat(result).hasSize(1);
        }
    }

    // ---------------- UPDATE ----------------
    @Nested
    class UpdateTests {

        @Test
        void shouldUpdateSuccessfully() {
            when(repository.findById(id)).thenReturn(Optional.of(entity));
            when(repository.save(entity)).thenReturn(entity);
            when(mapper.toResponse(entity)).thenReturn(new CardProductResponse());

            CardProductResponse result =
                    service.update(id, updateRequest);

            assertThat(result).isNotNull();
            verify(repository).save(entity);
        }

        @Test
        void shouldThrowException_whenProductInactive() {
            entity.setStatus(ProductStatus.INACTIVE);
            when(repository.findById(id)).thenReturn(Optional.of(entity));

            assertThrows(BadRequestException.class,
                    () -> service.update(id, updateRequest));
        }
    }

    // ---------------- UPDATE STATUS ----------------
    @Nested
    class UpdateStatusTests {

        @Test
        void shouldUpdateStatusSuccessfully() {
            entity.setStatus(ProductStatus.INACTIVE);
            when(repository.findById(id)).thenReturn(Optional.of(entity));

            String result =
                    service.updateStatus(id, ProductStatus.ACTIVE);

            assertThat(result).isEqualTo("ACTIVE");
            verify(repository).save(entity);
        }

        @Test
        void shouldThrowException_whenSameStatus() {
            entity.setStatus(ProductStatus.ACTIVE);
            when(repository.findById(id)).thenReturn(Optional.of(entity));

            assertThrows(BadRequestException.class,
                    () -> service.updateStatus(id, ProductStatus.ACTIVE));
        }
    }

    // ---------------- GET ACTIVE ENTITY ----------------
    @Nested
    class GetActiveEntityTests {

        @Test
        void shouldReturnEntity_whenActive() {
            entity.setStatus(ProductStatus.ACTIVE);
            when(repository.findById(id)).thenReturn(Optional.of(entity));

            CreditCardProduct result =
                    service.getActiveCardProductEntity(id);

            assertThat(result).isSameAs(entity);
        }

        @Test
        void shouldThrowException_whenInactive() {
            entity.setStatus(ProductStatus.INACTIVE);
            when(repository.findById(id)).thenReturn(Optional.of(entity));

            assertThrows(BadRequestException.class,
                    () -> service.getActiveCardProductEntity(id));
        }
    }
}