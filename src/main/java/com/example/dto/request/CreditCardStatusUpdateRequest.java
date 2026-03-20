package com.example.dto.request;

import lombok.*;


import com.example.enums.CardStatus;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreditCardStatusUpdateRequest {

    @NotNull(message = "Status is required")
    @Schema(
        description = "Card status",
        example = "ACTIVE",
        allowableValues = {"PENDING_ACTIVATION", "ACTIVE", "BLOCKED", "EXPIRED", "CANCELLED"}
    )
    private CardStatus status;

    @Schema(description = "Reason for status change", example = "User requested block")
    private String reason;
}