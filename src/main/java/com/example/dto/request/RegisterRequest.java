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

/**
 * DTO for registering a new customer along with user authentication details.
 * <p>
 * This request contains both:
 * <ul>
 *     <li>User credentials (email, password, mobile)</li>
 *     <li>Customer personal details</li>
 * </ul>
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request object for registering a new customer with user credentials")
public class RegisterRequest {

    /**
     * Email address used for login.
     */
    @Schema(
        description = "Email used for login",
        example = "amit.sharma@example.com",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    private String email;

    /**
     * Mobile number of the user.
     * Must be a valid 10-digit Indian number starting with 6-9.
     */
    @Schema(
        description = "10-digit mobile number starting with 6-9",
        example = "9876543210",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Mobile number is required")
    @Pattern(
        regexp = "^[6-9]\\d{9}$",
        message = "Mobile number must be 10 digits and start with 6-9"
    )
    private String mobileNumber;

    /**
     * Password for the user account.
     * Must contain at least one uppercase letter, one number, and one special character.
     */
    @Schema(
        description = "Password (8-20 characters, must contain uppercase, number, and special character)",
        example = "Secure@123",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 20, message = "Password must be between 8 and 20 characters")
    @Pattern(
        regexp = "^(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).+$",
        message = "Password must contain at least one uppercase letter, one number, and one special character"
    )
    private String password;

    /**
     * First name of the customer.
     */
    @Schema(
        description = "Customer first name",
        example = "Amit",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "First name is required")
    @Size(max = 50, message = "First name cannot exceed 50 characters")
    private String firstName;

    /**
     * Last name of the customer.
     */
    @Schema(
        description = "Customer last name",
        example = "Sharma",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Last name is required")
    @Size(max = 50, message = "Last name cannot exceed 50 characters")
    private String lastName;

    /**
     * Date of birth of the customer.
     * Must be a past date and ideally represent age >= 18 (business validation required separately).
     */
    @Schema(
        description = "Customer date of birth (yyyy-MM-dd). Must be in the past and at least 18 years old",
        example = "1995-08-15",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;

    /**
     * Gender of the customer.
     */
    @Schema(
        description = "Customer gender",
        example = "MALE",
        implementation = Gender.class,
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Gender is required")
    private Gender gender;

    /**
     * PAN number (Permanent Account Number - India).
     */
    @Schema(
        description = "PAN number (Indian tax ID)",
        example = "ABCDE1234F",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "PAN number is required")
    @Pattern(
        regexp = "^[A-Z]{5}[0-9]{4}[A-Z]{1}$",
        message = "Invalid PAN format (e.g., ABCDE1234F)"
    )
    private String panNumber;

    /**
     * Residency status of the customer (e.g., RESIDENT, NRI).
     */
    @Schema(
        description = "Residency status of the customer (e.g., RESIDENT, NRI)",
        example = "RESIDENT",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Residency status is required")
    @Size(max = 50, message = "Residency status cannot exceed 50 characters")
    private String residencyStatus;

    /**
     * Country of citizenship.
     */
    @Schema(
        description = "Citizenship country",
        example = "India",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Citizenship country is required")
    @Size(max = 100, message = "Citizenship country cannot exceed 100 characters")
    private String citizenshipCountry;

    /**
     * Timezone of the customer.
     */
    @Schema(
        description = "Timezone of customer (IANA format)",
        example = "Asia/Kolkata",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Timezone is required")
    @Size(max = 50, message = "Timezone cannot exceed 50 characters")
    private String timezone;
}