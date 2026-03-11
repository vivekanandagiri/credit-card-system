package com.example.controller;

import com.example.dto.request.ApplicationDecisionRequest;
import com.example.dto.request.CreditCardApplicationRequest;
import com.example.dto.response.ApiResponse;
import com.example.dto.response.CardProductResponse;
import com.example.dto.response.CreditCardApplicationResponse;
import com.example.security.CustomUserPrincipal;
import com.example.service.CreditCardApplicationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/applications")
public class CreditCardApplicationController {

    private final CreditCardApplicationService service;

    public CreditCardApplicationController(CreditCardApplicationService service) {
        this.service = service;
    }

    // CUSTOMER — browse card products
    @GetMapping("/card-products")
    public ResponseEntity<ApiResponse<List<CardProductResponse>>> getAvailableCardProducts() {
        return ResponseEntity.ok(service.getAvailableCardProducts());
    }

    // CUSTOMER — apply for card
    @PostMapping
    public ResponseEntity<ApiResponse<CreditCardApplicationResponse>> apply(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestBody CreditCardApplicationRequest request) {

        ApiResponse<CreditCardApplicationResponse> response =
                service.apply(principal.getUserId(), request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // CUSTOMER — view my applications
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<CreditCardApplicationResponse>>> getMyApplications(
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        return ResponseEntity.ok(service.getMyApplications(principal.getUserId()));
    }

    // CUSTOMER — view my application
    @GetMapping("/my/{applicationId}")
    public ResponseEntity<ApiResponse<CreditCardApplicationResponse>> getMyApplicationById(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID applicationId) {

        return ResponseEntity.ok(
                service.getMyApplicationById(principal.getUserId(), applicationId));
    }

    // ADMIN — view all applications
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CreditCardApplicationResponse>>> getAll() {
        return ResponseEntity.ok(service.getAllApplications());
    }

    // ADMIN — filter by status
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<List<CreditCardApplicationResponse>>> getByStatus(
            @RequestParam String status) {

        return ResponseEntity.ok(service.getApplicationsByStatus(status));
    }

    // ADMIN — approve/reject
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{applicationId}/decide")
    public ResponseEntity<ApiResponse<CreditCardApplicationResponse>> decide(
            @PathVariable UUID applicationId,
            @RequestBody ApplicationDecisionRequest request) {

        return ResponseEntity.ok(service.decide(applicationId, request));
    }
}