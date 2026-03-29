package com.example.api;

import com.example.dto.request.ApplicationDecisionRequest;
import com.example.dto.request.CreditCardApplicationRequest;
import com.example.dto.response.ApiResponse;
import com.example.dto.response.CreditCardApplicationResponse;
import com.example.dto.response.CreditCardApplicationSummaryResponse;
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

@Tag(name = "Credit Account Applications")
@RequestMapping("/api/v1")
public interface CreditCardApplicationApi {

	 @Operation(
		        summary = "Apply for a credit product",
		        description = """
		            Customer submits a credit application. Underwriting runs automatically.
		            Decision is one of: AUTO_APPROVED, AUTO_REJECTED, or PENDING_REVIEW.
		            If AUTO_APPROVED, a credit account is created immediately.
		            """
		    )
    @PostMapping("/applications")
    ResponseEntity<ApiResponse<CreditCardApplicationSummaryResponse>> apply(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody CreditCardApplicationRequest request);


    @Operation(summary = "Get applications (customer gets own applications, admin gets all)")
    @GetMapping("/applications")
    ResponseEntity<ApiResponse<List<CreditCardApplicationSummaryResponse>>> getApplications(
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