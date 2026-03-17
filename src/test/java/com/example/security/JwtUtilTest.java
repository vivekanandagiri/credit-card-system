package com.example.security;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.entity.Customer;
import com.example.entity.User;
import com.example.enums.UserRole;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private User user;

    @BeforeEach
    void setup() {

        jwtUtil = new JwtUtil();

        // set fields that @Value normally injects
        ReflectionTestUtils.setField(
                jwtUtil,
                "secret",
                "testsecretkeytestsecretkeytestsecretkey"
        );
        ReflectionTestUtils.setField(
                jwtUtil,
                "expiration",
                3600000L
        );

        // simulate @PostConstruct
        jwtUtil.init();
        Customer customer = new Customer();
        customer.setCustomerId(UUID.randomUUID());

        
        user = new User();
        user.setUserId(UUID.randomUUID());
        user.setEmail("vivek@gmail.com");
        user.setRole(UserRole.CUSTOMER);
        user.setCustomer(customer);
    }


    @Test
    void generate_token_success() {

        String token = jwtUtil.generateToken(user);

        assertNotNull(token);
        assertTrue(token.length() > 20);
    }


    @Test
    void extract_email_success() {

        String token = jwtUtil.generateToken(user);

        String email = jwtUtil.extractEmail(token);

        assertEquals("vivek@gmail.com", email);
    }



    @Test
    void extract_role_success() {

        String token = jwtUtil.generateToken(user);

        String role = jwtUtil.extractRole(token);

        assertEquals("CUSTOMER", role);
    }


    @Test
    void extract_expiration_success() {

        String token = jwtUtil.generateToken(user);

        Date expiration = jwtUtil.extractExpiration(token);

        assertNotNull(expiration);
        assertTrue(expiration.after(new Date()));
    }

    // =========================
    // VALIDATE TOKEN
    // =========================

    @Test
    void validate_token_success() {

        String token = jwtUtil.generateToken(user);

        boolean valid = jwtUtil.validateToken(token, "vivek@gmail.com");

        assertTrue(valid);
    }

    @Test
    void validate_token_wrong_email() {

        String token = jwtUtil.generateToken(user);

        boolean valid = jwtUtil.validateToken(token, "wrong@gmail.com");

        assertFalse(valid);
    }



    @Test
    void token_should_not_be_expired() {

        String token = jwtUtil.generateToken(user);

        boolean expired = jwtUtil.isExpired(token);

        assertFalse(expired);
    }


    @Test
    void is_token_valid_success() {

        String token = jwtUtil.generateToken(user);

        boolean valid = jwtUtil.isTokenValid(token);

        assertTrue(valid);
    }

    @Test
    void is_token_invalid() {

        String invalidToken = "invalid.jwt.token";

        boolean valid = jwtUtil.isTokenValid(invalidToken);

        assertFalse(valid);
    }

}