package com.example.idempotency;

import java.time.Instant;

import lombok.Getter;

@Getter
public class IdempotencyRecord<T> {

    private final String requestHash;
    private final T responseBody;
    private final Instant createdAt;
    private final boolean duplicate;

    // Constructor for NEW records
    public IdempotencyRecord(String requestHash, T responseBody, Instant createdAt) {
        this(requestHash, responseBody, createdAt, false);
    }

    // Full constructor
    public IdempotencyRecord(String requestHash, T responseBody, Instant createdAt, boolean duplicate) {
        this.requestHash = requestHash;
        this.responseBody = responseBody;
        this.createdAt = createdAt;
        this.duplicate = duplicate;
    }
}