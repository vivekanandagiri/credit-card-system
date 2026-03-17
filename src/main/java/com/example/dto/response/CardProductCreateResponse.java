package com.example.dto.response;

import java.util.UUID;

import com.example.enums.NetworkType;
import com.example.enums.ProductStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Response returned after creating a card product")
public class CardProductCreateResponse {
	 @Schema(description = "Unique identifier of the card product",
	            example = "550e8400-e29b-41d4-a716-446655440000")
	    private UUID cardProductId;

	    @Schema(description = "Card product display name",
	            example = "Gold Visa Card")
	    private String productName;

	    @Schema(description = "Card network provider",
	            example = "VISA")
	    private NetworkType networkType;

	    @Schema(description = "Current card product status",
	            example = "ACTIVE")
	    private ProductStatus status;

}
