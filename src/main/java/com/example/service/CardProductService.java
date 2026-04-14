package com.example.service;

import java.util.List;
import java.util.UUID;

import com.example.dto.request.CardProductCreateRequest;
import com.example.dto.request.CardProductUpdateRequest;
import com.example.dto.response.CardProductCreateResponse;
import com.example.dto.response.CardProductResponse;
import com.example.entity.CreditCardProduct;
import com.example.enums.ProductStatus;

/**
 * Service interface for credit card product lifecycle management.
 */
public interface CardProductService {

	CardProductCreateResponse create(CardProductCreateRequest request);
    CardProductResponse getById(UUID id);

    List<CardProductResponse> getAll(ProductStatus status);

    List<CardProductResponse> getAllActive();


    CardProductResponse update(UUID id, CardProductUpdateRequest request);

    
    /**
     * Returns the raw {@link CreditCardProduct} entity for internal service-to-service use.
     * The product must be ACTIVE.
     *
     * @throws ResourceNotFoundException if not found
     * @throws BusinessRuleException     if the product is INACTIVE
     */
    CreditCardProduct getActiveCardProductEntity(UUID cardProductId);
}