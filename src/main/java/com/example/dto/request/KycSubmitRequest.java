package com.example.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Request object for submitting KYC document details")
public class KycSubmitRequest {

	@Schema(description = "Type of KYC document (PAN, AADHAR, PASSPORT, DRIVING_LICENSE)", example = "PAN", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "Document type is required")
	@Size(max = 30, message = "Document type cannot exceed 30 characters")
	private String documentType;

	@Schema(description = "Document identification number", example = "ABCDE1234F", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "Document number is required")
	@Size(max = 20, message = "Document number cannot exceed 20 characters")
	private String documentNumber;
}