package com.example.dto.request;

import lombok.Data;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

@Data
@Schema(description = "Request object for Credit Application approval or rejection decission ")
public class ApplicationDecisionRequest {
	
	@Schema(description = "Indicates wheather the application is approved or rejected",
			example = "true",
			requiredMode = Schema.RequiredMode.REQUIRED
			)
    private boolean approved;
	
	@NotBlank(message = "Decission is required")
	@Schema(description = "Reason For Approval or Rejection",
	example ="Income verified and credit score acceptable" )
    private String decisionReason;

    // Only required if approved = true
	@DecimalMin(value = "1000.0", 
			message = "Approved credit limit must be at least 1000") 
	@Schema( description = "Approved credit limit (required when application is approved),It should in between the Credit product  Min credit limit and Max credit limit ",
	example = "200000" )
    private BigDecimal approvedCreditLimit;
	@DecimalMin(value = "0.1", message = "APR must be greater than 0") 
	@Schema( description = "Approved APR percentage (required when application is approved) ,It should satishfy  the Credit Product Min apr and max apr criteria ", example = "14.5" )
    private BigDecimal approvedApr;
}