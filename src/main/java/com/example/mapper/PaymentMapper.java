package com.example.mapper;

import com.example.dto.response.PaymentAllocationResponse;
import com.example.dto.response.PaymentResponse;
import com.example.entity.BillingStatement;
import com.example.entity.Payment;
import com.example.entity.PaymentAllocation;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Mapper responsible for converting {@link Payment} entities
 * into API response DTOs.
 *
 * <p>This mapper handles:</p>
 * <ul>
 *     <li>Transformation of {@link Payment} → {@link PaymentResponse}</li>
 *     <li>Nested mapping of allocation details</li>
 *     <li>Ensuring null-safe financial calculations</li>
 * </ul>
 *
 * <p><b>Important:</b> Financial values are handled using {@link BigDecimal}
 * to maintain precision.</p>
 */
@Component
public class PaymentMapper {

    /**
     * Converts a {@link Payment} entity into a {@link PaymentResponse}.
     *
     * <p>Includes mapping of all associated payment allocations.</p>
     *
     * @param payment the payment entity
     * @return mapped response DTO
     */
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

    /**
     * Maps a {@link PaymentAllocation} entity into
     * {@link PaymentAllocationResponse}.
     *
     * <p>Calculation logic:</p>
     * <ul>
     *     <li><b>remainingAfter</b> → current remaining balance on statement</li>
     *     <li><b>remainingBefore</b> → remainingAfter + allocated amount</li>
     * </ul>
     *
     * <p>Also ensures:
     * <ul>
     *     <li>null-safe handling of remaining amount</li>
     *     <li>no negative remaining balances</li>
     * </ul>
     * </p>
     *
     * @param allocation payment allocation entity
     * @return mapped allocation response
     */
    private PaymentAllocationResponse mapAllocation(
            PaymentAllocation allocation) {

        BillingStatement statement = allocation.getStatement();

        BigDecimal allocated = allocation.getAllocatedAmount();

        BigDecimal remainingAfter =
                statement.getRemainingAmount() != null
                        ? statement.getRemainingAmount()
                        : BigDecimal.ZERO;

        // Ensure no negative values
        if (remainingAfter.compareTo(BigDecimal.ZERO) < 0) {
            remainingAfter = BigDecimal.ZERO;
        }

        BigDecimal remainingBefore = remainingAfter.add(allocated);

        return PaymentAllocationResponse.builder()
                .statementId(statement.getStatementId())
                .allocatedAmount(allocated)
                .remainingBeforeAllocation(remainingBefore)
                .remainingAfterAllocation(remainingAfter)
                .build();
    }
}