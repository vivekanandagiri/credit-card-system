package com.example.idempotency;

import java.util.function.Supplier;

/**
 * Contract for idempotency handling.
 *
 * <p>This interface ensures that duplicate requests with the same idempotency key
 * are processed safely without executing business logic multiple times.
 *
 * <p><b>Core Idea:</b>
 * <ul>
 *     <li>If request with same key + hash exists → return stored response</li>
 *     <li>If not → execute supplier and store result</li>
 * </ul>
 *
 * <p><b>Typical Flow:</b>
 * <pre>
 * Client → sends Idempotency-Key
 *        ↓
 * Server → calls compute()
 *        ↓
 * If key exists → return cached response
 * Else → execute supplier → store result → return response
 * </pre>
 *
 * <p><b>Use Cases:</b>
 * <ul>
 *     <li>Payment APIs (prevent duplicate charges)</li>
 *     <li>Transaction processing</li>
 * </ul>
 */
public interface IdempotencyStore {

    /**
     * Computes or retrieves an idempotent response.
     *
     * <p>If a record exists for the given key:
     * <ul>
     *     <li>Same requestHash → return existing response</li>
     *     <li>Different requestHash → should throw conflict exception (implementation responsibility)</li>
     * </ul>
     *
     * <p>If no record exists:
     * <ul>
     *     <li>Execute supplier</li>
     *     <li>Store response</li>
     *     <li>Return response</li>
     * </ul>
     *
     * @param key unique idempotency key (usually from request header)
     * @param requestHash hash of request payload (to detect mismatched retries)
     * @param supplier business logic to execute if not cached
     * @param <T> response type
     * @return idempotency record containing response and metadata
     */
    <T> IdempotencyRecord<T> compute(
            String key,
            String requestHash,
            Supplier<T> supplier
    );

    /**
     * Stores a response for a given idempotency key.
     *
     * <p>This method is typically used internally after successful processing.
     *
     * @param key idempotency key
     * @param requestHash request payload hash
     * @param response response to store
     * @param <T> response type
     */
    <T> void put(
            String key,
            String requestHash,
            T response
    );
}