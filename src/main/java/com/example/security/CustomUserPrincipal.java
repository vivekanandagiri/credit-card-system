package com.example.security;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@Getter
@AllArgsConstructor
public class CustomUserPrincipal {

    private UUID userId;
    private UUID customerId;
    private String email;
    private String role;
}