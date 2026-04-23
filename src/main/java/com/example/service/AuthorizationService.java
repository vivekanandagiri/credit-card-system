package com.example.service;

import java.math.BigDecimal;
import java.util.UUID;

import com.example.entity.Authorization;

public interface AuthorizationService {

    Authorization authorize(UUID accountId, UUID cardId,
                            BigDecimal amount, String networkRef);

    Authorization capture(UUID authorizationId);

    void expire(UUID authorizationId);
}