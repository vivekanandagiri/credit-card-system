package com.example.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response returned after successful user registration")
public class RegisterResponse {

	@Schema(description = "Confirmation message", example = "User registered successfully")
	private String message;

	@Schema(description = "Unique identifier of the newly created user", example = "550e8400-e29b-41d4-a716-446655440000")
	private UUID userId;
}