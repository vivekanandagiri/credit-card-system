package com.example.service;

import com.example.dto.request.CustomerProfileUpdateRequest;
import com.example.dto.response.CustomerProfileResponse;
import com.example.entity.Customer;
import com.example.exception.BadRequestException;
import com.example.exception.ProfileNotCreatedException;
import com.example.exception.UserNotFoundException;
import com.example.mapper.CustomerProfileMapper;
import com.example.repository.CustomerRepository;
import com.example.service.ServiceImpl.CustomerProfileServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CustomerProfileServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class CustomerProfileServiceImplTest {

    // FIX: UserService replaces UserRepository
    @Mock  private CustomerService customerService;
    @Mock  private CustomerRepository customerRepository;
    // @Spy kept — tests assert on real mapped field values
    @Spy   private CustomerProfileMapper profileMapper = new CustomerProfileMapper();

    @InjectMocks
    private CustomerProfileServiceImpl service;

    private UUID userId;
    private Customer customer;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        // FIX: User object removed — service no longer resolves via User entity;
        // UserService.getCustomerByUserId() returns Customer directly.
        customer = new Customer();
        customer.setCustomerId(UUID.randomUUID());
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setDateOfBirth(LocalDate.of(1995, 8, 15));
        customer.setEmail("john@example.com");
        customer.setPhone("9876543210");
        customer.setPanNumber("ABCDE1234F");
        customer.setResidencyStatus("RESIDENT");
        customer.setCitizenshipCountry("India");
    }

    // =========================================================================
    // getProfile
    // =========================================================================

    @Nested
    @DisplayName("Get Profile")
    class GetProfileTests {

        @Test
        void shouldGetProfileSuccessfully() {
            // FIX: userRepository.findById() → userService.getCustomerByUserId()
            // returns Customer directly — no Optional or User unwrap
            when(customerService.getCustomerByUserId(userId)).thenReturn(customer);

            CustomerProfileResponse response = service.getProfile(userId);

            assertNotNull(response);
    
            // Real @Spy mapper maps firstName from the customer entity
            assertEquals("John", response.getFirstName());

            verify(customerService).getCustomerByUserId(userId);
        }

        @Test
        void shouldThrowUserNotFoundExceptionOnGetProfile() {
            // FIX: UserService.getCustomerByUserId throws directly — no Optional.empty()
            when(customerService.getCustomerByUserId(userId))
                    .thenThrow(new UserNotFoundException("User not found"));

            assertThrows(UserNotFoundException.class,
                    () -> service.getProfile(userId));
        }

        @Test
        void shouldThrowProfileNotCreatedExceptionOnGetProfile() {
            // FIX: UserService throws ProfileNotCreatedException when customer is null.
            // The old 'user.setCustomer(null)' setup is no longer needed — UserService
            // handles that guard internally.
            when(customerService.getCustomerByUserId(userId))
                    .thenThrow(new ProfileNotCreatedException("Profile not created"));

            assertThrows(ProfileNotCreatedException.class,
                    () -> service.getProfile(userId));
        }
    }

    // =========================================================================
    // updateProfile
    // =========================================================================

    @Nested
    @DisplayName("Update Profile")
    class UpdateProfileTests {

        @Test
        void shouldUpdateProfileSuccessfully() {
            CustomerProfileUpdateRequest request = new CustomerProfileUpdateRequest();
            request.setFirstName("Amit");
            request.setLastName("Sharma");
            request.setCitizenshipCountry("India");

            // FIX: userRepository.findById() → userService.getCustomerByUserId()
            when(customerService.getCustomerByUserId(userId)).thenReturn(customer);
            when(customerRepository.save(any(Customer.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            String response = service.updateProfile(userId, request);

            assertEquals("Profile updated", response);

            // @Spy mapper mutates the same customer object — field assertions work directly
            assertEquals("Amit", customer.getFirstName());
            assertEquals("Sharma", customer.getLastName());
            assertEquals("India", customer.getCitizenshipCountry());

            verify(customerService).getCustomerByUserId(userId);
            verify(customerRepository).save(customer);
        }

        @Test
        void shouldUpdateDobAndResidencyStatus() {
            CustomerProfileUpdateRequest request = new CustomerProfileUpdateRequest();
            request.setDateOfBirth(LocalDate.of(1990, 1, 1));
            request.setResidencyStatus("NON_RESIDENT");

            when(customerService.getCustomerByUserId(userId)).thenReturn(customer);
            when(customerRepository.save(any())).thenReturn(customer);

            String response = service.updateProfile(userId, request);

            assertEquals("Profile updated", response);
            assertEquals(LocalDate.of(1990, 1, 1), customer.getDateOfBirth());
            assertEquals("NON_RESIDENT", customer.getResidencyStatus());

            verify(customerRepository).save(customer);
        }

        @Test
        void shouldThrowBadRequestExceptionWhenUpdateRequestIsEmpty() {
            // FIX: refactored impl throws BadRequestException, NOT IllegalArgumentException.
            // validateAtLeastOneFieldProvided() fires BEFORE userService is called,
            // so userService must never be touched.
            CustomerProfileUpdateRequest request = new CustomerProfileUpdateRequest();

            BadRequestException exception = assertThrows(
                    BadRequestException.class,
                    () -> service.updateProfile(userId, request)
            );

            assertEquals("At least one field must be provided for update", exception.getMessage());

            // FIX: userRepository → userService; both must never be called
            verify(customerService, never()).getCustomerByUserId(any());
            verify(customerRepository, never()).save(any());
        }

        @Test
        void shouldThrowUserNotFoundWhenUpdating() {
            CustomerProfileUpdateRequest request = new CustomerProfileUpdateRequest();
            request.setFirstName("Amit");

            // FIX: validation passes (firstName provided), then userService throws
            when(customerService.getCustomerByUserId(userId))
                    .thenThrow(new UserNotFoundException("User not found"));

            assertThrows(UserNotFoundException.class,
                    () -> service.updateProfile(userId, request));

            verify(customerRepository, never()).save(any());
        }

        @Test
        void shouldThrowProfileNotCreatedWhenUpdating() {
            CustomerProfileUpdateRequest request = new CustomerProfileUpdateRequest();
            request.setFirstName("Amit");

            // FIX: UserService throws ProfileNotCreatedException when customer is null.
            // 'user.setCustomer(null)' setup no longer needed.
            when(customerService.getCustomerByUserId(userId))
                    .thenThrow(new ProfileNotCreatedException("Profile not created"));

            assertThrows(ProfileNotCreatedException.class,
                    () -> service.updateProfile(userId, request));

            verify(customerRepository, never()).save(any());
        }
    }
}