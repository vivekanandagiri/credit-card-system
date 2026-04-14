package com.example.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import com.example.api.CreditProductApi;
import com.example.dto.request.CreditProductCreateRequest;
import com.example.dto.request.CreditProductUpdateRequest;
import com.example.dto.response.ApiResponse;
import com.example.dto.response.CreditProductCreateResponse;
import com.example.dto.response.CreditProductResponse;
import com.example.service.CreditProductService;

@RestController
public class CreditProductController implements CreditProductApi {

    private final CreditProductService creditProductService;

    public CreditProductController(CreditProductService creditProductService) {
        this.creditProductService = creditProductService;
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CreditProductCreateResponse>> create(
            CreditProductCreateRequest request) {

       CreditProductCreateResponse response =
                creditProductService.create(request);

       return ResponseEntity.status(HttpStatus.CREATED)
               .body(ApiResponse.success(
            		   HttpStatus.CREATED,
            		   "Credit product created successfully",
            		   response));
    }

    @Override
    public ResponseEntity<ApiResponse<CreditProductResponse>> getById(Long id) {

    	 CreditProductResponse response =
                 creditProductService.getById(id);

         return ResponseEntity.ok(
                 ApiResponse.success(HttpStatus.OK,
                		 "Credit product fetched successfully",
                		 response)
         );
    }

    @Override
    public ResponseEntity<ApiResponse<List<CreditProductResponse>>> getAll() {

        List<CreditProductResponse> responses =
                creditProductService.getAll();

        return ResponseEntity.ok(
                ApiResponse.success(
                		HttpStatus.OK,
                		"Credit products fetched successfully",
                		responses)
        );
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CreditProductResponse>> update(
            Long id,
            CreditProductUpdateRequest request) {

        CreditProductResponse response =
                creditProductService.update(id, request);

        return ResponseEntity.ok(
                ApiResponse.success(
                		HttpStatus.OK,
                		"Credit product updated successfully", response)
        );
    }


}