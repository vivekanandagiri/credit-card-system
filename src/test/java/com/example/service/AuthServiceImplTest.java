package com.example.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.util.Optional;

import com.example.dto.request.LoginRequest;
import com.example.dto.request.RegisterRequest;
import com.example.dto.response.RegisterResponse;
import com.example.entity.Customer;
import com.example.entity.User;
import com.example.exception.BusinessRuleException;
import com.example.exception.ConflictException;
import com.example.exception.InvalidCredentialsException;
import com.example.mapper.AuthMapper;
import com.example.repository.UserRepository;
import com.example.security.JwtUtil;
import com.example.service.ServiceImpl.AuthServiceImpl;

import com.example.testutil.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Unit tests for {@link AuthServiceImpl}.
 */

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private CustomerService customerService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthMapper authMapper;
    @Mock private JwtUtil jwtUtil;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest request;

    @BeforeEach
    void setUp() {
        request = TestFixtures.validRegisterRequest();
    }

    // ================= REGISTER =================

    @Nested
    @DisplayName("Register Tests")
    class RegisterTests {

        @Test
        void shouldRegisterUserSuccessfully_whenValidRequest() {
            // GIVEN
            Customer customer = new Customer();
            User user = TestFixtures.validUser();

            mockValidChecks();

            when(passwordEncoder.encode(any())).thenReturn("encoded");
            when(authMapper.toCustomer(request)).thenReturn(customer);
            when(authMapper.toUser(any(), any(), any())).thenReturn(user);
            when(customerService.saveCustomer(customer)).thenReturn(customer);

            // WHEN
            RegisterResponse response = authService.register(request);

            // THEN
            assertThat(response.getUserId()).isEqualTo(user.getUserId());

            verify(userRepository).save(user);
            verify(customerService).saveCustomer(customer);
        }

        @Test
        void shouldThrowConflict_whenEmailExists() {
            // GIVEN
            when(userRepository.existsByEmail(request.getEmail()))
                    .thenReturn(true);

            // WHEN + THEN
            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(ConflictException.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        void shouldThrowConflict_whenMobileExists() {
            // GIVEN
            when(userRepository.existsByEmail(any())).thenReturn(false);
            when(userRepository.existsByMobileNumber(any())).thenReturn(true);

            // WHEN + THEN
            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(ConflictException.class);
        }

        @Test
        void shouldThrowException_whenDobIsNull() {
            request.setDateOfBirth(null);
            mockValidChecks();

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(BusinessRuleException.class);
        }
    }

    // ================= LOGIN =================

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {

        @Test
        void shouldLoginSuccessfully_whenValidCredentials() {
            // GIVEN
            LoginRequest login = TestFixtures.validLoginRequest();
            User user = TestFixtures.validUser();

            when(userRepository.findByEmail(login.getEmail()))
                    .thenReturn(Optional.of(user));
            when(passwordEncoder.matches(any(), any())).thenReturn(true);
            when(jwtUtil.generateToken(user)).thenReturn("jwt-token");

            // WHEN
            var response = authService.login(login);

            // THEN
            assertThat(response.getAccessToken()).isEqualTo("jwt-token");
        }

        @Test
        void shouldThrowException_whenUserNotFound() {
            when(userRepository.findByEmail(any()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(new LoginRequest()))
                    .isInstanceOf(InvalidCredentialsException.class);
        }

        @Test
        void shouldThrowException_whenAccountLocked() {
            User user = TestFixtures.validUser();
            user.setLocked(true);

            when(userRepository.findByEmail(any()))
                    .thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.login(TestFixtures.validLoginRequest()))
                    .isInstanceOf(BusinessRuleException.class);

            verify(passwordEncoder, never()).matches(any(), any());
        }
    }

    // ================= HELPER =================

    private void mockValidChecks() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.existsByMobileNumber(any())).thenReturn(false);
        when(customerService.panNumberExists(any())).thenReturn(false);
    }
}