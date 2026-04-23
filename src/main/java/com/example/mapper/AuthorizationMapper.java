package com.example.mapper;

import org.springframework.stereotype.Component;

import com.example.dto.response.AuthorizationResponse;
import com.example.entity.Authorization;

@Component
public class AuthorizationMapper {

    public AuthorizationResponse toResponse(Authorization auth) {
        return AuthorizationResponse.builder()
                .id(auth.getId())
                .accountId(auth.getAccountId())
                .amount(auth.getAmount())
                .status(auth.getStatus())
                .expiresAt(auth.getExpiresAt())
                .build();
    }
}
