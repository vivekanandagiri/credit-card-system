package com.example.service.ServiceImpl;


import com.example.dto.request.LoginRequest;
import com.example.dto.request.RegisterRequest;
import com.example.dto.response.ApiResponse;
import com.example.dto.response.LoginResponse;
import com.example.dto.response.RegisterResponse;
import com.example.dto.response.UserInfo;
import com.example.entity.Customer;
import com.example.entity.User;
import com.example.exception.BusinessRuleException;
import com.example.exception.ConflictException;
import com.example.exception.InvalidCredentialsException;
import com.example.mapper.AuthMapper;
import com.example.repository.CustomerRepository;
import com.example.repository.UserRepository;
import com.example.security.JwtUtil;
import com.example.service.AuthService;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

	private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthMapper authMapper;

    public AuthServiceImpl(UserRepository userRepository,
    				CustomerRepository customerRepository,
                           PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil, AuthMapper authMapper) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
		this.authMapper = authMapper;
    }

    // =========================================
    // REGISTER
    // =========================================
    @Override
    public ApiResponse<RegisterResponse> register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail()))
            throw new ConflictException("Email already registered: " + request.getEmail());
        
        if(userRepository.existsByMobileNumber(request.getMobileNumber()))
        	throw new ConflictException("Mobile number Aalready exist ");
        if(customerRepository.existsByPanNumber(request.getPanNumber()))
        	throw new ConflictException("PAN Already Exist");
        
        if (request.getDateOfBirth() != null &&
        	    request.getDateOfBirth().isAfter(LocalDate.now().minusYears(18))) {

        	    throw new BusinessRuleException(
        	        "Customer must be at least 18 years old"
        	    );
        	}
        
        LocalDate dob = request.getDateOfBirth();
        
        if(dob==null) {
        	throw new BusinessRuleException("Date of Birth is required");
        }
        
        if(dob.isAfter(LocalDate.now())) {
        	throw new BusinessRuleException("Date of Birth can not be in future");
        }
        
        if(dob.isAfter(LocalDate.now().minusYears(18))) {
        	 throw new BusinessRuleException(
         	        "Customer must be at least 18 years old"
         	    );
        }
        
        Customer customer = authMapper.toCustomer(request);
        customerRepository.save(customer);
        
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        User user = authMapper.toUser(request, customer, encodedPassword);

        userRepository.save(user);

        RegisterResponse registerResponse =
                new RegisterResponse(
                        "Customer registered successfully",
                        user.getUserId()
                );

       return new ApiResponse<>(
                Instant.now(),
                HttpStatus.CREATED.value(),
                "User registered successfully",
                registerResponse
        );
    }

    // =========================================
    // LOGIN
    // =========================================
    @Override
    public ApiResponse<LoginResponse> login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(InvalidCredentialsException::new);

        if (!user.isActive()) {
            throw new BusinessRuleException("Account is disabled");
        }

        if (user.isLocked()) {
            throw new BusinessRuleException("Account is locked");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }


        user.setLastLoginAt(LocalDateTime.now());

        String token = jwtUtil.generateToken(user);

        UserInfo userInfo = authMapper.toUserInfo(user);

        LoginResponse loginResponse = new LoginResponse(
                token,
                "Bearer",
                3000,
                userInfo
        );

        return new ApiResponse<>(
                Instant.now(),
                HttpStatus.OK.value(),
                "Login successful",
                loginResponse
        );
    }
}