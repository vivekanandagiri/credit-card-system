package com.example.service.ServiceImpl;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.example.service.CreditProductService;
import com.example.util.ProductCodeGenerator;


/**
 * Build The lifecycle and configuration of Credit Card Products.
 * <p>
 * This service acts as the source of truth for product rules (APRs, fees, limits).
 * Changes to these entities directly impact down-stream billing and origination engines.
 */
@Service
@Transactional
public class CreditProductServiceImpl implements CreditProductService {

	private static final Logger log = LoggerFactory.getLogger(CreditProductServiceImpl.class);
	
    private final CreditProductRepository creditProductRepository;
    private final CreditProductMapper mapper;
    private final ProductCodeGenerator codeGenerator;
 
    public CreditProductServiceImpl(
            CreditProductRepository creditProductRepository,
            CreditProductMapper mapper,
            ProductCodeGenerator codeGenerator) {
        this.creditProductRepository = creditProductRepository;
        this.mapper = mapper;
        this.codeGenerator = codeGenerator;
    }

	/**
	 * Credit Product Create 
	 */
	@Override
	@Transactional
	public CreditProductCreateResponse create(CreditProductCreateRequest request) {
		
		validateCreditLimits(request);
		validateEffectiveDates(request);
		 
		CreditProduct product = mapper.toEntity(request);
		String baseCode = codeGenerator.generateBaseCode(request.getProductName());

		String finalCode = generateUniqueCode(baseCode);

		product.setProductCode(finalCode);
		
		CreditProduct savedProduct = creditProductRepository.save(product);
		
		CreditProductCreateResponse response = mapper.toCreateResponse(savedProduct);
		log.info("New credit product created: {} with code {}", savedProduct.getProductName(), finalCode);
        return response;
	}

    @Override
    @Transactional(readOnly = true)
	public CreditProductResponse getById(Long id) {
		CreditProduct product = findById(id);
		return mapper.toResponse(product);
	}

