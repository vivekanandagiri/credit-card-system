package com.example.idempotency;

import java.time.Instant;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;

import com.example.exception.ConflictException;
import com.github.benmanes.caffeine.cache.Cache;

import lombok.extern.slf4j.Slf4j;

/**
 * Caffeine-based implementation of {@link IdempotencyStore}.
 *
 * <p>This implementation ensures:
 * <ul>
 *     <li>Atomic execution using {@code compute()}</li>
 *     <li>Thread-safe idempotency handling</li>
 *     <li>Prevention of duplicate request execution</li>
 * </ul>
 *
 * <p><b>Behavior:</b>
 * <ul>
 *     <li>If key exists with same hash → return cached response</li>
 *     <li>If key exists with different hash → throw exception</li>
 *     <li>If key does not exist → execute supplier and store result</li>
 * </ul>
 *
 * <p><b>Note:</b>
 * This implementation is local to a single JVM instance.
 * For distributed systems, use Redis or database-backed storage.
 */
@Component
@Slf4j
public class CaffeineIdempotencyStore implements IdempotencyStore {

    private final Cache<String, IdempotencyRecord<?>> cache;

    public CaffeineIdempotencyStore(
            Cache<String, IdempotencyRecord<?>> cache) {
        this.cache = cache;
    }

    /**
     * Computes or retrieves idempotent response atomically.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> IdempotencyRecord<T> compute(
            String key,
            String requestHash,
            Supplier<T> supplier) {

        IdempotencyRecord<?> record = cache.asMap().compute(
                key,
                (k, existing) -> {

                    // Case 1: Record already exists
                    if (existing != null) {

                        log.info("""
                                IDEMPOTENCY CACHE HIT
                                key={}
                                createdAt={}
                                """,
                                key,
                                existing.getCreatedAt());

                        // Validate request hash
                        if (!existing.getRequestHash().equals(requestHash)) {

                            log.warn("""
                                    IDEMPOTENCY HASH MISMATCH
                                    key={}
                                    existingHash={}
                                    incomingHash={}
                                    """,
                                    key,
                                    existing.getRequestHash(),
                                    requestHash);

                            throw new ConflictException(
                            	    "Idempotency key already used with different request payload"
                            	);
                        }

                        return new IdempotencyRecord<>(
                                existing.getRequestHash(),
                                (T) existing.getResponseBody(),
                                existing.getCreatedAt(),
                                true 
                        );
                    }

                    // Case 2: No record → execute supplier
                    log.info("""
                            IDEMPOTENCY CACHE MISS
                            key={}
                            Processing supplier...
                            """,
                            key);

                    T response = supplier.get();

                    Instant now = Instant.now();

                    log.info("""
                            IDEMPOTENCY RECORD STORED
                            key={}
                            storedAt={}
                            """,
                            key,
                            now);

                    return new IdempotencyRecord<>(
                            requestHash,
                            response,
                            now
                    );
                });

        return (IdempotencyRecord<T>) record;
    }

    /**
     * Stores idempotency record manually.
     */
    @Override
    public <T> void put(
            String key,
            String requestHash,
            T response) {

        cache.put(
                key,
                new IdempotencyRecord<>(
                        requestHash,
                        response,
                        Instant.now()
                )
        );
    }
}