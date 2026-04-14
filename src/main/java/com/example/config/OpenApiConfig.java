package com.example.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for OpenAPI (Swagger) documentation.
 *
 * <p>This class:
 * <ul>
 *     <li>Defines API metadata (title, version, description)</li>
 *     <li>Registers global tags for API grouping</li>
 *     <li>Configures JWT-based authentication (Bearer token)</li>
 * </ul>
 *
 * <p><b>Swagger UI Usage:</b>
 * <ol>
 *     <li>Login via /api/auth/login</li>
 *     <li>Copy JWT token</li>
 *     <li>Click "Authorize" in Swagger UI</li>
 *     <li>Enter: Bearer &lt;your-token&gt;</li>
 * </ol>
 *
 * <p>All secured endpoints require JWT authorization.
 */
@Configuration
@OpenAPIDefinition(

		info = @Info(
				title = "Credit Card System",
				version = "v1",
				description = """
                        This REST API provides complete Credit Card Management functionality.

                        Core Modules:
                        - Customer Registration & Profile Management
                        - Credit Card Application Processing
                        - Underwriting & Risk Evaluation
                        - Credit Card Account Management
                        - Transactions & Billing
                        - Payments Processing
                        - Fraud Monitoring

                        Authentication Flow:
                        1. Login using /api/auth/login
                        2. Copy the JWT token from response
                        3. Click 'Authorize' in Swagger UI
                        4. Paste: Bearer <your-token>

                        All secured endpoints require JWT authorization.
                        """
		),

		tags = {
				@Tag(name = "Authentication", description = "Authentication APIs"),
				@Tag(name = "Customer Profile", description = "Customer profile management APIs"),
				@Tag(name = "Customer Address", description = "Customer address management APIs"),
				@Tag(name = "KYC Management", description = "KYC submission and verification APIs"),
				@Tag(name = "Credit Products", description = "Admin Credit Product Management APIs"),
				@Tag(name = "Credit Account Applications", description = "Credit Account application management APIs"),
				@Tag(name = "Credit Account API", description = "Credit Account management endpoints"),
				@Tag(name = "Credit Card API", description = "Credit card management APIs"),
				@Tag(name = "Card Products", description = "Card product management APIs"),
				@Tag(name = "Transaction API", description = "Operations related to transactions"),
				@Tag(name = "Billing Statement API", description = "Billing statement related APIs"),
				@Tag(name = "Payments", description = "Payment processing APIs")
		},

		// Apply JWT globally
		security = @SecurityRequirement(name = "bearerAuth")
)
public class OpenApiConfig {

	/**
	 * Configures OpenAPI bean with JWT security scheme.
	 *
	 * @return configured OpenAPI instance
	 */
	@Bean
	public OpenAPI customOpenAPI() {

		return new OpenAPI()

				// Global security requirement
				.addSecurityItem(
						new io.swagger.v3.oas.models.security.SecurityRequirement()
								.addList("bearerAuth")
				)

				// Security configuration
				.components(
						new Components()
								.addSecuritySchemes(
										"bearerAuth",
										new SecurityScheme()
												.name("Authorization")
												.type(SecurityScheme.Type.HTTP)
												.scheme("bearer")
												.bearerFormat("JWT")
												.description("Enter JWT token in format: Bearer <token>")
								)
				);
	}
}