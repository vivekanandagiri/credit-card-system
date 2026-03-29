package com.example.service;

import com.example.entity.Customer;
import com.example.exception.ResourceNotFoundException;
import com.example.repository.CustomerRepository;
import com.example.service.ServiceImpl.CustomerServiceImpl;

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
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerServiceImpl customerService;

    private UUID customerId;
    private UUID userId;
    private Customer customer;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        userId = UUID.randomUUID();

        customer = new Customer();
        customer.setCustomerId(customerId);
    }

    @Nested
    @DisplayName("getCustomer")
    class GetCustomerTests {

        @Test
        void shouldReturnCustomer_whenFound() {
            when(customerRepository.findById(customerId))
                    .thenReturn(Optional.of(customer));

            Customer result = customerService.getCustomer(customerId);

            assertThat(result).isSameAs(customer);
            verify(customerRepository).findById(customerId);
        }

        @Test
        void shouldThrowException_whenNotFound() {
            when(customerRepository.findById(customerId))
                    .thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> customerService.getCustomer(customerId));

            verify(customerRepository).findById(customerId);
        }
    }

    @Nested
    @DisplayName("panNumberExists")
    class PanExistsTests {

        @Test
        void shouldReturnTrue_whenPanExists() {
            when(customerRepository.existsByPanNumber("ABC123"))
                    .thenReturn(true);

            boolean result = customerService.panNumberExists("ABC123");

            assertThat(result).isTrue();
            verify(customerRepository).existsByPanNumber("ABC123");
        }
    }

    @Nested
    @DisplayName("saveCustomer")
    class SaveCustomerTests {

        @Test
        void shouldSaveCustomer() {
            when(customerRepository.save(customer)).thenReturn(customer);

            Customer result = customerService.saveCustomer(customer);

            assertThat(result).isSameAs(customer);
            verify(customerRepository).save(customer);
        }
    }

    @Nested
    @DisplayName("customerExists")
    class CustomerExistsTests {

        @Test
        void shouldReturnTrue_whenCustomerExists() {
            when(customerRepository.existsById(customerId)).thenReturn(true);

            boolean result = customerService.customerExists(customerId);

            assertThat(result).isTrue();
            verify(customerRepository).existsById(customerId);
        }
    }

    @Nested
    @DisplayName("getCustomerByUserId")
    class GetCustomerByUserIdTests {

        @Test
        void shouldReturnCustomer_whenFound() {
            when(customerRepository.findByUserUserId(userId))
                    .thenReturn(Optional.of(customer));

            Customer result = customerService.getCustomerByUserId(userId);

            assertThat(result).isSameAs(customer);
            verify(customerRepository).findByUserUserId(userId);
        }

        @Test
        void shouldThrowException_whenNotFound() {
            when(customerRepository.findByUserUserId(userId))
                    .thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> customerService.getCustomerByUserId(userId));

            verify(customerRepository).findByUserUserId(userId);
        }
    }

    @Nested
    @DisplayName("findCustomerByUserId")
    class FindCustomerByUserIdTests {

        @Test
        void shouldReturnOptionalCustomer_whenFound() {
            when(customerRepository.findByUserUserId(userId))
                    .thenReturn(Optional.of(customer));

            Optional<Customer> result =
                    customerService.findCustomerByUserId(userId);

            assertThat(result).isPresent().contains(customer);
        }

        @Test
        void shouldReturnEmptyOptional_whenNotFound() {
            when(customerRepository.findByUserUserId(userId))
                    .thenReturn(Optional.empty());

            Optional<Customer> result =
                    customerService.findCustomerByUserId(userId);

            assertThat(result).isEmpty();
        }
    }
}