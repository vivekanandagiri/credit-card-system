package com.example.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.example.enums.PaymentMethod;
import com.example.enums.PaymentStatus;

@Builder
@Data
public class PaymentResponse {

    private UUID paymentId;
    private UUID accountId;

    private BigDecimal amount;

    private PaymentStatus status;
    private PaymentMethod paymentMethod;

    private String referenceId;

    private Instant paidAt;

    private List<PaymentAllocationResponse> allocations;
}