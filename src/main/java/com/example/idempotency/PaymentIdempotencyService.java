package com.example.idempotency;

import java.time.Instant;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.example.dto.request.PaymentRequest;
import com.example.dto.response.PaymentResponse;
import com.example.service.PaymentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
/**
 * Service responsible for handling idempotent payment processing.
 *
 * <p>This service ensures that a payment request with the same reference ID
 * is executed only once, even in the presence of retries, network failures,
 * or concurrent requests.</p>
 *
 * <p><b>Idempotency Strategy:</b></p>
 * <ul>
 *     <li>Uses {@code referenceId} as the idempotency key</li>
 *     <li>Generates a request hash to validate payload consistency</li>
 *     <li>Delegates execution control to {@link IdempotencyStore}</li>
 * </ul>
 *
 * <p><b>Execution Flow:</b></p>
 * <ul>
 *     <li>Validate reference ID</li>
 *     <li>Generate request hash</li>
 *     <li>Attempt execution via idempotency store</li>
 *     <li>If duplicate → return cached response</li>
 *     <li>If new → execute payment and store result</li>
 * </ul>
 *
 * <p><b>Failure Handling:</b></p>
 * <ul>
 *     <li>If a database-level uniqueness constraint is violated (e.g., duplicate insert),
 *     the service recovers by fetching the existing payment and returning it as a duplicate response.</li>
 * </ul>
 *
 * <p><b>Guarantee:</b>
 * Ensures at-most-once execution of payment per reference ID.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentIdempotencyService {

    private final IdempotencyStore store;
    private final RequestHashUtil hashUtil;
    private final PaymentService paymentService;

    /**
     * Processes a payment request in an idempotent manner.
     *
     * <p>This method guarantees:
     * <ul>
     *     <li>Duplicate requests with the same reference ID return the same response</li>
     *     <li>Requests with the same reference ID but different payloads are rejected
     *     (handled by the underlying {@link IdempotencyStore})</li>
     *     <li>Concurrent or race-condition scenarios are safely handled using
     *     database-level constraints and fallback logic</li>
     * </ul>
     *
     * @param accountId the account initiating the payment
     * @param request   the payment request payload
     * @return an {@link IdempotencyRecord} containing the payment response and metadata
     * @throws IllegalArgumentException if the reference ID is null or blank
     */
    public IdempotencyRecord<PaymentResponse> process(
            UUID accountId,
            PaymentRequest request) {

        String referenceId = request.getReferenceId();
        validateReference(referenceId);

        String requestHash = hashUtil.hash(request);

        try {

            IdempotencyRecord<PaymentResponse> record =
                    store.compute(
                            referenceId,
                            requestHash,
                            () -> paymentService.makePayment(
                                    accountId,
                                    request
                            )
                    );

            log.info("Payment processed/retrieved for referenceId={}", referenceId);

            return record;

        } catch (DataIntegrityViolationException ex) {

            log.warn(
                    "Duplicate payment detected at DB level for referenceId={}",
                    referenceId
            );

            //  Fetch existing payment 
            PaymentResponse existing =
                    paymentService.getByReferenceId(referenceId);

            //  Return as duplicate instead of throwing error
            return new IdempotencyRecord<>(
                    requestHash,
                    existing,
                    Instant.now(),
                    true
            );
        }
    }

    /**
     * Validates that the reference ID is present and non-empty.
     *
     * @param referenceId the payment reference ID
     * @throws IllegalArgumentException if reference ID is null or blank
     */
    private void validateReference(String referenceId) {
        if (referenceId == null || referenceId.isBlank()) {
            throw new IllegalArgumentException("ReferenceId is required");
        }
    }
}