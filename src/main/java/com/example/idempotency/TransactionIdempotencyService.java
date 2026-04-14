package com.example.idempotency;

import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.example.dto.request.TransactionRequest;
import com.example.dto.response.TransactionSummaryResponse;
import com.example.service.TransactionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionIdempotencyService {

    private final IdempotencyStore store;
    private final RequestHashUtil hashUtil;
    private final TransactionService transactionService;

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

            return record; // 🔥 IMPORTANT

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

    private void validateReference(String transactionReference) {
        if (transactionReference == null || transactionReference.isBlank()) {
            throw new IllegalArgumentException(
                    "Transaction reference is required"
            );
        }
    }
}