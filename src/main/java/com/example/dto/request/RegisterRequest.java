package com.example.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

import com.example.enums.Gender;
import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request object for registring a new customer with user credentials")
public class RegisterRequest {

	// USER (authentication)
	@Schema(description = "Email used for login", example = "amit.sharma@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "Email is required")
	@Email(message = "Invalid email format")
	@Size(max = 100, message = "Email cannot exceed 100 characters")
	private String email;

	@Schema(description = "10-digit mobile number starting with 6-9", example = "9876543210", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "Mobile number is required")
	@Pattern(regexp = "^[6-9]\\d{9}$", message = "Mobile number must be 10 digits and start with 6-9")
	private String mobileNumber;

	@Schema(description = "Password (8-20 characters, must contain uppercase, number, and special character)", example = "Secure@123", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "Password is required")
	@Size(min = 8, max = 20, message = "Password must be between 8 and 20 characters")
	@Pattern(regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).+$", message = "Password must contain at least one uppercase letter, one number, and one special character")
	private String password;

	// CUSTOMER

	@Schema(description = "Customer first name", example = "Amit", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "First name is required")
	@Size(max = 50, message = "First name cannot exceed 50 characters")
	private String firstName;

	@Schema(description = "Customer last name", example = "Sharma", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "Last name is required")
	@Size(max = 50, message = "Last name cannot exceed 50 characters")
	private String lastName;

	@Schema(description = "Format[yyyy-MM-dd] Customer date of birth (must be at least 18 years old)", example = "1995-08-15", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotNull(message = "Date of Birth is Required")
	@Past(message = "Date of birth must be in the past")
	@JsonFormat(pattern = "yyyy-MM-dd")
	private LocalDate dateOfBirth;

	@Schema(description = "Customer gender", example = "MALE", requiredMode = Schema.RequiredMode.REQUIRED)
	private Gender gender;

	@Schema(description = "PAN number (Indian tax ID)", example = "ABCDE1234F", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "PAN number is required")
	@Pattern(regexp = "^[A-Z]{5}[0-9]{4}[A-Z]{1}$", message = "Invalid PAN format (e.g., ABCDE1234F)")
	private String panNumber;

	@Schema(description = "Residency status of the customer", example = "RESIDENT", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "Residency status is required")
	private String residencyStatus;

	@Schema(description = "Citizenship country", example = "India", requiredMode = Schema.RequiredMode.REQUIRED)
	@NotBlank(message = "Citizenship country is required")
	@Size(max = 100, message = "Citizenship country cannot exceed 100 characters")
	private String citizenshipCountry;
}