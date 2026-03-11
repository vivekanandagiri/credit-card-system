package com.example.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Getter
@NoArgsConstructor
@Schema(description = "Standard API error response structure")
public class ErrorResponse {

	@Schema(description = "Timestamp when the error occurred (UTC)", example = "2026-03-03T12:30:45Z")
	private Instant timestamp;

	@Schema(description = "HTTP status code", example = "400")
	private int status;

	@Schema(description = "HTTP error reason", example = "BAD_REQUEST")
	private String error;

	@Schema(description = "Detailed error message", example = "Validation failed")
	private String message;

	@Schema(description = "API endpoint path where the error occurred", example = "/api/v1/credit-products")
	private String path;

	@Schema(description = "Field-level validation errors (if applicable)")
	private Map<String, String> fieldErrors;

	// constructor WITHOUT field errors
	public ErrorResponse(Instant timestamp, int status, String error, String message, String path) {

		this.timestamp = timestamp;
		this.status = status;
		this.error = error;
		this.message = message;
		this.path = path;
	}

	// constructor WITH field errors
	public ErrorResponse(Instant timestamp, int status, String error, String message, String path,
			Map<String, String> fieldErrors) {

		this(timestamp, status, error, message, path);
		this.fieldErrors = fieldErrors;
	}

	// setter only for validation handler
	public void setFieldErrors(Map<String, String> fieldErrors) {
		this.fieldErrors = fieldErrors;
	}
}