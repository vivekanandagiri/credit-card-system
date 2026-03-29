package com.example.util;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generates a unique transaction reference number.
 *
 * Format: TXN{YYYYMMDD}{HHmmss}{SEQ(4)}
 * Example: TXN202603200934150001
 *
 * Components:
 *   TXN       — fixed prefix
 *   YYYYMMDD  — date
 *   HHmmss    — time (hours, minutes, seconds)
 *   SEQ(4)    — 4-digit sequence, resets every second
 *
 * This guarantees uniqueness within the same JVM instance.
 * In a distributed system, this would use a DB sequence or UUID-based reference.
 */
@Component
public class ReferenceNumberGenerator {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // Sequence counter — resets each time a new timestamp second is encountered
    private final AtomicInteger sequence = new AtomicInteger(0);
    private volatile String lastTimestamp = "";

    public synchronized String generate() {

        String timestamp = LocalDateTime.now().format(FORMATTER);

        // Reset sequence if we're in a new second
        if (!timestamp.equals(lastTimestamp)) {
            sequence.set(0);
            lastTimestamp = timestamp;
        }

        int seq = sequence.incrementAndGet();
        return "TXN" + timestamp + String.format("%04d", seq);
    }
}