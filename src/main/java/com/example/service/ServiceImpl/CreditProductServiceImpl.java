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

import lombok.RequiredArgsConstructor;

/**
 * Implementation of {@link CreditProductService} responsible for managing the
 * lifecycle and configuration of credit products.
 *
 * <p>
 * <b>Critical Role:</b>
 * </p>
 * <ul>
 * <li>Acts as the single source of truth for product rules</li>
 * <li>Defines APRs, fees, limits, and eligibility constraints</li>
 * <li>Impacts billing, underwriting, and risk engines downstream</li>
 * </ul>
 *
 * <p>
 * <b>Lifecycle:</b>
 * </p>
 * <ul>
 * <li>CREATE → Product defined with rules and limits</li>
 * <li>UPDATE → Modify configuration (with constraints)</li>
 * <li>ACTIVATE / INACTIVATE → Controls availability</li>
 * </ul>
 *
 * <p>
 * <b>Key Constraints:</b>
 * </p>
 * <ul>
 * <li>Min credit limit must not exceed max credit limit</li>
 * <li>Effective dates must be valid and non-retroactive</li>
 * <li>Inactive products are immutable unless reactivated</li>
 * </ul>
 */
@Service
@Transactional
@RequiredArgsConstructor
public class CreditProductServiceImpl implements CreditProductService {

	private static final Logger log = LoggerFactory.getLogger(CreditProductServiceImpl.class);

	private final CreditProductRepository creditProductRepository;
	private final CreditProductMapper mapper;
	private final ProductCodeGenerator codeGenerator;

	/**
	 * Creates a new credit product.
	 *
	 * <p>
	 * Steps:
	 * </p>
	 * <ol>
	 * <li>Validate credit limits</li>
	 * <li>Validate effective dates</li>
	 * <li>Generate unique product code</li>
	 * <li>Persist product</li>
	 * </ol>
	 *
	 * @param request creation payload
	 * @return {@link CreditProductCreateResponse}
	 * @throws BusinessRuleException if validation fails
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

	/**
	 * Retrieves a credit product by ID.
	 *
	 * @param id product ID
	 * @return {@link CreditProductResponse}
	 * @throws ResourceNotFoundException if not found
	 */
	@Override
	@Transactional(readOnly = true)
	public CreditProductResponse getById(Long id) {
		CreditProduct product = findById(id);
		return mapper.toResponse(product);
	}

	/**
	 * Retrieves all credit products.
	 */
	@Override
	@Transactional(readOnly = true)
	public List<CreditProductResponse> getAll() {

		return creditProductRepository.findAll().stream().map(mapper::toResponse).collect(Collectors.toList());
	}

	/**
	 * Retrieves all active credit products.
	 */
	@Override
	@Transactional(readOnly = true)
	public List<CreditProductResponse> getAllActive() {

		return creditProductRepository.findAllByStatus(ProductStatus.ACTIVE).stream().map(mapper::toResponse)
				.collect(Collectors.toList());
	}

	/**
	 * Updates an existing credit product.
	 *
	 * <p>
	 * Rules:
	 * </p>
	 * <ul>
	 * <li>Inactive products cannot be modified unless reactivated</li>
	 * <li>Status transitions must not be redundant</li>
	 * <li>All updated values must remain valid</li>
	 * </ul>
	 *
	 * @param id      product ID
	 * @param request update payload
	 * @return updated {@link CreditProductResponse}
	 * @throws BusinessRuleException if validation fails
	 */
	@Override
	public CreditProductResponse update(Long id, CreditProductUpdateRequest request) {

		CreditProduct product = findById(id);
		// Handle explicit status updates during a general edit
		validateStatusTransition(product, request);
		boolean reactivating = product.getStatus() == ProductStatus.INACTIVE
				&& request.getStatus() == ProductStatus.ACTIVE;
		// Prevent updates if inactive (unless activating)
		// Do not allow modifying the financial parameters of a Inactive product
		if (product.getStatus() == ProductStatus.INACTIVE && !reactivating) {
			throw new BusinessRuleException("Cannot update an inactive credit product unless reactivating it");
		}

		mapper.updateEntity(request, product);

		validateUpdatedProduct(product);

		CreditProduct saved = creditProductRepository.save(product);

		return mapper.toResponse(saved);
	}

	/**
	 * Updates only the status of a credit product.
	 *
	 * @param id     product ID
	 * @param status new status
	 * @return status string
	 */
	@Override
	public String updateStatus(Long id, ProductStatus status) {

		CreditProduct creditProduct = findById(id);

		if (creditProduct.getStatus() == status) {
			throw new BusinessRuleException("Credit product is already " + status.name().toLowerCase());
		}

		creditProduct.setStatus(status);
		creditProductRepository.save(creditProduct);

		return status == ProductStatus.ACTIVE ? "ACTIVE" : "INACTIVE";
	}

	/**
	 * {@inheritDoc} Retrieves an active credit product entity.
	 *
	 * @param creditProductId product ID
	 * @return active {@link CreditProduct}
	 * @throws BusinessRuleException if inactive
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
	 * <p>
	 * Returns the entity regardless of status. Use {@link #getActiveCreditProduct}
	 * when the product must be active.
	 */
	@Override
	@Transactional(readOnly = true)
	public CreditProduct getCreditProductEntity(Long creditProductId) {
		return findById(creditProductId);
	}

	// --------------------Private Helpers--------------

	/**
	 * Finds product by ID.
	 */
	private CreditProduct findById(Long id) {
		return creditProductRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Credit product with id " + id + " not found"));
	}

	/**
	 * Validates credit limit boundaries.
	 */
	private void validateCreditLimits(CreditProductCreateRequest request) {
		if (request.getMinCreditLimit().compareTo(request.getMaxCreditLimit()) > 0) {
			throw new BusinessRuleException("Minimum credit limit cannot exceed maximum credit limit");
		}
	}

	/**
	 * Validation of Effective date: -- Effective from cannot be past
	 * 
	 * @param request
	 */
	private void validateEffectiveDates(CreditProductCreateRequest request) {
		if (request.getEffectiveFrom().isBefore(LocalDate.now())) {
			throw new BusinessRuleException("Effective start date cannot be in the past");
		}
		if (request.getEffectiveTo() != null && request.getEffectiveTo().isBefore(request.getEffectiveFrom())) {
			throw new BusinessRuleException("Effective end date cannot be before start date");
		}
	}

	/**
	 * Validates status transitions.
	 */
	private void validateStatusTransition(CreditProduct product, CreditProductUpdateRequest request) {

		if (request.getStatus() != null && product.getStatus() == request.getStatus()) {

			throw new BusinessRuleException("Credit product is already " + request.getStatus().name().toLowerCase());
		}
	}

	/**
	 * Validates product after applying updates.
	 */
	private void validateUpdatedProduct(CreditProduct product) {

		if (product.getMinCreditLimit() != null && product.getMaxCreditLimit() != null
				&& product.getMinCreditLimit().compareTo(product.getMaxCreditLimit()) > 0) {

			throw new BusinessRuleException("Minimum credit limit cannot exceed maximum credit limit");
		}

		if (product.getEffectiveFrom() != null && product.getEffectiveFrom().isBefore(LocalDate.now())) {

			throw new BusinessRuleException("Effective start date cannot be in the past");
		}

		if (product.getEffectiveFrom() != null && product.getEffectiveTo() != null
				&& product.getEffectiveTo().isBefore(product.getEffectiveFrom())) {

			throw new BusinessRuleException("Effective end date cannot be before start date");
		}
	}

	/**
	 * Generates a unique product code by appending a sequence number.
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
