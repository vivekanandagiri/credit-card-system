package com.example.dto.request;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthorizationRequest {
    private UUID accountId;
    private UUID cardId;
    private BigDecimal amount;
    private String networkReference;
}
