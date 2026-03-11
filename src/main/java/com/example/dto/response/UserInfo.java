package com.example.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Authenticated user information")
public class UserInfo {

	@Schema(description = "Unique identifier of the user", example = "550e8400-e29b-41d4-a716-446655440000")
	private UUID userId;

	@Schema(description = "Role assigned to the user", example = "CUSTOMER")
	private String role;

	@Schema(description = "Associated customer ID (null for admin)", example = "9b2d9f6e-9e1b-4c72-8f1c-1f9a4b7d2a11", nullable = true)
	private UUID customerId;
}