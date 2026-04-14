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

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentIdempotencyService {

    private final IdempotencyStore store;
    private final RequestHashUtil hashUtil;
    private final PaymentService paymentService;

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

            // 🔥 Fetch existing payment (you must implement this)
            PaymentResponse existing =
                    paymentService.getByReferenceId(referenceId);

            // 🔥 Return as duplicate instead of throwing error
            return new IdempotencyRecord<>(
                    requestHash,
                    existing,
                    Instant.now(),
                    true
            );
        }
    }

    private void validateReference(String referenceId) {
        if (referenceId == null || referenceId.isBlank()) {
            throw new IllegalArgumentException("ReferenceId is required");
        }
    }
}