    @Override
    @Transactional(readOnly = true)
    public List<CreditProductResponse> getAll() {

        return creditProductRepository.findAll()
                .stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CreditProductResponse> getAllActive() {

        return creditProductRepository.findAllByStatus(ProductStatus.ACTIVE)
                .stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CreditProductResponse update(Long id, CreditProductUpdateRequest request) {

        CreditProduct product = findById(id);
        // Handle explicit status updates during a general edit
        validateStatusTransition(product, request);
        boolean reactivating =
                product.getStatus() == ProductStatus.INACTIVE &&
                request.getStatus() == ProductStatus.ACTIVE;
        // Prevent updates if inactive (unless activating)
        // Do not allow modifying the financial parameters of a Inactive product
        if (product.getStatus() == ProductStatus.INACTIVE && !reactivating) {
            throw new BusinessRuleException(
                    "Cannot update an inactive credit product unless reactivating it");
        }

        applyUpdates(request, product);
        
        validateUpdatedProduct(product);

        CreditProduct saved = creditProductRepository.save(product);

        return mapper.toResponse(saved);
    }


    private void validateStatusTransition(
            CreditProduct product,
            CreditProductUpdateRequest request) {

        if (request.getStatus() != null &&
            product.getStatus() == request.getStatus()) {

            throw new BusinessRuleException(
                    "Credit product is already " +
                    request.getStatus().name().toLowerCase());
        }
    }
    private void validateUpdatedProduct(CreditProduct product) {

        if (product.getMinCreditLimit() != null &&
            product.getMaxCreditLimit() != null &&
            product.getMinCreditLimit().compareTo(product.getMaxCreditLimit()) > 0) {

            throw new BusinessRuleException(
                    "Minimum credit limit cannot exceed maximum credit limit");
        }

        if (product.getEffectiveFrom() != null &&
            product.getEffectiveFrom().isBefore(LocalDate.now())) {

            throw new BusinessRuleException(
                    "Effective start date cannot be in the past");
        }

        if (product.getEffectiveFrom() != null &&
            product.getEffectiveTo() != null &&
            product.getEffectiveTo().isBefore(product.getEffectiveFrom())) {

            throw new BusinessRuleException(
                    "Effective end date cannot be before start date");
        }
    }

	@Override
    public String updateStatus(Long id, ProductStatus status) {

        CreditProduct creditProduct = findById(id);

        if (creditProduct.getStatus() == status) {
            throw new BusinessRuleException(
                    "Credit product is already " + status.name().toLowerCase());
        }

        creditProduct.setStatus(status);
        creditProductRepository.save(creditProduct);

        return status == ProductStatus.ACTIVE
                ? "ACTIVE"
                : "INACTIVE";
    }
	
    /**
     * {@inheritDoc}
     *
     * <p>Returns the entity only if ACTIVE; throws {@link BusinessRuleException} otherwise.
     */
	
    @Override
    @Transactional(readOnly = true)
    public CreditProduct getActiveCreditProduct(Long creditProductId) {
        CreditProduct product = findById(creditProductId);
        if (product.getStatus() == ProductStatus.INACTIVE) {
            throw new BusinessRuleException("Selected credit product is no longer available");
        }
        return product;
    }

    /**
     * {@inheritDoc}
     * <p>Returns the entity regardless of status.
     * Use {@link #getActiveCreditProduct} when the product must be active.
     */
    @Override
    @Transactional(readOnly = true)
    public CreditProduct getCreditProductEntity(Long creditProductId) {
        return findById(creditProductId);
    }
	
	// --------------------Private Helpers--------------
	
    private CreditProduct findById(Long id) {
        return creditProductRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Credit product with id " + id + " not found"));
    }
    /**
     * Credit Limit Validation check
     * @param request
     */
    private void validateCreditLimits(CreditProductCreateRequest request) {
        if (request.getMinCreditLimit().compareTo(request.getMaxCreditLimit()) > 0) {
            throw new BusinessRuleException("Minimum credit limit cannot exceed maximum credit limit");
        }
    }
    
    /**
     * Validation of Effective date:
     * -- Effective from cannot be past
     * @param request
     */
    private void validateEffectiveDates(CreditProductCreateRequest request) {
        if (request.getEffectiveFrom().isBefore(LocalDate.now())) {
            throw new BusinessRuleException("Effective start date cannot be in the past");
        }
        if (request.getEffectiveTo() != null
                && request.getEffectiveTo().isBefore(request.getEffectiveFrom())) {
            throw new BusinessRuleException("Effective end date cannot be before start date");
        }
    }
    
    
    private void applyUpdates(CreditProductUpdateRequest request, CreditProduct product) {
		if (request.getProductName() != null)
			product.setProductName(request.getProductName());
		if (request.getMinCreditLimit() != null)
			product.setMinCreditLimit(request.getMinCreditLimit());
		if (request.getMaxCreditLimit() != null)
			product.setMaxCreditLimit(request.getMaxCreditLimit());
		if (request.getMinIncomeRequired() != null)
			product.setMinIncomeRequired(request.getMinIncomeRequired());
		if (request.getMinCreditScore() != null)
			product.setMinCreditScore(request.getMinCreditScore());
		if (request.getAprPurchase() != null)
			product.setAprPurchase(request.getAprPurchase());
		if (request.getAprCashAdvance() != null)
			product.setAprCashAdvance(request.getAprCashAdvance());
		if (request.getGracePeriodDays() != null)
			product.setGracePeriodDays(request.getGracePeriodDays());
		if (request.getInterestCalculationMethod() != null)
			product.setInterestCalculationMethod(request.getInterestCalculationMethod());
		if (request.getMinimumDuePercent() != null)
			product.setMinimumDuePercent(request.getMinimumDuePercent());
		if (request.getMinimumDueAmount() != null)
			product.setMinimumDueAmount(request.getMinimumDueAmount());
		if (request.getLateFeeAmount() != null)
			product.setLateFeeAmount(request.getLateFeeAmount());
		if (request.getOverlimitFee() != null)
			product.setOverlimitFee(request.getOverlimitFee());
		if (request.getJoiningFee() != null)
			product.setJoiningFee(request.getJoiningFee());
		if (request.getForeignTransactionFeePercent() != null)
			product.setForeignTransactionFeePercent(request.getForeignTransactionFeePercent());
		if (request.getBalanceTransferFeePercent() != null)
			product.setBalanceTransferFeePercent(request.getBalanceTransferFeePercent());
		if (request.getCashAdvanceFeePercent() != null)
			product.setCashAdvanceFeePercent(request.getCashAdvanceFeePercent());
		if (request.getCashAdvanceFeeMin() != null)
			product.setCashAdvanceFeeMin(request.getCashAdvanceFeeMin());
		if (request.getEffectiveFrom() != null)
			product.setEffectiveFrom(request.getEffectiveFrom());
		if (request.getEffectiveTo() != null)
			product.setEffectiveTo(request.getEffectiveTo());
		if (request.getStatus() != null)
		    product.setStatus(request.getStatus());
	}
    
    /**
     * Unique Product Code Generator
     * @param baseCode
     * @return
     */

		private String generateUniqueCode(String baseCode) {

			int counter = 1;
			String newCode = baseCode + "-001";

			while (creditProductRepository.existsByProductCode(newCode)) {

				counter++;
				newCode = String.format("%s-%03d", baseCode, counter);
			}

			return newCode;
		}

}
