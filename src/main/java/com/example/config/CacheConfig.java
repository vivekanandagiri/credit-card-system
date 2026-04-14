package com.example.config;

import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.idempotency.IdempotencyRecord;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Configuration class for application-level caching.
 *
 * <p>This configuration provides a cache for handling idempotency,
 * ensuring that duplicate requests (e.g., payment retries) are
 * processed safely without duplication.
 *
 * <p><b>Cache Characteristics:</b>
 * <ul>
 *     <li>Key: Idempotency key (String)</li>
 *     <li>Value: {@link IdempotencyRecord}</li>
 *     <li>Expiry: 15 minutes after write</li>
 *     <li>Max size: 100,000 entries</li>
 * </ul>
 *
 * <p><b>Use Cases:</b>
 * <ul>
 *     <li>Prevent duplicate payments and transaction</li>
 *     <li>Ensure safe retries for APIs</li>
 *     <li>Handle network retries gracefully</li>
 * </ul>
 *
 * <p><b>Note:</b>
 * This cache is in-memory and local to a single instance.
 * For distributed systems, use Redis or a shared cache.
 */
@Configuration
public class CacheConfig {

    /**
     * Cache for storing idempotency records.
     *
     * @return Caffeine cache instance
     */
    @Bean
    public Cache<String, IdempotencyRecord<?>> idempotencyCache() {

        return Caffeine.newBuilder()

                // Expire entries 15 minutes after creation
                .expireAfterWrite(15, TimeUnit.MINUTES)

                // Maximum number of entries
                .maximumSize(100_000)

                // Record stats for monitoring (optional but recommended)
                .recordStats()

                .build();
    }
}