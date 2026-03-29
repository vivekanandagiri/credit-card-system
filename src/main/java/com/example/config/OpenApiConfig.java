package com.example.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(

    info = @Info(
        title = "Credit Card System ",
        version = "1.0.0",

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
    		        @Tag(name = "Transaction API", description = "Operations related to transactions")
    		    },

    // Apply JWT globally
    security = @SecurityRequirement(name = "bearerAuth")
)
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {

        return new OpenAPI()

            // Enable JWT globally
            .addSecurityItem(
                new io.swagger.v3.oas.models.security.SecurityRequirement()
                    .addList("bearerAuth")
            )

            .components(
                new Components()

                    .addSecuritySchemes(
                        "bearerAuth",

                        new SecurityScheme()
                            .name("Authorization")
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                    )
            );
    }
}
