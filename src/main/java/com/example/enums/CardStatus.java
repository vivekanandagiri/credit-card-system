package com.example.enums;

public enum CardStatus {
    PENDING_ACTIVATION,  // issued but not yet activated by customer
    ACTIVE,              // in use
    BLOCKED,             // temporarily blocked (lost/stolen report)
    EXPIRED,             // past expiry date
    CANCELLED            // permanently cancelled — terminal state
}