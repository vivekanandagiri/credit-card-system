package com.example.service;

import com.example.entity.User;
import com.example.exception.UserNotFoundException;
import com.example.repository.UserRepository;
import com.example.service.ServiceImpl.UserServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        user = new User();
        user.setUserId(userId);
    }

    @Nested
    @DisplayName("getUser")
    class GetUserTests {

        @Test
        void shouldReturnUserWhenFound() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            User result = userService.getUser(userId);

            assertThat(result).isSameAs(user);
            verify(userRepository).findById(userId);
        }

        @Test
        void shouldThrowUserNotFoundExceptionWhenUserMissing() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class,
                    () -> userService.getUser(userId));

            verify(userRepository).findById(userId);
        }
    }
}