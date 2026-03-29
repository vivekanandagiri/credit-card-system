package com.example.service;

import com.example.dto.request.CreditProductCreateRequest;
import com.example.dto.request.CreditProductUpdateRequest;
import com.example.dto.response.CreditProductCreateResponse;
import com.example.dto.response.CreditProductResponse;
import com.example.entity.CreditProduct;
import com.example.enums.ProductStatus;

import java.util.List;

/**
 * Service interface for credit product lifecycle management.
 */
public interface CreditProductService {

	CreditProductCreateResponse create(CreditProductCreateRequest request);

	CreditProductResponse getById(Long id);

	List<CreditProductResponse> getAll();

	List<CreditProductResponse> getAllActive();

	CreditProductResponse update(Long id, CreditProductUpdateRequest request);

	String updateStatus(Long id, ProductStatus status);

	/**
	 * Returns the raw {@link CreditProduct} entity for internal service-to-service
	 * use. Must not be exposed via the REST layer.
	 *
	 * @throws com.example.exception.ResourceNotFoundException if not found
	 * @throws com.example.exception.BusinessRuleException     if the product is
	 *                                                         INACTIVE
	 */
	CreditProduct getActiveCreditProduct(Long creditProductId);

	/**
	 * Returns the raw {@link CreditProduct} entity regardless of status. Use
	 * {@link #getActiveCreditProduct} when the product must be active.
	 *
	 * @throws com.example.exception.ResourceNotFoundException if not found
	 */
	CreditProduct getCreditProductEntity(Long creditProductId);
}