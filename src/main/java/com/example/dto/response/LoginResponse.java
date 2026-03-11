package com.example.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response returned after successful authentication")
public class LoginResponse {

	@Schema(description = "JWT access token used for authenticated requests", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
	private String accessToken;

	@Schema(description = "Type of the token", example = "Bearer")
	private String tokenType;

	@Schema(description = "Access token expiration time in seconds", example = "3600")
	private long expiresIn;

	@Schema(description = "Authenticated user information")
	private UserInfo user;
}