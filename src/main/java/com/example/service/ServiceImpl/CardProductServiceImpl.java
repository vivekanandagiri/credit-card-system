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
import lombok.RequiredArgsConstructor;

import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link CardProductService} responsible for managing
 * the lifecycle of credit card products.
 *
 * <p><b>Key Characteristics:</b></p>
 * <ul>
 *     <li>Card products are managed as an independent catalog</li>
 *     <li>No dependency on credit product during creation/update</li>
 *     <li>Supports creation, retrieval, update, and activation validation</li>
 * </ul>
 *
 * <p><b>Business Rules:</b></p>
 * <ul>
 *     <li>Statement cycle day must be between 1 and 28</li>
 *     <li>Financial limits must be non-negative</li>
 *     <li>Feature flags must align with limits (e.g., ATM disabled → no ATM limit)</li>
 *     <li>Inactive products cannot be modified unless reactivated</li>
 * </ul>
 */
@Service
@Transactional
@RequiredArgsConstructor
public class CardProductServiceImpl implements CardProductService {
	
    private static final int STATEMENT_CYCLE_DAY_MIN = 1;
    private static final int STATEMENT_CYCLE_DAY_MAX = 28;

    private final CreditCardProductRepository cardProductRepository;          
    private final CardProductMapper cardProductMapper;
 
    
    /**
     * Creates a new credit card product.
     *
     * <p>Validations performed:</p>
     * <ul>
     *     <li>Statement cycle day range</li>
     *     <li>Feature compatibility (ATM/online flags vs limits)</li>
     *     <li>Financial constraints (non-negative values, forex limits)</li>
     * </ul>
     *
     * @param request creation payload
     * @return {@link CardProductCreateResponse} with created product details
     * @throws BadRequestException if validation fails
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
     * Retrieves a card product by its unique identifier.
     *
     * @param id product ID
     * @return {@link CardProductResponse}
     * @throws ResourceNotFoundException if product not found
     */
    @Override
    public CardProductResponse getById(UUID id) {

        CardProductResponse response =
        		cardProductMapper.toResponse(findById(id));

        return response;
    }

    /**
     * Retrieves all card products optionally filtered by status.
     *
     * @param status product status filter (nullable)
     * @return list of {@link CardProductResponse}
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
     *
     * @return list of active products
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
     * Updates an existing card product.
     *
     * <p>Rules:</p>
     * <ul>
     *     <li>At least one field must be provided</li>
     *     <li>Inactive products cannot be modified unless status is changed</li>
     *     <li>Status change must not be redundant</li>
     * </ul>
     *
     * @param id      product ID
     * @param request update payload
     * @return updated {@link CardProductResponse}
     * @throws BadRequestException if validation fails
     * @throws ResourceNotFoundException if product not found
     */
    @Override
    public CardProductResponse update(UUID id, CardProductUpdateRequest request) {

        CreditCardProduct card = findById(id);
        //  Handle status update 
        if (request.getStatus() != null) {
            if (card.getStatus() == request.getStatus()) {
                throw new BadRequestException(
                        "Card product is already " + request.getStatus().name().toLowerCase());
            }
            card.setStatus(request.getStatus());
        }

        //  Prevent updating inactive product unless activating it
        if (card.getStatus() == ProductStatus.INACTIVE && request.getStatus() == null) {
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
        //Apply updates (partial or full)
        cardProductMapper.updateEntity(request, card);

        CardProductResponse response =
                cardProductMapper.toResponse(cardProductRepository.save(card));
        return response;
    }

    /**
     * {@inheritDoc}
     * Retrieves an active card product entity.
     *
     * <p>Used internally by other services (e.g., card issuance).</p>
     *
     * @param cardProductId product ID
     * @return active {@link CreditCardProduct}
     * @throws BadRequestException if product is not active
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
     * Retrieves card product entity by ID.
     *
     * @param id product ID
     * @return {@link CreditCardProduct}
     * @throws ResourceNotFoundException if not found
     */
    private CreditCardProduct findById(UUID id) {

        return cardProductRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Credit Card product with id " + id + " not found"));
    }
    /**
     * Validates statement cycle day range (1–28).
     * @param cycleDay
     */
    private void validateStatementCycleDay(Integer cycleDay) {
        if (cycleDay != null && (cycleDay < STATEMENT_CYCLE_DAY_MIN || cycleDay > STATEMENT_CYCLE_DAY_MAX)) {
            throw new BadRequestException("Statement cycle day must be between 1 and 28");
        }
    }
    /**
     * Validates financial constraints for card product.
     */
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
    /**
     * Validates feature toggles against corresponding limits.
     */
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
    /**
     * Ensures at least one field is provided for update.
     */
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
                request.getProductDescription() == null &&
                request.getStatus() == null) {

            throw new BadRequestException(
                    "At least one field must be provided for update");
        }
    }
}