package com.example.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentAllocationResponse {

    private UUID statementId;
    private BigDecimal allocatedAmount;
    private BigDecimal totalAmountDue;
}