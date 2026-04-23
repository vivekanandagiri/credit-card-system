package com.example.service.ServiceImpl;

import com.example.dto.request.LoginRequest;
import com.example.dto.request.RegisterRequest;
import com.example.dto.response.LoginResponse;
import com.example.dto.response.RegisterResponse;
import com.example.entity.Customer;
import com.example.entity.User;
import com.example.exception.BusinessRuleException;
import com.example.exception.ConflictException;
import com.example.exception.InvalidCredentialsException;
import com.example.mapper.AuthMapper;
import com.example.repository.UserRepository;
import com.example.security.JwtUtil;
import com.example.service.AuthService;
import com.example.service.CustomerService;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Implementation of {@link AuthService} responsible for:
 * <ul>
 *     <li>User registration (identity + credentials)</li>
 *     <li>Authentication (credential verification)</li>
 *     <li>JWT token issuance</li>
 * </ul>
 *
 * <p><b>Security Considerations:</b></p>
 * <ul>
 *     <li>Passwords are securely hashed using {@link PasswordEncoder}</li>
 *     <li>JWT tokens are generated after successful authentication</li>
 *     <li>Account state (active/locked) is validated before password check</li>
 * </ul>
 *
 * <p><b>Business Rules:</b></p>
 * <ul>
 *     <li>Email, mobile number, and PAN must be unique</li>
 *     <li>User must be at least 18 years old</li>
 * </ul>
 */
@Service
@Transactional
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final int MINIMUM_AGE_YEARS = 18;

    private final UserRepository userRepository;   
    private final CustomerService customerService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthMapper authMapper;

    
    /**
     * JWT expiration time in seconds (configured via application properties).
     */    @Value("${jwt.expiration}")
    private long jwtExpiration;

     /**
      * Registers a new user in the system.
      *
      * <p>Steps:</p>
      * <ol>
      *     <li>Validate uniqueness (email, mobile, PAN)</li>
      *     <li>Validate age and date of birth</li>
      *     <li>Create and persist {@link Customer}</li>
      *     <li>Create and persist {@link User} with encoded password</li>
      * </ol>
      *
      * @param request registration request payload
      * @return {@link RegisterResponse} containing created user ID
      *
      * @throws ConflictException if email/mobile/PAN already exists
      * @throws BusinessRuleException if DOB is invalid or age < 18
      */
    @Override
    public RegisterResponse register(RegisterRequest request) {

        // Validate the request data first
        validateUniqueFields(request);
        validateDateOfBirth(request.getDateOfBirth());

        // Convert DTO to Customer entity
        Customer customer = authMapper.toCustomer(request);
        
        // Save customer to the database
        customerService.saveCustomer(customer);

        // Convert DTO to User entity and encode the password
        User user = authMapper.toUser(
                request,
                customer,
                passwordEncoder.encode(request.getPassword())
        );

        // Save user to the database
        userRepository.save(user);

        // Return the response with user id
        return new RegisterResponse(user.getUserId());
    }

    /**
     * Authenticates a user and generates a JWT token.
     *
     * <p>Steps:</p>
     * <ol>
     *     <li>Fetch user by email</li>
     *     <li>Validate account state (active & not locked)</li>
     *     <li>Verify password</li>
     *     <li>Update last login timestamp</li>
     *     <li>Generate JWT token</li>
     * </ol>
     *
     * @param request login request containing credentials
     * @return {@link LoginResponse} with JWT token and user info
     *
     * @throws InvalidCredentialsException if email not found or password invalid
     * @throws BusinessRuleException if account is locked or disabled
     */
    @Override
    public LoginResponse login(LoginRequest request) {

        // Find the user by email, throw exception if not found
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(InvalidCredentialsException::new);

        // Validate account state BEFORE checking the password,Check if user is active and not locked
        validateAccountState(user);

        // Check if the provided password matches the encrypted password in db
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException(); // throw error if password wrong
        }

        // Set the last login time
        user.setLastLoginAt(Instant.now());

        // Generate JWT token for the user
        String token = jwtUtil.generateToken(user);

        // Return the login response with token and user info
        return new LoginResponse(
                token,
                "Bearer",
                jwtExpiration,
                authMapper.toUserInfo(user)
        );
    }
    
 // ---------------------- VALIDATIONS ---------------------------------

    /**
     * Validates uniqueness of user registration fields.
     *
     * @param request registration request
     * @throws ConflictException if any unique constraint is violated
     */
    private void validateUniqueFields(RegisterRequest request) {

        // Check if email exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already registered");
        }

        // Check if mobile exists
        if (userRepository.existsByMobileNumber(request.getMobileNumber())) {
            throw new ConflictException("Mobile number already exists");
        }

        // Check if PAN exists
        if (customerService.panNumberExists(request.getPanNumber())) {
            throw new ConflictException("PAN already exists");
        }
    }

    // Helper method to validate DOB
    private void validateDateOfBirth(LocalDate dob) {

        if (dob == null) {
            throw new BusinessRuleException("Date of birth is required");
        }

        // DOB cannot be future date
        if (dob.isAfter(LocalDate.now())) {
            throw new BusinessRuleException("Date of birth cannot be in the future");
        }

        // Check if user is 18 years old
        if (dob.isAfter(LocalDate.now().minusYears(MINIMUM_AGE_YEARS))) {
            throw new BusinessRuleException(
                    "Customer must be at least " + MINIMUM_AGE_YEARS + " years old"
            );
        }
    }

    /**
     * Validates date of birth and minimum age requirement.
     *
     * @param dob date of birth
     * @throws BusinessRuleException if DOB is null, future, or age < 18
     */
    private void validateAccountState(User user) {

        if (!user.isActive()) {
            throw new BusinessRuleException("Account is disabled");
        }

        if (user.isLocked()) {
            throw new BusinessRuleException("Account is locked");
        }
    }
}