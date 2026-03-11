package com.example.service;

import com.example.dto.request.CustomerProfileUpdateRequest;
import com.example.dto.response.ApiResponse;
import com.example.dto.response.CustomerProfileResponse;
import com.example.entity.Customer;
import com.example.entity.User;
import com.example.exception.ProfileNotCreatedException;
import com.example.exception.UserNotFoundException;
import com.example.mapper.CustomerProfileMapper;
import com.example.repository.CustomerRepository;
import com.example.repository.UserRepository;
import com.example.service.ServiceImpl.CustomerProfileServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerProfileServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CustomerRepository customerRepository;
    
    @Spy
    private CustomerProfileMapper profileMapper = new CustomerProfileMapper();

    @InjectMocks
    private CustomerProfileServiceImpl service;

    private UUID userId;
    private User user;
    private Customer customer;

    @BeforeEach
    void setUp() {

        userId = UUID.randomUUID();

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

        user = new User();
        user.setUserId(userId);
        user.setCustomer(customer);
    }


    // GET PROFILE SUCCESS
    @Test
    void shouldGetProfileSuccessfully() {

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        ApiResponse<CustomerProfileResponse> response = service.getProfile(userId);

        assertNotNull(response);
        assertEquals("Profile fetched successfully", response.getMessage());
        assertEquals("John", response.getData().getFirstName());

        verify(userRepository).findById(userId);
    }

    // USER NOT FOUND
    @Test
    void shouldThrowUserNotFoundException() {

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> service.getProfile(userId));
    }


    // PROFILE NOT CREATED
    @Test
    void shouldThrowProfileNotCreatedException() {

        user.setCustomer(null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThrows(ProfileNotCreatedException.class,
                () -> service.getProfile(userId));
    }


    // UPDATE PROFILE SUCCESS
    @Test
    void shouldUpdateProfileSuccessfully() {

        customer.setFirstName("Aman");

        CustomerProfileUpdateRequest request = new CustomerProfileUpdateRequest();
        request.setFirstName("Amit");
        request.setLastName("Sharma");
        request.setCitizenshipCountry("India");

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(customerRepository.save(any(Customer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ApiResponse<String> response =
                service.updateProfile(userId, request);

        assertNotNull(response);
        assertEquals("Customer profile updated successfully", response.getMessage());

        // verify update happened
        assertEquals("Amit", customer.getFirstName());
        assertEquals("Sharma", customer.getLastName());
        assertEquals("India", customer.getCitizenshipCountry());

        verify(userRepository).findById(userId);
        verify(customerRepository).save(customer);
    }

    // UPDATE PROFILE USER NOT FOUND
    @Test
    void shouldThrowUserNotFoundWhenUpdating() {

        CustomerProfileUpdateRequest request = new CustomerProfileUpdateRequest();
        request.setFirstName("Amit"); 

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> service.updateProfile(userId, request));
    }

    // UPDATE PROFILE NOT CREATED

    @Test
    void shouldThrowProfileNotCreatedWhenUpdating() {

        CustomerProfileUpdateRequest request = new CustomerProfileUpdateRequest();
        request.setFirstName("Amit"); 

        user.setCustomer(null);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        assertThrows(ProfileNotCreatedException.class,
                () -> service.updateProfile(userId, request));
    }
    
    @Test
    void shouldThrowExceptionWhenUpdateRequestIsEmpty() {

        CustomerProfileUpdateRequest request = new CustomerProfileUpdateRequest();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.updateProfile(userId, request)
        );

        assertEquals("At least one field must be provided for update", exception.getMessage());

        // Ensure DB was never called
        verify(userRepository, never()).findById(any());
        verify(customerRepository, never()).save(any());
    }
    
    @Test
    void shouldUpdateDobAndResidencyStatus() {

        CustomerProfileUpdateRequest request = new CustomerProfileUpdateRequest();
        request.setDateOfBirth(LocalDate.of(1990,1,1));
        request.setResidencyStatus("RESIDENT");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(customerRepository.save(any())).thenReturn(customer);

        ApiResponse<String> response = service.updateProfile(userId, request);

        assertEquals("Customer profile updated successfully", response.getMessage());

        verify(customerRepository).save(customer);
    }
}