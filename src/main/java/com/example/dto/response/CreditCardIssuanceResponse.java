package com.example.dto.response;

import java.util.UUID;

import com.example.enums.CardStatus;
import com.example.enums.NetworkType;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Response object for credit card issuance details")
public class CreditCardIssuanceResponse {

	@Schema(description = "Unique identifier of the credit card", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
	private UUID cardId;

	@Schema(description = "Associated credit account ID", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
	private UUID accountId;

	@Schema(description = "Current status of the credit card", example = "ACTIVE", allowableValues = {
			"PENDING_ACTIVATION", "ACTIVE", "BLOCKED", "EXPIRED", "CANCELLED" })
	private CardStatus cardStatus;
	
	@Schema(description = "Customer full name", example = "Vivek Kumar")
	private String customerName;

	@Schema(description = "Masked card number for display (only last 4 digits visible)", example = "411111XXXXXX1234")
	private String maskedCardNumber;
	
	@Schema(description = "Network type", example = "VISA")
	private NetworkType networkType;
	
	@Schema(description = "Formatted expiry (MM/YY)", example = "03/29")
	private String expiryFormatted;
}