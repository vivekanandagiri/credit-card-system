package com.example.idempotency;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Utility class for generating a deterministic hash of request objects.
 *
 * <p>This component serializes a given request object into JSON and produces a
 * SHA-256 hash of the serialized representation. The resulting hash is used
 * to ensure idempotency by uniquely identifying request payloads.</p>
 *
 * <p><b>Key characteristics:</b></p>
 * <ul>
 *     <li>Deterministic hashing using consistent JSON serialization</li>
 *     <li>Map entries are ordered to avoid hash mismatches due to key ordering</li>
 *     <li>UTF-8 encoding is used for platform-independent hashing</li>
 *     <li>Uses SHA-256 for strong collision resistance</li>
 * </ul>
 *
 * <p><b>Typical usage:</b></p>
 * <pre>
 * String hash = requestHashUtil.hash(request);
 * </pre>
 *
 * <p>This hash is typically used alongside an idempotency key to:</p>
 * <ul>
 *     <li>Detect duplicate requests</li>
 *     <li>Prevent re-processing of identical operations</li>
 *     <li>Validate that repeated requests have identical payloads</li>
 * </ul>
 *
 * <p><b>Note:</b> This implementation relies on Jackson serialization. Changes
 * in object structure or serialization configuration may affect hash output.</p>
 */
@Component
public class RequestHashUtil {

    private final ObjectMapper objectMapper;

    /**
     * Constructs a {@code RequestHashUtil} with a configured {@link ObjectMapper}.
     *
     * <p>Map entries are sorted by keys to ensure deterministic JSON output,
     * preventing hash inconsistencies due to unordered map serialization.</p>
     */
    public RequestHashUtil() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(
                SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS,
                true);
    }

    /**
     * Generates a SHA-256 hash for the given request object.
     *
     * <p>The request is first serialized into a JSON string and then hashed using
     * the SHA-256 algorithm. The resulting hash is returned as a hexadecimal string.</p>
     *
     * @param request the request object to hash
     * @return a hexadecimal string representing the SHA-256 hash of the request
     * @throws RuntimeException if serialization or hashing fails
     */
    public String hash(Object request) {
        try {
            String json = objectMapper.writeValueAsString(request);

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hashBytes = digest.digest(
                    json.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();

            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to hash request",
                    e);
        }
    }
}