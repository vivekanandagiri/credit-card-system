package com.example.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import com.example.dto.request.*;
import com.example.dto.response.*;
import com.example.entity.CreditCardProduct;
import com.example.enums.ProductStatus;
import com.example.exception.*;
import com.example.mapper.CardProductMapper;
import com.example.repository.CreditCardProductRepository;
import com.example.service.ServiceImpl.CardProductServiceImpl;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class CardProductServiceImplTest {

    @Mock private CreditCardProductRepository repository;
    @Mock private CardProductMapper mapper;

    @InjectMocks
    private CardProductServiceImpl service;

    private UUID id;
    private CreditCardProduct entity;
    private CardProductCreateRequest createRequest;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();

        entity = new CreditCardProduct();
        entity.setStatus(ProductStatus.ACTIVE);

        createRequest = new CardProductCreateRequest();
        createRequest.setStatementCycleDay(10);
        createRequest.setAtmWithdrawalAllowed(true);
        createRequest.setOnlineTransactionsAllowed(true);
    }

    // ================= CREATE =================

    @Test
    void shouldCreate_success() {
        when(mapper.toEntity(createRequest)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toCardProductSummaryResponse(entity))
                .thenReturn(new CardProductCreateResponse());

        CardProductCreateResponse res = service.create(createRequest);

        assertThat(res).isNotNull();
    }

    @Test
    void shouldThrow_whenInvalidStatementCycle() {
        createRequest.setStatementCycleDay(40);

        assertThatThrownBy(() -> service.create(createRequest))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void shouldThrow_whenNegativeAnnualFee() {
        createRequest.setAnnualFee(BigDecimal.valueOf(-1));

        assertThatThrownBy(() -> service.create(createRequest))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void shouldThrow_whenNegativeAtmLimit() {
        createRequest.setAtmDailyLimit(BigDecimal.valueOf(-1));

        assertThatThrownBy(() -> service.create(createRequest))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void shouldThrow_whenNegativePosLimit() {
        createRequest.setPosDailyLimit(BigDecimal.valueOf(-1));

        assertThatThrownBy(() -> service.create(createRequest))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void shouldThrow_whenNegativeEcommerceLimit() {
        createRequest.setEcommerceDailyLimit(BigDecimal.valueOf(-1));

        assertThatThrownBy(() -> service.create(createRequest))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void shouldThrow_whenForexAbove100() {
        createRequest.setForexMarkupPercent(new BigDecimal("101"));

        assertThatThrownBy(() -> service.create(createRequest))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void shouldThrow_whenAtmDisabledButLimitProvided() {
        createRequest.setAtmWithdrawalAllowed(false);
        createRequest.setAtmDailyLimit(BigDecimal.TEN);

        assertThatThrownBy(() -> service.create(createRequest))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void shouldThrow_whenOnlineDisabledButLimitProvided() {
        createRequest.setOnlineTransactionsAllowed(false);
        createRequest.setEcommerceDailyLimit(BigDecimal.TEN);

        assertThatThrownBy(() -> service.create(createRequest))
                .isInstanceOf(BadRequestException.class);
    }

    // ================= GET =================

    @Test
    void shouldGetById_success() {
        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(mapper.toResponse(entity)).thenReturn(new CardProductResponse());

        CardProductResponse res = service.getById(id);

        assertThat(res).isNotNull();
    }

    @Test
    void shouldThrow_whenNotFound() {
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ================= GET ALL =================

    @Test
    void shouldGetAll_withStatus() {
        when(repository.findAllByStatus(ProductStatus.ACTIVE))
                .thenReturn(List.of(entity));
        when(mapper.toResponse(entity)).thenReturn(new CardProductResponse());

        List<CardProductResponse> res =
                service.getAll(ProductStatus.ACTIVE);

        assertThat(res).hasSize(1);
    }

    @Test
    void shouldGetAll_withoutStatus() {
        when(repository.findAll()).thenReturn(List.of(entity));
        when(mapper.toResponse(entity)).thenReturn(new CardProductResponse());

        List<CardProductResponse> res =
                service.getAll(null);

        assertThat(res).hasSize(1);
    }

    @Test
    void shouldGetAllActive() {
        when(repository.findAllByStatus(ProductStatus.ACTIVE))
                .thenReturn(List.of(entity));
        when(mapper.toResponse(entity)).thenReturn(new CardProductResponse());

        List<CardProductResponse> res =
                service.getAllActive();

        assertThat(res).hasSize(1);
    }

    // ================= UPDATE =================

    @Test
    void shouldUpdate_success() {
        CardProductUpdateRequest req = new CardProductUpdateRequest();
        req.setProductName("New");

        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toResponse(entity)).thenReturn(new CardProductResponse());

        CardProductResponse res = service.update(id, req);

        assertThat(res).isNotNull();
    }

    @Test
    void shouldThrow_whenNoFieldsProvided() {
        CardProductUpdateRequest req = new CardProductUpdateRequest();

        when(repository.findById(id)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.update(id, req))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void shouldThrow_whenSameStatus() {
        CardProductUpdateRequest req = new CardProductUpdateRequest();
        req.setStatus(ProductStatus.ACTIVE);

        when(repository.findById(id)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.update(id, req))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void shouldThrow_whenInactiveAndNoStatusChange() {
        entity.setStatus(ProductStatus.INACTIVE);

        CardProductUpdateRequest req = new CardProductUpdateRequest();
        req.setProductName("test");

        when(repository.findById(id)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.update(id, req))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void shouldValidateFeatureRules_onUpdate() {
        CardProductUpdateRequest req = new CardProductUpdateRequest();
        req.setAtmWithdrawalAllowed(false);
        req.setAtmDailyLimit(BigDecimal.TEN);

        when(repository.findById(id)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.update(id, req))
                .isInstanceOf(BadRequestException.class);
    }

    // ================= ACTIVE ENTITY =================

    @Test
    void shouldReturnActiveEntity() {
        entity.setStatus(ProductStatus.ACTIVE);
        when(repository.findById(id)).thenReturn(Optional.of(entity));

        CreditCardProduct res =
                service.getActiveCardProductEntity(id);

        assertThat(res).isNotNull();
    }

    @Test
    void shouldThrow_whenInactiveEntity() {
        entity.setStatus(ProductStatus.INACTIVE);
        when(repository.findById(id)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() ->
                service.getActiveCardProductEntity(id))
                .isInstanceOf(BadRequestException.class);
    }
}