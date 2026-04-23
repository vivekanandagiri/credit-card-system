package com.example.api;

import com.example.dto.request.ApplicationDecisionRequest;
import com.example.dto.request.CreditCardApplicationRequest;
import com.example.dto.response.ApiResponse;
import com.example.dto.response.CreditCardApplicationResponse;
import com.example.dto.response.CreditCardApplicationSummaryResponse;
import com.example.enums.ApplicationStatus;
import com.example.security.CustomUserPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * API contract for Credit Card Application workflow.
 *
 * <p>This API supports:
 * <ul>
 *     <li>Submitting credit card applications</li>
 *     <li>Fetching applications (Customer: own, Admin: all)</li>
 *     <li>Viewing detailed application info</li>
 *     <li>Admin decision (approve / reject)</li>
 * </ul>
 *
 * <p>Base URL: <b>/api/v1</b>
 *
 * <p>All responses are wrapped in {@link ApiResponse}
 */
@Tag(name = "7. Credit Account Applications", description = "APIs for managing credit card applications")
@RequestMapping("/api/v1")
public interface CreditCardApplicationApi {

    /**
     * Submit a credit card application.
     *
     * <p>When a customer applies:
     * <ul>
     *     <li>Underwriting is triggered automatically</li>
     *     <li>Decision outcomes:
     *          <ul>
     *              <li>AUTO_APPROVED → account created instantly</li>
     *              <li>AUTO_REJECTED → application closed</li>
     *              <li>PENDING_REVIEW → manual intervention required</li>
     *          </ul>
     *     </li>
     * </ul>
     *
     * @param principal authenticated user (customer)
     * @param request application request payload
     * @return application summary response
     */
    @Operation(
            summary = "Apply for a credit product",
            description = """
                Customer submits a credit application. Underwriting runs automatically.

                Decision outcomes:
                - AUTO_APPROVED → Account created instantly
                - AUTO_REJECTED → Application rejected
                - PENDING_REVIEW → Requires admin review
                """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Application submitted successfully",
                    content = @Content(
                            schema = @Schema(implementation = CreditCardApplicationSummaryResponse.class)
                    )
            )
    })
    @PostMapping("/applications")
    ResponseEntity<ApiResponse<CreditCardApplicationSummaryResponse>> apply(

            @AuthenticationPrincipal
            CustomUserPrincipal principal,

            @Valid @RequestBody CreditCardApplicationRequest request
    );

    /**
     * Fetch credit card applications.
     *
     * <p>Behavior:
     * <ul>
     *     <li><b>Customer:</b> Gets only their applications</li>
     *     <li><b>Admin:</b> Gets all applications</li>
     * </ul>
     *
     * <p>Optional filtering by application status.
     *
     * @param principal authenticated user
     * @param status optional application status filter
     * @return list of application summaries
     */
    @Operation(
            summary = "Get applications",
            description = "Fetch applications (Customer: own, Admin: all) with optional status filter"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Applications fetched successfully",
                    content = @Content(
                            schema = @Schema(implementation = CreditCardApplicationSummaryResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Access denied"
            )
    })
    @GetMapping("/applications")
    ResponseEntity<ApiResponse<List<CreditCardApplicationSummaryResponse>>> getApplications(

            @AuthenticationPrincipal
            CustomUserPrincipal principal,

            @Parameter(
                    description = "Filter applications by status (optional)",
                    example = "PENDING_REVIEW"
            )
            @RequestParam(required = false) ApplicationStatus status
    );

    /**
     * Fetch detailed information about a specific application.
     *
     * <p>Customers can only access their own applications.
     * Admins can access any application.
     *
     * @param principal authenticated user
     * @param applicationId application ID
     * @return detailed application response
     */
    @Operation(
            summary = "Get application by ID",
            description = "Fetch detailed information for a specific application"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Application found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Application not found"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "Access denied"
            )
    })
    @GetMapping("/applications/{applicationId}")
    ResponseEntity<ApiResponse<CreditCardApplicationResponse>> getApplicationById(

            @AuthenticationPrincipal
            CustomUserPrincipal principal,

            @Parameter(
                    description = "Application ID",
                    required = true,
                    example = "b12a3c45-6789-4abc-9def-1234567890ab"
            )
            @PathVariable UUID applicationId
    );

    /**
     * Make a decision on a credit card application.
     *
     * <p>This endpoint is used by administrators to:
     * <ul>
     *     <li>Approve an application</li>
     *     <li>Reject an application</li>
     * </ul>
     *
     * <p>If approved, a credit account may be created.
     *
     * <p><b>Security:</b> Admin only.
     *
     * @param applicationId application ID
     * @param request decision payload
     * @return updated application details
     */
    @Operation(
            summary = "Admin decision on application (approve / reject)",
            description = "Allows admin to approve or reject a pending application"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Application decision applied successfully",
                    content = @Content(
                            schema = @Schema(implementation = CreditCardApplicationResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "Application not found"
            )
    })
    @PatchMapping("/applications/{applicationId}")
    ResponseEntity<ApiResponse<CreditCardApplicationResponse>> decide(

            @Parameter(
                    description = "Application ID",
                    required = true,
                    example = "b12a3c45-6789-4abc-9def-1234567890ab"
            )
            @PathVariable UUID applicationId,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Decision request (APPROVE / REJECT)",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = ApplicationDecisionRequest.class)
                    )
            )
            @Valid @RequestBody ApplicationDecisionRequest request
    );
}