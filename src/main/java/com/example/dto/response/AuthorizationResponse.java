package com.example.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.example.enums.AuthStatus;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthorizationResponse {
    private UUID id;
    private UUID accountId;
    private BigDecimal amount;
    private AuthStatus status;
    private Instant expiresAt;
}
