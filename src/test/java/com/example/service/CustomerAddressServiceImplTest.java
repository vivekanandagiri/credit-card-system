package com.example.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.dto.request.AddressCreateRequest;
import com.example.dto.response.AddressResponse;
import com.example.entity.Customer;
import com.example.entity.CustomerAddress;
import com.example.exception.ResourceNotFoundException;
import com.example.mapper.CustomerAddressMapper;
import com.example.repository.CustomerAddressRepository;
import com.example.service.ServiceImpl.CustomerAddressServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link CustomerAddressServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
class CustomerAddressServiceImplTest {

    @Mock private CustomerAddressRepository addressRepository;
    @Mock private CustomerService customerService;
    @Mock private CustomerAddressMapper addressMapper;

    @InjectMocks
    private CustomerAddressServiceImpl service;

    private UUID customerId;
    private UUID addressId;
    private AddressCreateRequest request;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        addressId  = UUID.randomUUID();

        request = new AddressCreateRequest(
                "HOME",
                "123 MG Road",
                "Near Metro",
                "Bangalore",
                "Karnataka",
                "560001",
                "India",
                true
        );
    }

    // =========================================================================
    // addAddress
    // =========================================================================

    @Nested
    @DisplayName("Add Address")
    class AddAddressTests {

        @Test
        void add_address_success() {
            Customer customer = new Customer();
            customer.setCustomerId(customerId);

            CustomerAddress address = new CustomerAddress();

            when(customerService.getCustomer(customerId)).thenReturn(customer);
            when(addressMapper.toEntity(request, customer)).thenReturn(address);

            String result = service.addAddress(customerId, request);

            assertEquals("Address created", result);
            verify(addressRepository).save(address);
        }

        @Test
        void add_address_customer_not_found() {
            when(customerService.getCustomer(customerId))
                    .thenThrow(new ResourceNotFoundException("Customer not found"));

            assertThrows(ResourceNotFoundException.class,
                    () -> service.addAddress(customerId, request));

            verify(addressRepository, never()).save(any());
        }
    }

    // =========================================================================
    // getAddresses
    // =========================================================================

    @Nested
    @DisplayName("Get Addresses")
    class GetAddressesTests {

        @Test
        void get_addresses_success() {
            CustomerAddress address = new CustomerAddress();
            address.setAddressId(addressId);
            address.setLine1("123 MG Road");
            address.setCity("Bangalore");
            address.setState("Karnataka");
            address.setPostalCode("560001");
            address.setCountry("India");

            AddressResponse addressResponse = mock(AddressResponse.class);

            when(customerService.customerExists(customerId)).thenReturn(true);
            when(addressRepository.findByCustomerCustomerId(customerId))
                    .thenReturn(List.of(address));
            when(addressMapper.toResponse(address)).thenReturn(addressResponse);

            List<AddressResponse> result = service.getAddresses(customerId);

            assertThat(result).hasSize(1);
        }

        @Test
        void get_addresses_customer_not_found() {
            when(customerService.customerExists(customerId)).thenReturn(false);

            assertThrows(ResourceNotFoundException.class,
                    () -> service.getAddresses(customerId));

            verify(customerService).customerExists(customerId);
            verify(addressRepository, never()).findByCustomerCustomerId(any());
        }

        @Test
        void get_addresses_empty_list() {
            when(customerService.customerExists(customerId)).thenReturn(true);
            when(addressRepository.findByCustomerCustomerId(customerId))
                    .thenReturn(List.of());

            List<AddressResponse> result = service.getAddresses(customerId);

            assertThat(result).hasSize(1);
        }
    }

    // =========================================================================
    // deleteAddress
    // =========================================================================

    @Nested
    @DisplayName("Delete Address")
    class DeleteAddressTests {

        @Test
        void delete_address_success() {
            CustomerAddress address = new CustomerAddress();
            address.setAddressId(addressId);

            when(addressRepository.findById(addressId))
                    .thenReturn(Optional.of(address));

            String result = service.deleteAddress(customerId, addressId);

            assertEquals("Address Deleted", result);
            verify(addressRepository).delete(address);
        }
    }
}