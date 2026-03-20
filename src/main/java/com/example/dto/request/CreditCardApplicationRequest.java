package com.example.dto.request;

import lombok.Data;

import java.math.BigDecimal;

import com.example.enums.EmploymentType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Data
@Schema(description = "Request object for submitting a credit Account application")
public class CreditCardApplicationRequest {

    @Schema(
        description = "Unique ID of the credit product the customer is applying for",
        example = "101",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Credit product ID is required")
    @Positive(message = "Credit product ID must be positive")
    private Long creditProductId;


    @Schema(
        description = "Customer employment type",
        example = "SALARIED",
        allowableValues = {
            "SALARIED",
            "SELF_EMPLOYED",
            "BUSINESS_OWNER",
            "FREELANCER",
            "STUDENT",
            "RETIRED",
            "UNEMPLOYED"
        },
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Employment type is required")
    private EmploymentType employmentType;


    @Schema(
        description = "Employer name (required if employment type is SALARIED)",
        example = "TCS"
    )
    @Size(max = 255, message = "Employer name cannot exceed 255 characters")
    private String employerName;


    @Schema(
        description = "Customer monthly income (in INR)",
        example = "75000",
        minimum = "1"
    )
    @NotNull(message = "Monthly income is required")
    @DecimalMin(value = "1.0", inclusive = true, message = "Monthly income must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Invalid income format")
    private BigDecimal monthlyIncome;


    @Schema(
        description = "Total monthly liabilities (EMIs, loans, etc.)",
        example = "15000",
        minimum = "0"
    )
    @NotNull(message = "Existing liabilities are required")
    @DecimalMin(value = "0.0", message = "Existing liabilities cannot be negative")
    @Digits(integer = 10, fraction = 2, message = "Invalid liabilities format")
    private BigDecimal existingLiabilities;


    @Schema(
        description = "Customer credit score at the time of application",
        example = "720",
        minimum = "300",
        maximum = "900"
    )
    @NotNull(message = "Credit score is required")
    @Min(value = 300, message = "Credit score must be at least 300")
    @Max(value = 900, message = "Credit score cannot exceed 900")
    private Integer creditScoreAtApplication;


    @Schema(
        description = "Requested credit limit",
        example = "200000",
        minimum = "1000"
    )
    @NotNull(message = "Requested credit limit is required")
    @DecimalMin(value = "1000.0", message = "Requested credit limit must be at least 1000")
    @Digits(integer = 10, fraction = 2, message = "Invalid credit limit format")
    private BigDecimal requestedCreditLimit;
}