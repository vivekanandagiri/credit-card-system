package com.example.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "Request object for updating customer profile details")
public class CustomerProfileUpdateRequest {

	@Schema(description = "Customer first name", example = "Amit", maxLength = 50, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
	@NotBlank(message = "First name cannot be blank")
	@Size(max = 50, message = "First name cannot exceed 50 characters")
	@Pattern(regexp = "^[A-Za-z]+$", message = "First name must contain only alphabets")
	private String firstName;

	@Schema(description = "Customer last name", example = "Sharma", maxLength = 50)
	@NotBlank(message = "Last name cannot be blank")
	@Size(max = 50, message = "Last name cannot exceed 50 characters")
	@Pattern(regexp = "^[A-Za-z]+$", message = "Last name must contain only alphabets")
	private String lastName;

	@Schema(description = "Customer date of birth (must be in the past and at least 18 years old)", example = "1995-08-15", format = "date")
	@NotNull(message = "Date of birth is required")
	@Past(message = "Date of birth must be in the past")
	private LocalDate dateOfBirth;

	
	@Schema(description = "Residency status of the customer", example = "RESIDENT", allowableValues = { "RESIDENT",
			"NON_RESIDENT", "NRI" }, maxLength = 30)
	@NotBlank(message = "Residency status cannot be blank")
	@Size(max = 30, message = "Residency status cannot exceed 30 characters")
	private String residencyStatus;

	@Schema(description = "Citizenship country", example = "India", maxLength = 100)
	@NotBlank(message = "Citizenship country cannot be blank")
	@Size(max = 100, message = "Citizenship country cannot exceed 100 characters")
	private String citizenshipCountry;
}