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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;

/**
 * AuthServiceImpl class for user authentication and registration.
 */
@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private static final int MINIMUM_AGE_YEARS = 18;

    private final UserRepository userRepository;   
    private final CustomerService customerService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthMapper authMapper;

    // Injecting dependencies using constructor
    public AuthServiceImpl(UserRepository userRepository,
                           CustomerService customerService,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil, AuthMapper authMapper) {
        this.userRepository = userRepository;
        this.customerService = customerService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authMapper = authMapper;
    }
    
    // Getting jwt expiration time from application.properties
    @Value("${jwt.expiration}")
    private long jwtExpiration;

    /**
     * Method to register a new user
     *
     * @param request the register request
     * @return RegisterResponse
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
     * Method to login a user
     *
     * @param request the login request
     * @return LoginResponse
     */
    @Override
    public LoginResponse login(LoginRequest request) {

        // Find the user by email, throw exception if not found
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(InvalidCredentialsException::new);

        // Check if user is active and not locked
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
    
 // ── VALIDATIONS ─────────────────────────

    // Helper method to check unique fields
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

    // Helper method to check account state
    private void validateAccountState(User user) {

        if (!user.isActive()) {
            throw new BusinessRuleException("Account is disabled");
        }

        if (user.isLocked()) {
            throw new BusinessRuleException("Account is locked");
        }
    }
}