package com.example.dto.request;

import com.example.enums.CardFormat;
import com.example.enums.CardIssuanceReason;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to issue a new credit card")
public class CreditCardIssuanceRequest {

    @NotNull(message = "Card product ID is required")
    @Schema(
        description = "Card product to be issued",
        example = "b96f62e0-a17c-410a-887c-f90075889b09",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private UUID cardProductId;

    @NotNull(message = "Card format is required")
    @Schema(
        description = "Format of the card",
        example = "VIRTUAL",
        allowableValues = {"VIRTUAL", "PHYSICAL"},
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private CardFormat cardFormat;

    @Schema(
        description = "Reason for card issuance",
        example = "NEW_CARD",
        allowableValues = {"NEW_CARD", "REPLACEMENT", "UPGRADE"}
    )
    private CardIssuanceReason issuanceReason;
}