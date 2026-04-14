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

    // =====================================================
//
//    @GetMapping("/{accountId}/statements/{statementId}")
//    @PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
//    public ResponseEntity<ApiResponse<StatementResponse>> getStatementById(
//            @AuthenticationPrincipal CustomUserPrincipal principal,
//            @PathVariable UUID accountId,
//            @PathVariable UUID statementId) {
//
//        StatementResponse response;
//
//        if (principal.getRole() == UserRole.ADMIN) {
//
//            response = billingStatementService.getStatementById(accountId, statementId);
//
//        } else {
//
//            response = billingStatementService.getCustomerStatementById(
//                    principal.getUserId(), accountId, statementId);
//        }
//
//        return ResponseEntity.ok(
//                ApiResponse.success(
//                        HttpStatus.OK,
//                        "Statement fetched successfully",
//                        response
//                )
//        );
//    }

//    // =====================================================
//    // ADMIN ONLY
//    // =====================================================
//
//    @GetMapping("/statements")
//    @PreAuthorize("hasRole('ADMIN')")
//    public ResponseEntity<ApiResponse<List<StatementResponse>>> getAllStatements() {
//
//        List<StatementResponse> responses =
//                billingStatementService.getAllStatements();
//
//        return ResponseEntity.ok(
//                ApiResponse.success(
//                        HttpStatus.OK,
//                        "All statements fetched successfully",
//                        responses
//                )
//        );
//    }
//
//    // =====================================================
//
//    @GetMapping(value = "/statements", params = "status")
//    @PreAuthorize("hasRole('ADMIN')")
//    public ResponseEntity<ApiResponse<List<StatementResponse>>> getStatementsByStatus(
//            @RequestParam StatementStatus status) {
//
//        List<StatementResponse> responses =
//                billingStatementService.getStatementsByStatus(status);
//
//        return ResponseEntity.ok(
//                ApiResponse.success(
//                        HttpStatus.OK,
//                        "Statements fetched for status: " + status,
//                        responses
//                )
//        );
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