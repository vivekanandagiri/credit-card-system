package com.example.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request object for creating a customer address")
public class AddressCreateRequest {

	@Schema(description = "Type of address (HOME, WORK, BILLING, SHIPPING)", example = "HOME", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "Address type is required")
	@Size(max = 30, message = "Address type cannot exceed 30 characters")
	private String addressType;

	@Schema(description = "Address line 1 (house number, street)", example = "123 MG Road", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "Address line1 is required")
	@Size(max = 150, message = "Address line1 cannot exceed 150 characters")
	private String line1;

	@Schema(description = "Address line 2 (apartment, landmark)", example = "Near Metro Station")
	@Size(max = 150, message = "Address line2 cannot exceed 150 characters")
	private String line2;

	@Schema(description = "City name", example = "Bangalore", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "City is required")
	@Size(max = 100, message = "City cannot exceed 100 characters")
	private String city;

	@Schema(description = "State or province", example = "Karnataka", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "State is required")
	@Size(max = 100, message = "State cannot exceed 100 characters")
	private String state;

	@Schema(description = "Postal or ZIP code", example = "560001", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "Postal code is required")
	@Pattern(regexp = "^[0-9]{5,6}$", message = "Postal code must be 5 or 6 digits")
	private String postalCode;

	@Schema(description = "Country name or ISO country code", example = "India", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "Country is required")
	@Size(max = 100, message = "Country cannot exceed 100 characters")
	private String country;

	@Schema(description = "Indicates whether this is the primary address", example = "true")
	private boolean primary;
}