package com.example.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import com.example.api.CardProductApi;
import com.example.dto.request.CardProductCreateRequest;
import com.example.dto.request.CardProductUpdateRequest;
import com.example.dto.response.CardProductCreateResponse;
import com.example.dto.response.ApiResponse;
import com.example.dto.response.CardProductResponse;
import com.example.enums.ProductStatus;
import com.example.service.CardProductService;

@RestController
public class CardProductController implements CardProductApi {

	private final CardProductService cardProductService;

	public CardProductController(CardProductService cardProductService) {
		this.cardProductService = cardProductService;
	}

	@Override
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<CardProductCreateResponse>> create(CardProductCreateRequest request) {

		CardProductCreateResponse response = cardProductService.create(request);

		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiResponse.success(HttpStatus.CREATED,"Card product created successfully", response));
	}

	@Override
	public ResponseEntity<ApiResponse<CardProductResponse>> getById(UUID id) {
		CardProductResponse response = cardProductService.getById(id);

		return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK,"Card product fetched successfully", response));
	}

	@Override
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<ApiResponse<CardProductResponse>> update(UUID id, CardProductUpdateRequest request) {

		CardProductResponse response = cardProductService.update(id, request);

		return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK,"Card product updated successfully", response));
	}



	@Override
	public ResponseEntity<ApiResponse<List<CardProductResponse>>> getAll(ProductStatus status) {
		 List<CardProductResponse> responses =
	                cardProductService.getAll(status);

	        return ResponseEntity.ok(
	                ApiResponse.success(HttpStatus.OK,"Card products fetched successfully", responses)
	        );
	}
}