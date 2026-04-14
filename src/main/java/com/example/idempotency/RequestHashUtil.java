package com.example.idempotency;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

@Component
public class RequestHashUtil {

    private final ObjectMapper objectMapper;

    public RequestHashUtil() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(
                SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS,
                true);
    }

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