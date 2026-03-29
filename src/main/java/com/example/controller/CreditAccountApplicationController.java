package com.example.controller;

import com.example.api.CreditCardApplicationApi;
import com.example.dto.request.ApplicationDecisionRequest;
import com.example.dto.request.CreditCardApplicationRequest;
import com.example.dto.response.ApiResponse;
import com.example.dto.response.CreditCardApplicationResponse;
import com.example.dto.response.CreditCardApplicationSummaryResponse;
import com.example.enums.ApplicationStatus;
import com.example.enums.UserRole;
import com.example.security.CustomUserPrincipal;
import com.example.service.CreditAccountApplicationService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class CreditAccountApplicationController implements CreditCardApplicationApi {

    private final CreditAccountApplicationService service;

    public CreditAccountApplicationController(CreditAccountApplicationService service) {
        this.service = service;
    }


    @Override
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<CreditCardApplicationSummaryResponse>> apply(
            CustomUserPrincipal principal,
            @Valid CreditCardApplicationRequest request) {

    	CreditCardApplicationSummaryResponse response =
                service.apply(principal.getUserId(), request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(HttpStatus.CREATED,"Application submitted successfully", response));
    }

    @Override
    public ResponseEntity<ApiResponse<List<CreditCardApplicationSummaryResponse>>> getApplications(
            CustomUserPrincipal principal,
            ApplicationStatus status) {

        List<CreditCardApplicationSummaryResponse> responses;

        if (principal.getRole() == UserRole.ADMIN) {
        	responses = (status != null)
                    ? service.getApplicationsByStatus(status.name())
                    : service.getAllApplications();
        } else {
        	responses = (status != null)
                    ? service.getCustomerApplicationsByStatus(principal.getUserId(), status.name())
                    : service.getCustomerApplications(principal.getUserId());
        }

        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK,"Applications fetched successfully", responses)
        );
    }

    @Override
    public ResponseEntity<ApiResponse<CreditCardApplicationResponse>> getApplicationById(
            CustomUserPrincipal principal,
            UUID applicationId) {

        CreditCardApplicationResponse response;

        if (principal.getRole() == UserRole.ADMIN) {
        	response = service.getApplicationById(applicationId);
        } else {
        	response = service.getCustomerApplicationById(
                    principal.getCustomerId(),
                    applicationId
            );
        }

        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK,"Application fetched successfully", response)
        );
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CreditCardApplicationResponse>> decide(
            UUID applicationId,
            ApplicationDecisionRequest request) {

        CreditCardApplicationResponse response =
                service.decide(applicationId, request);

        return ResponseEntity.ok(
                ApiResponse.success(HttpStatus.OK,"Application decision processed successfully", response)
        );
    }
}