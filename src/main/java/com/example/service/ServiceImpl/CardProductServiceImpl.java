package com.example.service.ServiceImpl;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.example.dto.request.CardProductCreateRequest;
import com.example.dto.request.CardProductUpdateRequest;
import com.example.dto.response.ApiResponse;
import com.example.dto.response.CardProductResponse;
import com.example.entity.CreditCardProduct;
import com.example.entity.CreditProduct;
import com.example.enums.ProductStatus;
import com.example.exception.BadRequestException;
import com.example.exception.ConflictException;
import com.example.exception.ResourceNotFoundException;
import com.example.mapper.CardProductMapper;
import com.example.repository.CreditCardProductRepository;
import com.example.repository.CreditProductRepository;
import com.example.service.CardProductService;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class CardProductServiceImpl implements CardProductService {

    private final CreditCardProductRepository cardProductRepository;
    private final CreditProductRepository creditProductRepository;
    private final CardProductMapper mapper;

    public CardProductServiceImpl(
            CreditCardProductRepository cardProductRepository,
            CreditProductRepository creditProductRepository,
            CardProductMapper mapper) {

        this.cardProductRepository = cardProductRepository;
        this.creditProductRepository = creditProductRepository;
        this.mapper = mapper;
    }

    // CREATE CARD PRODUCT
    @Override
    public ApiResponse<CardProductResponse> create(CardProductCreateRequest request) {

        CreditProduct creditProduct = creditProductRepository
                .findById(request.getCreditProductId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Credit Product with id " + request.getCreditProductId() + " not found"));

        // Credit product must be active
        if (creditProduct.getStatus() == ProductStatus.INACTIVE) {
            throw new BadRequestException(
                    "Cannot create card product under an inactive credit product");
        }

        // Duplicate card product check
        if (cardProductRepository
                .existsByProductNameAndCreditProductCreditProductId(
                        request.getProductName(),
                        request.getCreditProductId())) {

            throw new ConflictException(
                    "Card product '" + request.getProductName()
                            + "' already exists for this credit product");
        }

        validateStatementCycle(request.getStatementCycleDay());
        validateFeatureRules(
                request.getAtmWithdrawalAllowed(),
                request.getAtmDailyLimit(),
                request.getOnlineTransactionsAllowed(),
                request.getEcommerceDailyLimit()
        );
        validateFinancialLimits(request);

        CreditCardProduct cardProduct =
                mapper.toEntity(request, creditProduct);

        CardProductResponse response =
                mapper.toResponse(cardProductRepository.save(cardProduct));

        return new ApiResponse<>(
                Instant.now(),
                HttpStatus.CREATED.value(),
                "Card product created successfully",
                response
        );
    }

    // GET BY ID
    @Override
    public ApiResponse<CardProductResponse> getById(UUID id) {

        CardProductResponse response =
                mapper.toResponse(findById(id));

        return new ApiResponse<>(
                Instant.now(),
                HttpStatus.OK.value(),
                "Credit Card product fetched successfully",
                response
        );
    }

    // GET ALL
    @Override
    public ApiResponse<List<CardProductResponse>> getAll() {

        List<CardProductResponse> list = cardProductRepository.findAll()
                .stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());

        return new ApiResponse<>(
                Instant.now(),
                HttpStatus.OK.value(),
                "Credit card products fetched successfully",
                list
        );
    }

    // GET ALL ACTIVE
    @Override
    public ApiResponse<List<CardProductResponse>> getAllActive() {

        List<CardProductResponse> list = cardProductRepository
                .findAllByStatus(ProductStatus.ACTIVE)
                .stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());

        return new ApiResponse<>(
                Instant.now(),
                HttpStatus.OK.value(),
                "Active card products fetched successfully",
                list
        );
    }

    // GET BY CREDIT PRODUCT
    @Override
    public ApiResponse<List<CardProductResponse>> getByCreditProduct(Long creditProductId) {

        List<CardProductResponse> list =
                cardProductRepository
                        .findAllByCreditProductCreditProductId(creditProductId)
                        .stream()
                        .map(mapper::toResponse)
                        .collect(Collectors.toList());

        return new ApiResponse<>(
                Instant.now(),
                HttpStatus.OK.value(),
                "Card products fetched for credit product " + creditProductId,
                list
        );
    }

    // UPDATE CARD PRODUCT
    @Override
    public ApiResponse<CardProductResponse> update(UUID id, CardProductUpdateRequest request) {

        CreditCardProduct card = findById(id);

        if (card.getStatus() == ProductStatus.INACTIVE) {
            throw new BadRequestException(
                    "Cannot update an inactive card product");
        }

        validateUpdateRequest(request);

        validateStatementCycle(request.getStatementCycleDay());

        validateFeatureRules(
                request.getAtmWithdrawalAllowed(),
                request.getAtmDailyLimit(),
                request.getOnlineTransactionsAllowed(),
                request.getEcommerceDailyLimit()
        );

        mapper.updateEntity(request, card);

        CardProductResponse response =
                mapper.toResponse(cardProductRepository.save(card));

        return new ApiResponse<>(
                Instant.now(),
                HttpStatus.OK.value(),
                "Card product updated successfully",
                response
        );
    }

    // DEACTIVATE CARD PRODUCT
    @Override
    public ApiResponse<String> deactivate(UUID id) {

        CreditCardProduct card = findById(id);

        if (card.getStatus() == ProductStatus.INACTIVE) {
            throw new BadRequestException(
                    "Card product is already inactive");
        }

        card.setStatus(ProductStatus.INACTIVE);

        cardProductRepository.save(card);

        return new ApiResponse<>(
                Instant.now(),
                HttpStatus.OK.value(),
                "Card product deactivated successfully",
                "Deactivated"
        );
    }

    // FIND BY ID
    private CreditCardProduct findById(UUID id) {

        return cardProductRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Credit Card product with id " + id + " not found"));
    }


    // VALIDATIONS

    private void validateStatementCycle(Integer cycleDay) {

        if (cycleDay != null && (cycleDay < 1 || cycleDay > 28)) {
            throw new BadRequestException(
                    "Statement cycle day must be between 1 and 28");
        }
    }

    private void validateFinancialLimits(CardProductCreateRequest request) {

        if (request.getAnnualFee() != null
                && request.getAnnualFee().signum() < 0) {
            throw new BadRequestException(
                    "Annual fee cannot be negative");
        }

        if (request.getAtmDailyLimit() != null
                && request.getAtmDailyLimit().signum() < 0) {
            throw new BadRequestException(
                    "ATM daily limit cannot be negative");
        }

        if (request.getPosDailyLimit() != null
                && request.getPosDailyLimit().signum() < 0) {
            throw new BadRequestException(
                    "POS daily limit cannot be negative");
        }

        if (request.getEcommerceDailyLimit() != null
                && request.getEcommerceDailyLimit().signum() < 0) {
            throw new BadRequestException(
                    "E-commerce daily limit cannot be negative");
        }

        if (request.getForexMarkupPercent() != null
                && request.getForexMarkupPercent()
                .compareTo(new BigDecimal("100")) > 0) {

            throw new BadRequestException(
                    "Forex markup cannot exceed 100%");
        }
    }
    
    private void validateFeatureRules(
            Boolean atmWithdrawalAllowed,
            BigDecimal atmDailyLimit,
            Boolean onlineTransactionsAllowed,
            BigDecimal ecommerceDailyLimit) {

        if (Boolean.FALSE.equals(atmWithdrawalAllowed) && atmDailyLimit != null) {
            throw new BadRequestException(
                    "ATM limit cannot be set when ATM withdrawals are disabled");
        }

        if (Boolean.FALSE.equals(onlineTransactionsAllowed) && ecommerceDailyLimit != null) {
            throw new BadRequestException(
                    "E-commerce limit cannot be set when online transactions are disabled");
        }
    }

    private void validateUpdateRequest(CardProductUpdateRequest request) {

        if (request.getProductName() == null &&
                request.getNetworkType() == null &&
                request.getCardType() == null &&
                request.getAnnualFee() == null &&
                request.getCardValidityYears() == null &&
                request.getContactlessEnabled() == null &&
                request.getInternationalUsageAllowed() == null &&
                request.getOnlineTransactionsAllowed() == null &&
                request.getAtmWithdrawalAllowed() == null &&
                request.getAtmDailyLimit() == null &&
                request.getPosDailyLimit() == null &&
                request.getEcommerceDailyLimit() == null &&
                request.getStatementCycleDay() == null &&
                request.getForexMarkupPercent() == null &&
                request.getProductDescription() == null) {

            throw new BadRequestException(
                    "At least one field must be provided for update");
        }
    }
}