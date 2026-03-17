package com.example.dto.response;

import com.example.enums.ProductStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;



@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Response object of Credit product creation ")
public class CreditProductCreateResponse {

    @Schema(
            description = "Unique identifier of the credit product",
            example = "1"
    )
    private Long creditProductId;

    @Schema(
            description = "System-generated unique product code",
            example = "GOLD-CREDIT-CARD-001"
    )
    private String productCode;

    @Schema(
            description = "Product display name",
            example = "Gold Credit Card"
    )
    private String productName;

    @Schema(description = "Current product status",
            example = "ACTIVE")
    private ProductStatus status;
}