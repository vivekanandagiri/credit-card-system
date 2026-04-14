package com.example.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.example.dto.request.RegisterRequest;
import com.example.dto.response.RegisterResponse;
import com.example.repository.UserRepository;
import com.example.service.ServiceImpl.AuthServiceImpl;
import com.example.testutil.TestFixtures;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthServiceIntegrationTest {

    @Autowired
    private AuthServiceImpl authService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldRegisterUser_andPersistInPostgres() {
        // GIVEN
        RegisterRequest request = TestFixtures.validRegisterRequest();

        // WHEN
        RegisterResponse response = authService.register(request);

        // THEN
        assertThat(response.getUserId()).isNotNull();
        assertThat(userRepository.existsByEmail(request.getEmail())).isTrue();
    }
    @Test
    void shouldThrowException_whenEmailAlreadyExists() {
        RegisterRequest request = TestFixtures.validRegisterRequest();

        authService.register(request);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(Exception.class);
    }
    @Test
    void shouldLoginSuccessfully() {
        RegisterRequest register = TestFixtures.validRegisterRequest();
        authService.register(register);

        var login = TestFixtures.validLoginRequest();

        var response = authService.login(login);

        assertThat(response.getAccessToken()).isNotNull();
    }
    @Test
    void shouldFailLogin_whenPasswordIncorrect() {
        RegisterRequest register = TestFixtures.validRegisterRequest();
        authService.register(register);

        var login = TestFixtures.validLoginRequest();
        login.setPassword("wrong");

        assertThatThrownBy(() -> authService.login(login))
                .isInstanceOf(Exception.class);
    }
}