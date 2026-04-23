package com.example.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

import org.springframework.http.HttpStatus;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Generic API response wrapper used for all REST endpoints.
 *
 * <p>This class standardizes API responses across the system by providing:
 * <ul>
 *     <li>Timestamp of response generation</li>
 *     <li>HTTP status code</li>
 *     <li>Human-readable message</li>
 *     <li>Optional response payload</li>
 * </ul>
 *
 *
 * @param <T> response payload type
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standard API response wrapper")
public class ApiResponse<T> {

    /**
     * Timestamp when the response is generated (UTC).
     *
     * <p>Example (UTC): 2026-04-22T10:15:30Z</p>
     * <p>Example (IST): 2026-04-22T15:45:30+05:30</p>
     */
    @Schema(
            description = "Response timestamp in UTC (ISO-8601 format)",
            example = "2026-04-22T15:45:30+05:30"
    )
    private Instant timestamp;

    /**
     * HTTP status code of the response.
     */
    @Schema(
            description = "HTTP status code",
            example = "200"
    )
    private Integer status;

    /**
     * Human-readable message describing the response.
     */
    @Schema(
            description = "Response message",
            example = "Payment processed successfully"
    )
    private String message;

    /**
     * Actual response payload.
     */
    @Schema(
            description = "Response data payload"
    )
    private T data;

    /**
     * Builds a success response with status, message, and data.
     *
     * @param status  HTTP status
     * @param message response message
     * @param data    payload
     * @return ApiResponse
     */
    public static <T> ApiResponse<T> success(HttpStatus status, String message, T data) {
        return ApiResponse.<T>builder()
                .timestamp(Instant.now()) // convert to time zone By Time zone resolver
                .status(status.value())
                .message(message)
                .data(data)
                .build();
    }

    /**
     * Builds a success response with message and data.
     *
     * @param message response message
     * @param data    payload
     * @return ApiResponse
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return ApiResponse.<T>builder()
                .timestamp(Instant.now())
                .message(message)
                .data(data)
                .build();
    }

    /**
     * Builds a success response with only a message.
     *
     * @param message response message
     * @return ApiResponse
     */
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .timestamp(Instant.now())
                .message(message)
                .build();
    }
    /**
     * Builds an error response with status and message.
     *
     * @param status  HTTP status
     * @param message error message
     * @return ApiResponse
     */
    public static <T> ApiResponse<T> error(HttpStatus status, String message) {
        return ApiResponse.<T>builder()
                .timestamp(Instant.now())
                .status(status.value())
                .message(message)
                .build();
    }

    /**
     * Builds an error response with only a message (default 400).
     *
     * @param message error message
     * @return ApiResponse
     */
    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .message(message)
                .build();
    }
}