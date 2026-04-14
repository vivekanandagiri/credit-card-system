package com.example.mapper;

import com.example.dto.response.PaymentAllocationResponse;
import com.example.dto.response.PaymentResponse;
import com.example.entity.BillingStatement;
import com.example.entity.Payment;
import com.example.entity.PaymentAllocation;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class PaymentMapper {

    public PaymentResponse toResponse(Payment payment) {

        List<PaymentAllocationResponse> allocations =
                payment.getAllocations()
                        .stream()
                        .map(this::mapAllocation)
                        .toList();

        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .accountId(payment.getAccount().getAccountId())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .paymentMethod(payment.getPaymentMethod())
                .referenceId(payment.getReferenceId())
                .paidAt(payment.getPaidAt())
                .allocations(allocations)
                .build();
    }

    private PaymentAllocationResponse mapAllocation(
            PaymentAllocation allocation) {

        BillingStatement statement = allocation.getStatement();

        BigDecimal remaining =
                statement.getRemainingAmount() != null
                        ? statement.getRemainingAmount()
                        : BigDecimal.ZERO;

        if (remaining.compareTo(BigDecimal.ZERO) < 0) {
            remaining = BigDecimal.ZERO;
        }

        return PaymentAllocationResponse.builder()
                .statementId(statement.getStatementId())
                .allocatedAmount(allocation.getAllocatedAmount())
                .totalAmountDue(statement.getTotalAmountDue())
                .build();
    }
}