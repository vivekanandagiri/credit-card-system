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
import com.example.testutil.TestFixtures;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CustomerProfileServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class CustomerProfileServiceImplTest {

    @Mock private CustomerService customerService;
    @Mock private CustomerRepository customerRepository;

    @Spy
    private CustomerProfileMapper profileMapper = new CustomerProfileMapper();

    @InjectMocks
    private CustomerProfileServiceImpl service;

    private UUID userId;
    private Customer customer;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        customer = TestFixtures.validCustomer();
    }

    // ================= GET PROFILE =================

    @Nested
    @DisplayName("Get Profile")
    class GetProfileTests {

        @Test
        void shouldReturnProfile_whenCustomerExists() {
            // GIVEN
            when(customerService.getCustomerByUserId(userId))
                    .thenReturn(customer);

            // WHEN
            CustomerProfileResponse response = service.getProfile(userId);

            // THEN
            assertThat(response)
                    .isNotNull()
                    .extracting(CustomerProfileResponse::getFirstName)
                    .isEqualTo("John");

            verify(customerService).getCustomerByUserId(userId);
        }

        @Test
        void shouldThrowUserNotFound_whenUserDoesNotExist() {
            // GIVEN
            when(customerService.getCustomerByUserId(userId))
                    .thenThrow(new UserNotFoundException("User not found"));

            // WHEN + THEN
            assertThatThrownBy(() -> service.getProfile(userId))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        void shouldThrowProfileNotCreated_whenCustomerIsNull() {
            // GIVEN
            when(customerService.getCustomerByUserId(userId))
                    .thenReturn(null);

            // WHEN + THEN
            assertThatThrownBy(() -> service.getProfile(userId))
                    .isInstanceOf(ProfileNotCreatedException.class);
        }
    }

    // ================= UPDATE PROFILE =================

    @Nested
    @DisplayName("Update Profile")
    class UpdateProfileTests {

        @Test
        void shouldUpdateProfileSuccessfully_whenValidRequest() {
            // GIVEN
            CustomerProfileUpdateRequest request = new CustomerProfileUpdateRequest();
            request.setFirstName("Amit");
            request.setLastName("Sharma");

            when(customerService.getCustomerByUserId(userId)).thenReturn(customer);
            when(customerRepository.save(any())).thenReturn(customer);

            // WHEN
            String response = service.updateProfile(userId, request);

            // THEN
            assertThat(response).isEqualTo("Profile Updated Successfully");
            assertThat(customer.getFirstName()).isEqualTo("Amit");
            assertThat(customer.getLastName()).isEqualTo("Sharma");

            verify(customerRepository).save(customer);
        }

        @Test
        void shouldUpdateDobAndResidency_whenProvided() {
            // GIVEN
            CustomerProfileUpdateRequest request = new CustomerProfileUpdateRequest();
            request.setDateOfBirth(LocalDate.of(1990, 1, 1));
            request.setResidencyStatus("NON_RESIDENT");

            when(customerService.getCustomerByUserId(userId)).thenReturn(customer);
            when(customerRepository.save(any())).thenReturn(customer);

            // WHEN
            service.updateProfile(userId, request);

            // THEN
            assertThat(customer.getDateOfBirth())
                    .isEqualTo(LocalDate.of(1990, 1, 1));
            assertThat(customer.getResidencyStatus())
                    .isEqualTo("NON_RESIDENT");
        }

        @Test
        void shouldThrowBadRequest_whenRequestIsEmpty() {
            // GIVEN
            CustomerProfileUpdateRequest request = new CustomerProfileUpdateRequest();

            // WHEN + THEN
            assertThatThrownBy(() -> service.updateProfile(userId, request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("At least one field must be provided for update");

            verifyNoInteractions(customerService);
            verifyNoInteractions(customerRepository);
        }

        @Test
        void shouldThrowUserNotFound_whenUpdatingNonExistingUser() {
            // GIVEN
            CustomerProfileUpdateRequest request = new CustomerProfileUpdateRequest();
            request.setFirstName("Amit");

            when(customerService.getCustomerByUserId(userId))
                    .thenThrow(new UserNotFoundException("User not found"));

            // WHEN + THEN
            assertThatThrownBy(() -> service.updateProfile(userId, request))
                    .isInstanceOf(UserNotFoundException.class);

            verify(customerRepository, never()).save(any());
        }

        @Test
        void shouldThrowProfileNotCreated_whenCustomerIsNull() {
            // GIVEN
            CustomerProfileUpdateRequest request = new CustomerProfileUpdateRequest();
            request.setFirstName("Amit");

            when(customerService.getCustomerByUserId(userId))
                    .thenReturn(null);

            // WHEN + THEN
            assertThatThrownBy(() -> service.updateProfile(userId, request))
                    .isInstanceOf(ProfileNotCreatedException.class);

            verify(customerRepository, never()).save(any());
        }
    }
}