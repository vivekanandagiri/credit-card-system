package com.example.dto.request;

import lombok.Data;

import java.math.BigDecimal;

import com.example.enums.EmploymentType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Data
@Schema(description = "Request object for credit Card Application")
public class CreditCardApplicationRequest {
	
	@Schema(description = "Unique ID of the credits product the customer is applying for",
			example = "123",
			requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "Credit product ID is required")
    private Long creditProductId;
	
	@Schema(description = "Customer Employment Type :(e.g)"
			+ "	   SALARIED,\r\n"
			+ "    SELF_EMPLOYED,\r\n"
			+ "    BUSINESS_OWNER,\r\n"
			+ "    FREELANCER,\r\n"
			+ "    STUDENT,\r\n"
			+ "    RETIRED,\r\n"
			+ "    UNEMPLOYED",example = "SALARIED",
			requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "Employment type is required")
    private EmploymentType employmentType;  
	
	@Schema( description = "Employer name (required for salaried applicants)",
			example = "TCS" )
	@Size(max = 255, message = "Employer name cannot exceed 255 characters")
    private String employerName; 
	
	@Schema( description = "Customer monthly income", 
			example = "75000" )
    @NotNull(message = "Monthly income is required")
    @DecimalMin(value = "1.0", inclusive = true, message = "Monthly income must be greater than 0")
    private BigDecimal monthlyIncome;
    
    @Schema( description = "Total monthly liabilities (if customer have any loans, EMIs, etc.)", example = "15000" )
    @NotNull(message = "Existing liabilities are required") 
    @DecimalMin(value = "0.0", message = "Existing liabilities cannot be negative") 
    private BigDecimal existingLiabilities;
    
    @Schema( description = "Customer credit score at the time of application", 
    		example = "720", 
    		minimum = "300", 
    		maximum = "900" )
    @NotNull(message = "Credit score is required") 
    @Min(value = 300, message = "Credit score must be at least 300") 
    @Max(value = 900, message = "Credit score cannot exceed 900") 
    private Integer creditScoreAtApplication;
    
    @Schema( description = "Requested credit card limit", example = "200000" )
    @NotNull(message = "Requested credit limit is required") 
    @DecimalMin(value = "1000.0", message = "Requested credit limit must be at least 1000") 
    private BigDecimal requestedCreditLimit;
}