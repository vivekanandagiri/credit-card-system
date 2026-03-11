package com.example.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "Request object for updating customer profile details")
public class CustomerProfileUpdateRequest {

	@Schema(description = "Customer first name", example = "Amit")
	@Size(max = 50, message = "First name cannot exceed 50 characters")
	private String firstName;

	@Schema(description = "Customer last name", example = "Sharma")
	@Size(max = 50, message = "Last name cannot exceed 50 characters")
	private String lastName;

	@Schema(description = "Customer date of birth (must be in the past and 18+ years old)", example = "1995-08-15")
	@Past(message = "Date of birth must be in the past")
	private LocalDate dateOfBirth;

	@Schema(description = "Residency status of the customer", example = "RESIDENT")
	@Size(max = 30, message = "Residency status cannot exceed 30 characters")
	private String residencyStatus;

	@Schema(description = "Citizenship country", example = "India")
	@Size(max = 100, message = "Citizenship country cannot exceed 100 characters")
	private String citizenshipCountry;
}