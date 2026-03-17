package com.example.controller;

import com.example.api.CreditCardApplicationApi;
import com.example.dto.request.ApplicationDecisionRequest;
import com.example.dto.request.CreditCardApplicationRequest;
import com.example.dto.response.ApiResponse;
import com.example.dto.response.CreditCardApplicationCreateResponse;
import com.example.dto.response.CreditCardApplicationResponse;
import com.example.enums.ApplicationStatus;
import com.example.security.CustomUserPrincipal;
import com.example.service.CreditCardApplicationService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class CreditCardApplicationController implements CreditCardApplicationApi {

    private final CreditCardApplicationService service;

    public CreditCardApplicationController(CreditCardApplicationService service) {
        this.service = service;
    }

//    @Override
//    public ResponseEntity<ApiResponse<List<CardProductResponse>>> getAvailableCardProducts() {
//        return ResponseEntity.ok(service.getAvailableCardProducts());
//    }

    @Override
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<CreditCardApplicationCreateResponse>> apply(
            CustomUserPrincipal principal,
            @Valid CreditCardApplicationRequest request) {

        ApiResponse<CreditCardApplicationCreateResponse> response =
                service.apply(principal.getUserId(), request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<ApiResponse<List<CreditCardApplicationResponse>>> getApplications(
            CustomUserPrincipal principal,
            ApplicationStatus status) {

        if (principal.getRole().equals("ADMIN")) {

            if (status != null) {
                return ResponseEntity.ok(service.getApplicationsByStatus(status.name()));
            }

            return ResponseEntity.ok(service.getAllApplications());
        }

        return ResponseEntity.ok(service.getMyApplications(principal.getUserId()));
    }

    @Override
    public ResponseEntity<ApiResponse<CreditCardApplicationResponse>> getApplicationById(
            CustomUserPrincipal principal,
            UUID applicationId) {

        if (principal.getRole().equals("ADMIN")) {
            return ResponseEntity.ok(service.getApplicationById(applicationId));
        }

        return ResponseEntity.ok(
                service.getMyApplicationById(principal.getUserId(), applicationId));
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CreditCardApplicationResponse>> decide(
            UUID applicationId,
            ApplicationDecisionRequest request) {

        return ResponseEntity.ok(service.decide(applicationId, request));
    }
}