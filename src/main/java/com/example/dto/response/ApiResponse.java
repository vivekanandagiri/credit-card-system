package com.example.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "Standard API success response wrapper")
public class ApiResponse<T> {

    @Schema(
            description = "Timestamp when the response was generated",
            example = "2026-03-03T10:15:30Z"
    )
    private Instant timestamp;

    @Schema(
            description = "HTTP status code",
            example = "200"
    )
    private int status;

    @Schema(
            description = "Response message describing the result",
            example = "Operation completed successfully"
    )
    private String message;

    @Schema(
            description = "Response payload data"
    )
    private T data;

    public static <T> ApiResponse<T> success(int status, String message, T data) {
        return ApiResponse.<T>builder()
                .timestamp(Instant.now())
                .status(status)
                .message(message)
                .data(data)
                .build();
    }
    
    public static <T> ApiResponse<T> success(int status, String message) {
        return ApiResponse.<T>builder()
                .timestamp(Instant.now())
                .status(status)
                .message(message)
                .build();
    }
}