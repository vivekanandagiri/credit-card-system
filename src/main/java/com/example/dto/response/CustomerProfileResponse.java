package com.example.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@AllArgsConstructor
@Schema(description = "Customer profile details")
public class CustomerProfileResponse {

    @Schema(
            description = "Unique identifier of the customer",
            example = "550e8400-e29b-41d4-a716-446655440000"
    )
    private UUID customerId;

    @Schema(
            description = "Customer first name",
            example = "John"
    )
    private String firstName;

    @Schema(
            description = "Customer last name",
            example = "Doe"
    )
    private String lastName;

    @Schema(
            description = "Customer date of birth",
            example = "1995-08-15"
    )
    private LocalDate dateOfBirth;

    @Schema(
            description = "Registered email address",
            example = "john.doe@example.com"
    )
    private String email;

    @Schema(
            description = "Registered mobile number",
            example = "9876543210"
    )
    private String phone;

    @Schema(
            description = "PAN number (masked for security)",
            example = "ABCDE****F"
    )
    private String panNumber;

    @Schema(
            description = "Residency status",
            example = "RESIDENT"
    )
    private String residencyStatus;

    @Schema(
            description = "Citizenship country",
            example = "India"
    )
    private String citizenshipCountry;
}