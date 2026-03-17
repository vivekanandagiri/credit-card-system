package com.example.api;

import com.example.dto.request.ApplicationDecisionRequest;
import com.example.dto.request.CreditCardApplicationRequest;
import com.example.dto.response.ApiResponse;
import com.example.dto.response.CreditCardApplicationCreateResponse;
import com.example.dto.response.CreditCardApplicationResponse;
import com.example.enums.ApplicationStatus;
import com.example.security.CustomUserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Credit Account Applications", description = "Credit Account application management APIs")
@RequestMapping("/api/v1")
public interface CreditCardApplicationApi {

//    @Operation(summary = "Get available credit card products")
//    @GetMapping("/applications/card-products")
//    ResponseEntity<ApiResponse<List<CardProductResponse>>> getAvailableCardProducts();


    @Operation(summary = "Apply for a credit card")
    @PostMapping("/applications")
    ResponseEntity<ApiResponse<CreditCardApplicationCreateResponse>> apply(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody CreditCardApplicationRequest request);


    @Operation(summary = "Get applications (customer gets own applications, admin gets all)")
    @GetMapping("/applications")
    ResponseEntity<ApiResponse<List<CreditCardApplicationResponse>>> getApplications(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @RequestParam(required = false) ApplicationStatus status);


    @Operation(summary = "Get application by ID")
    @GetMapping("/applications/{applicationId}")
    ResponseEntity<ApiResponse<CreditCardApplicationResponse>> getApplicationById(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID applicationId);


    @Operation(summary = "Admin decision on application (approve / reject)")
    @PatchMapping("/applications/{applicationId}")
    ResponseEntity<ApiResponse<CreditCardApplicationResponse>> decide(
            @PathVariable UUID applicationId,
            @Valid @RequestBody ApplicationDecisionRequest request);
}