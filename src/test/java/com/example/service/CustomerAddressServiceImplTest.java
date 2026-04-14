package com.example.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.testutil.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.example.dto.request.AddressCreateRequest;
import com.example.dto.response.AddressResponse;
import com.example.entity.Customer;
import com.example.entity.CustomerAddress;
import com.example.exception.ResourceNotFoundException;
import com.example.mapper.CustomerAddressMapper;
import com.example.repository.CustomerAddressRepository;
import com.example.service.ServiceImpl.CustomerAddressServiceImpl;

@ExtendWith(MockitoExtension.class)
class CustomerAddressServiceImplTest {

    @Mock private CustomerAddressRepository addressRepository;
    @Mock private CustomerService customerService;
    @Mock private CustomerAddressMapper addressMapper;

    @InjectMocks
    private CustomerAddressServiceImpl service;

    private UUID userId;
    private UUID customerId;
    private UUID addressId;
    private Customer customer;
    private AddressCreateRequest request;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        customer = TestFixtures.validCustomer();
        customerId = customer.getCustomerId();
        addressId = UUID.randomUUID();
        request = TestFixtures.validAddressRequest();
    }

    // ================= ADD ADDRESS =================

    @Nested
    @DisplayName("Add Address")
    class AddAddressTests {

        @Test
        void shouldAddAddress_whenValidRequest() {
            // GIVEN
            CustomerAddress address = new CustomerAddress();

            when(customerService.getCustomerByUserId(userId)).thenReturn(customer);
            when(addressMapper.toEntity(request, customer)).thenReturn(address);

            // WHEN
            String result = service.addAddress(userId, request);

            // THEN
            assertThat(result).isEqualTo("Address created");
            verify(addressRepository).save(address);
        }

        @Test
        void shouldThrowException_whenCustomerNotFound() {
            // GIVEN
            when(customerService.getCustomerByUserId(userId))
                    .thenThrow(new ResourceNotFoundException("Customer not found"));

            // WHEN + THEN
            assertThatThrownBy(() -> service.addAddress(userId, request))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(addressRepository, never()).save(any());
        }
    }

    // ================= GET ADDRESSES =================

    @Nested
    @DisplayName("Get Addresses")
    class GetAddressesTests {

        @Test
        void shouldReturnAddresses_whenPresent() {
            // GIVEN
            CustomerAddress address = new CustomerAddress();
            AddressResponse response = mock(AddressResponse.class);

            when(customerService.getCustomerByUserId(userId)).thenReturn(customer);
            when(addressRepository.findByCustomerCustomerId(customerId))
                    .thenReturn(List.of(address));
            when(addressMapper.toResponse(address)).thenReturn(response);

            // WHEN
            List<AddressResponse> result = service.getAddresses(userId);

            // THEN
            assertThat(result).hasSize(1);
        }

        @Test
        void shouldReturnEmptyList_whenNoAddresses() {
            // GIVEN
            when(customerService.getCustomerByUserId(userId)).thenReturn(customer);
            when(addressRepository.findByCustomerCustomerId(customerId))
                    .thenReturn(List.of());

            // WHEN
            List<AddressResponse> result = service.getAddresses(userId);

            // THEN
            assertThat(result).isEmpty();
        }

        @Test
        void shouldThrowException_whenCustomerNotFound() {
            // GIVEN
            when(customerService.getCustomerByUserId(userId))
                    .thenThrow(new ResourceNotFoundException("Customer not found"));

            // WHEN + THEN
            assertThatThrownBy(() -> service.getAddresses(userId))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(addressRepository, never()).findByCustomerCustomerId(any());
        }
    }

    // ================= DELETE ADDRESS =================

    @Nested
    @DisplayName("Delete Address")
    class DeleteAddressTests {

        @Test
        void shouldDeleteAddress_whenValidOwner() {
            // GIVEN
            CustomerAddress address = new CustomerAddress();
            address.setCustomer(customer);

            when(customerService.getCustomerByUserId(userId)).thenReturn(customer);
            when(addressRepository.findById(addressId))
                    .thenReturn(Optional.of(address));

            // WHEN
            String result = service.deleteAddress(userId, addressId);

            // THEN
            assertThat(result).isEqualTo("Address deleted");
            verify(addressRepository).delete(address);
        }

        @Test
        void shouldThrowException_whenAddressNotFound() {
            // GIVEN
            when(customerService.getCustomerByUserId(userId)).thenReturn(customer);
            when(addressRepository.findById(addressId))
                    .thenReturn(Optional.empty());

            // WHEN + THEN
            assertThatThrownBy(() -> service.deleteAddress(userId, addressId))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(addressRepository, never()).delete(any());
        }

        @Test
        void shouldThrowAccessDenied_whenAddressBelongsToAnotherUser() {
            // GIVEN
            Customer anotherCustomer = new Customer();
            anotherCustomer.setCustomerId(UUID.randomUUID());

            CustomerAddress address = new CustomerAddress();
            address.setCustomer(anotherCustomer);

            when(customerService.getCustomerByUserId(userId)).thenReturn(customer);
            when(addressRepository.findById(addressId))
                    .thenReturn(Optional.of(address));

            // WHEN + THEN
            assertThatThrownBy(() -> service.deleteAddress(userId, addressId))
                    .isInstanceOf(AccessDeniedException.class);

            verify(addressRepository, never()).delete(any());
        }
    }

    // ================= HAS ADDRESS =================

    @Test
    void shouldReturnTrue_whenCustomerHasAddress() {
        when(addressRepository.existsByCustomerCustomerId(customerId)).thenReturn(true);

        boolean result = service.hasAddress(customerId);

        assertThat(result).isTrue();
    }

    @Test
    void shouldReturnFalse_whenCustomerHasNoAddress() {
        when(addressRepository.existsByCustomerCustomerId(customerId)).thenReturn(false);

        boolean result = service.hasAddress(customerId);

        assertThat(result).isFalse();
    }
}