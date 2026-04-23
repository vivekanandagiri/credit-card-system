package com.example.idempotency;

import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.example.dto.request.TransactionRequest;
import com.example.dto.response.TransactionSummaryResponse;
import com.example.service.TransactionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service responsible for handling idempotent transaction processing.
 *
 * <p>This service ensures that a transaction with the same reference
 * is processed only once, even in the presence of retries, network failures,
 * or concurrent requests.</p>
 *
 * <p><b>Idempotency Strategy:</b></p>
 * <ul>
 *     <li>Uses transactionReference as the idempotency key</li>
 *     <li>Generates a request hash to validate payload consistency</li>
 *     <li>Delegates execution control to {@link IdempotencyStore}</li>
 * </ul>
 *
 * <p><b>Execution Flow:</b></p>
 * <ul>
 *     <li>Validate transaction reference</li>
 *     <li>Generate request hash</li>
 *     <li>Attempt execution via idempotency store</li>
 *     <li>If duplicate → return cached response</li>
 *     <li>If new → execute transaction and store result</li>
 * </ul>
 *
 * <p><b>Failure Handling:</b></p>
 * <ul>
 *     <li>If a database-level uniqueness violation occurs, the service
 *     recovers by fetching the existing transaction and returning it
 *     as a duplicate response.</li>
 * </ul>
 *
 * <p><b>Guarantee:</b>
 * Ensures at-most-once transaction execution per transaction reference.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionIdempotencyService {

    private final IdempotencyStore store;
    private final RequestHashUtil hashUtil;
    private final TransactionService transactionService;

    /**
     * Processes a transaction request in an idempotent manner.
     *
     * <p>This method ensures that:
     * <ul>
     *     <li>Duplicate requests with the same reference return the same response</li>
     *     <li>Requests with the same reference but different payloads are rejected
     *     (handled by the underlying store)</li>
     *     <li>Concurrent or race-condition scenarios are handled via database fallback</li>
     * </ul>
     *
     * @param userId  the user initiating the transaction
     * @param cardId  the card associated with the transaction
     * @param request the transaction request payload
     * @return an {@link IdempotencyRecord} containing the transaction response and metadata
     * @throws IllegalArgumentException if transaction reference is missing or invalid
     */
    public IdempotencyRecord<TransactionSummaryResponse> process(
            UUID userId,
            UUID cardId,
            TransactionRequest request) {

        String transactionReference = request.getTransactionReference();
        validateReference(transactionReference);

        String requestHash = hashUtil.hash(request);

        try {
            IdempotencyRecord<TransactionSummaryResponse> record =
                    store.compute(
                            transactionReference,
                            requestHash,
                            () -> transactionService.postTransaction(
                                    userId,
                                    cardId,
                                    request
                            )
                    );

            log.info(
                    "Transaction processed/retrieved for transactionReference={}",
                    transactionReference
            );

            return record; 

        } catch (DataIntegrityViolationException e) {

            log.warn(
                    "Duplicate transaction detected at DB level for transactionReference={}",
                    transactionReference
            );

            //  Fetch existing transaction
            TransactionSummaryResponse existing =
                    transactionService.getByTransactionReference(transactionReference);

            // Return as duplicate (DO NOT throw exception)
            return new IdempotencyRecord<>(
                    requestHash,
                    existing,
                    java.time.Instant.now(),
                    true
            );
        }
    }

    /**
     * Validates that the transaction reference is present and non-empty.
     *
     * @param transactionReference the transaction reference to validate
     * @throws IllegalArgumentException if the reference is null or blank
     */
    private void validateReference(String transactionReference) {
        if (transactionReference == null || transactionReference.isBlank()) {
            throw new IllegalArgumentException(
                    "Transaction reference is required"
            );
        }
    }
}