package com.example.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter 
@NoArgsConstructor 
@AllArgsConstructor
@Schema(description = "Request object for Credit Account status Update ")
public class CreditAccountStatusUpdateRequest {

	@Schema(description = "Credit Account Status.(e.g:ACTIVE, SUSPENDED, BLOCKED, CLOSED)",
			example = "ACTIVE",
			requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Status is required")
    private String status;   // ACTIVE, SUSPENDED, BLOCKED, CLOSED
	
	@Schema(description = "Reason of changing the status ",
			example = "Suspicious Transaction detected",
			requiredMode = Schema.RequiredMode.REQUIRED)
	@Size(max = 255, message = "Rejection reason cannot exceed 255 characters")
    private String reason;  
}