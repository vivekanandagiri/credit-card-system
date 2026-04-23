package com.example.controller;

import com.example.api.BillingStatementApi;
import com.example.dto.response.ApiResponse;
import com.example.dto.response.BillingStatementResponse;
import com.example.enums.UserRole;
import com.example.security.CustomUserPrincipal;
import com.example.service.BillingStatementService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
public class BillingStatementController implements BillingStatementApi {

    private final BillingStatementService billingStatementService;

    public BillingStatementController(BillingStatementService billingStatementService) {
        this.billingStatementService = billingStatementService;
    }

    // =====================================================
    // SHARED ENDPOINTS
    // =====================================================

    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
    public ResponseEntity<ApiResponse<List<BillingStatementResponse>>> getStatements(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable UUID accountId) {

        List<BillingStatementResponse> responses;

        if (principal.getRole() == UserRole.ADMIN) {

            responses = billingStatementService.getStatements(accountId);

        } else {

            responses = billingStatementService.getCustomerStatementsByAccount(
                    principal.getUserId(), accountId);
        }

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK,
                        "Statements fetched successfully",
                        responses
                )
        );
    }
    
//    @PreAuthorize("hasRole('ADMIN')")
//    @PostMapping("/test-statement")
//    public BillingStatementResponse generateTestStatement(
//            @RequestParam UUID accountId,
//            @RequestParam String date // format: yyyy-MM-dd
//    ) {
//        LocalDate inputDate = LocalDate.parse(date);
//        return billingStatementService.generateStatementForDate(accountId, inputDate);
//    }


    // =====================================================

    @PostMapping("/{accountId}/statements")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BillingStatementResponse>> generateStatementManually(
            @PathVariable UUID accountId) {

        BillingStatementResponse response =
                billingStatementService.generateStatementManually(accountId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        HttpStatus.OK,
                        "Statement generated successfully",
                        response
                )
        );
    }

}