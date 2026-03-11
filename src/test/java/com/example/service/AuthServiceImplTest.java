package com.example.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import com.example.dto.request.LoginRequest;
import com.example.dto.request.RegisterRequest;
import com.example.dto.response.ApiResponse;
import com.example.dto.response.RegisterResponse;
import com.example.entity.Customer;
import com.example.entity.User;
import com.example.enums.Gender;
import com.example.enums.UserRole;
import com.example.exception.BusinessRuleException;
import com.example.exception.ConflictException;
import com.example.exception.InvalidCredentialsException;
import com.example.mapper.AuthMapper;
import com.example.repository.CustomerRepository;
import com.example.repository.UserRepository;
import com.example.security.JwtUtil;
import com.example.service.ServiceImpl.AuthServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private AuthMapper authMapper;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest registerRequest;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        registerRequest = new RegisterRequest(
                "vivek@gmail.com",
                "9765432101",
                "Password@123",
                "Vivek",
                "Giri",
                LocalDate.of(2000, 8, 15),
                Gender.MALE,
                "ABCDE1234F",
                "RESIDENT",
                "India"
        );
    }

    // REGISTER TESTS
    @Nested
    @DisplayName("Register Service Tests")
    class RegisterTests {

    	@Test
    	void register_success() {

    	    Customer customer = new Customer();
    	    User user = new User();
    	    user.setUserId(UUID.randomUUID());

    	    when(userRepository.existsByEmail(any())).thenReturn(false);
    	    when(userRepository.existsByMobileNumber(any())).thenReturn(false);
    	    when(customerRepository.existsByPanNumber(any())).thenReturn(false);

    	    when(passwordEncoder.encode(any())).thenReturn("encoded-password");

    	    when(authMapper.toCustomer(registerRequest)).thenReturn(customer);
    	    when(authMapper.toUser(any(), any(), any())).thenReturn(user);

    	    ApiResponse<RegisterResponse> response = authService.register(registerRequest);

    	    assertEquals(201, response.getStatus());
    	    assertEquals("User registered successfully", response.getMessage());

    	    verify(customerRepository).save(customer);
    	    verify(userRepository).save(user);
    	}

    	@Test
        void register_duplicate_email() {

            when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

            assertThrows(ConflictException.class, () -> {
                authService.register(registerRequest);
            });
        }

        @Test
        void register_duplicate_mobile() {

            when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
            when(userRepository.existsByMobileNumber(registerRequest.getMobileNumber())).thenReturn(true);

            assertThrows(ConflictException.class, () -> {
                authService.register(registerRequest);
            });
        }

        @Test
        void register_duplicate_pan() {

            when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
            when(userRepository.existsByMobileNumber(registerRequest.getMobileNumber())).thenReturn(false);
            when(customerRepository.existsByPanNumber(registerRequest.getPanNumber())).thenReturn(true);

            assertThrows(ConflictException.class, () -> {
                authService.register(registerRequest);
            });
        }

        @Test
        void register_dob_missing() {

            registerRequest.setDateOfBirth(null);

            when(userRepository.existsByEmail(any())).thenReturn(false);
            when(userRepository.existsByMobileNumber(any())).thenReturn(false);
            when(customerRepository.existsByPanNumber(any())).thenReturn(false);

            assertThrows(BusinessRuleException.class, () -> {
                authService.register(registerRequest);
            });
        }

        @Test
        void register_underage_customer() {

            registerRequest.setDateOfBirth(LocalDate.now().minusYears(10));

            when(userRepository.existsByEmail(any())).thenReturn(false);
            when(userRepository.existsByMobileNumber(any())).thenReturn(false);
            when(customerRepository.existsByPanNumber(any())).thenReturn(false);

            assertThrows(BusinessRuleException.class, () -> {
                authService.register(registerRequest);
            });
        }

        @Test
        void register_future_dob() {

            registerRequest.setDateOfBirth(LocalDate.now().plusDays(1));

            when(userRepository.existsByEmail(any())).thenReturn(false);
            when(userRepository.existsByMobileNumber(any())).thenReturn(false);
            when(customerRepository.existsByPanNumber(any())).thenReturn(false);

            assertThrows(BusinessRuleException.class, () -> {
                authService.register(registerRequest);
            });
        }
    }

    // LOGIN TESTS
    @Nested
    @DisplayName("Login Service Tests")
    class LoginTests {

        @Test
        void login_success() {

            LoginRequest request = new LoginRequest();
            request.setEmail("vivek@gmail.com");
            request.setPassword("Password@123");

            User user = new User();
            user.setUserId(UUID.randomUUID());
            user.setEmail("vivek@gmail.com");
            user.setPasswordHash("encoded-password");
            user.setRole(UserRole.CUSTOMER);
            user.setActive(true);

            when(userRepository.findByEmail(request.getEmail()))
                    .thenReturn(Optional.of(user));

            when(passwordEncoder.matches(any(), any())).thenReturn(true);
            when(jwtUtil.generateToken(user)).thenReturn("jwt-token");

            var response = authService.login(request);

            assertEquals(200, response.getStatus());
            assertEquals("Login successful", response.getMessage());
        }

        @Test
        void login_user_not_found() {

            LoginRequest request = new LoginRequest();
            request.setEmail("unknown@gmail.com");

            when(userRepository.findByEmail(any()))
                    .thenReturn(Optional.empty());

            assertThrows(InvalidCredentialsException.class, () -> {
                authService.login(request);
            });
        }

        @Test
        void login_invalid_password() {

            LoginRequest request = new LoginRequest();
            request.setEmail("vivek@gmail.com");
            request.setPassword("wrong");

            User user = new User();
            user.setEmail("vivek@gmail.com");
            user.setPasswordHash("encoded-password");
            user.setActive(true);

            when(userRepository.findByEmail(any()))
                    .thenReturn(Optional.of(user));

            when(passwordEncoder.matches(any(), any())).thenReturn(false);

            assertThrows(InvalidCredentialsException.class, () -> {
                authService.login(request);
            });
        }
    }
}