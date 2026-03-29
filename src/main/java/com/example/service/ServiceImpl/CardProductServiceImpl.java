package com.example.service.ServiceImpl;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

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
import com.example.service.CardProductService;
import com.example.service.CreditProductService;

import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link CardProductService} managing credit card product lifecycle.
 * CreditCardProduct is fully independent of CreditProduct.
 * No credit product lookup during creation or updates.
 * Admin manages card products as a standalone catalog.
 */

@Service
@Transactional
public class CardProductServiceImpl implements CardProductService {
	
    private static final int STATEMENT_CYCLE_DAY_MIN = 1;
    private static final int STATEMENT_CYCLE_DAY_MAX = 28;

    private final CreditCardProductRepository cardProductRepository;          
    private final CardProductMapper cardProductMapper;
 
    public CardProductServiceImpl(
            CreditCardProductRepository cardProductRepository,
            CreditProductService creditProductService,
            CardProductMapper cardProductMapper) {
        this.cardProductRepository = cardProductRepository;
        this.cardProductMapper = cardProductMapper;
    }

    /**
     * Creates a new card product under an existing active credit product.
     *
     * @param request creation payload
     * @return API response containing the created card product details
     */
    
    @Override
    public CardProductCreateResponse create(CardProductCreateRequest request) {

        validateStatementCycleDay(request.getStatementCycleDay());
        validateFeatureRules(
                request.getAtmWithdrawalAllowed(),
                request.getAtmDailyLimit(),
                request.getOnlineTransactionsAllowed(),
                request.getEcommerceDailyLimit()
        );
        validateFinancialLimits(request);

        CreditCardProduct cardProduct =
        		cardProductMapper.toEntity(request);

        CreditCardProduct saved = cardProductRepository.save(cardProduct);

           
        return cardProductMapper.toCardProductSummaryResponse(saved);
    }

    /**
     * Retrieves a single card product by its ID.
     */
    @Override
    public CardProductResponse getById(UUID id) {

        CardProductResponse response =
        		cardProductMapper.toResponse(findById(id));

        return response;
    }

    /**
     * Retrieves all card products, optionally filtered by status.
     *
     * @param status optional filter; returns all products when null
     */
    @Override
    public List<CardProductResponse> getAll(ProductStatus status) {
    	
    	List<CardProductResponse> list;
    	
    	if (status != null) {
            list = cardProductRepository.findAllByStatus(status)
                    .stream()
                    .map(cardProductMapper::toResponse)
                    .collect(Collectors.toList());
        } else {
            list = cardProductRepository.findAll()
                    .stream()
                    .map(cardProductMapper::toResponse)
                    .collect(Collectors.toList());
        }

        return list;
    }

    /**
     * Retrieves all active card products.
     */
    @Override
    @Transactional(readOnly = true)
    public List<CardProductResponse> getAllActive() {
 
        List<CardProductResponse> list =
                cardProductRepository.findAllByStatus(ProductStatus.ACTIVE)
                        .stream()
                        .map(cardProductMapper::toResponse)
                        .collect(Collectors.toList());
 
        return list;
    }

    /**
     * Updates an active card product. At least one field must be provided.
     */
    @Override
    public CardProductResponse update(UUID id, CardProductUpdateRequest request) {

        CreditCardProduct card = findById(id);

        if (card.getStatus() == ProductStatus.INACTIVE) {
            throw new BadRequestException(
                    "Cannot update an inactive card product");
        }

        validateUpdateRequest(request);

        validateStatementCycleDay(request.getStatementCycleDay());

        validateFeatureRules(
                request.getAtmWithdrawalAllowed(),
                request.getAtmDailyLimit(),
                request.getOnlineTransactionsAllowed(),
                request.getEcommerceDailyLimit()
        );

        cardProductMapper.updateEntity(request, card);

        CardProductResponse response =
                cardProductMapper.toResponse(cardProductRepository.save(card));

        return response;
    }

    /**
     * Activates or deactivates a card product.
     *
     * @param id     card product ID
     * @param status new target status
     */
    @Override
    public String updateStatus(UUID id, ProductStatus status) {

        CreditCardProduct card = findById(id);

        if (card.getStatus() == status) {
            throw new BadRequestException(
                    "Card product is already " + status.name().toLowerCase());
        }

        card.setStatus(status);
        cardProductRepository.save(card);

        return status.name();
    }
    
    /**
     * {@inheritDoc}
     *
     * <p>Used by {@link CreditCardServiceImpl} to resolve the card product entity
     * without injecting the card product repository.
     */
    @Override
    @Transactional(readOnly = true)
    public CreditCardProduct getActiveCardProductEntity(UUID cardProductId) {
        CreditCardProduct product = findById(cardProductId);
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new BadRequestException("Card product is not active");
        }
        return product;
    }
    

    //------------------Private Helpers ----------------

    /**
     * Extract Credit Card Product By id
     * @param id
     * @return
     */
    private CreditCardProduct findById(UUID id) {

        return cardProductRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Credit Card product with id " + id + " not found"));
    }
    /**
     * Statement Cycle validation
     * @param cycleDay
     */
    private void validateStatementCycleDay(Integer cycleDay) {
        if (cycleDay != null && (cycleDay < STATEMENT_CYCLE_DAY_MIN || cycleDay > STATEMENT_CYCLE_DAY_MAX)) {
            throw new BadRequestException("Statement cycle day must be between 1 and 28");
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