package com.example.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.dto.request.AddressCreateRequest;
import com.example.entity.Customer;
import com.example.entity.CustomerAddress;
import com.example.exception.ResourceNotFoundException;
import com.example.mapper.CustomerAddressMapper;
import com.example.repository.CustomerAddressRepository;
import com.example.repository.CustomerRepository;
import com.example.service.ServiceImpl.CustomerAddressServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.*;

class CustomerAddressServiceImplTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerAddressRepository addressRepository;
    
    @Spy
    private CustomerAddressMapper addressMapper = new CustomerAddressMapper();

    @InjectMocks
    private CustomerAddressServiceImpl service;

    private UUID customerId;
    private UUID addressId;
    private AddressCreateRequest request;

    @BeforeEach
    void setup() {

        MockitoAnnotations.openMocks(this);

        customerId = UUID.randomUUID();
        addressId = UUID.randomUUID();

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

    // ADD ADDRESS TESTS
    @Nested
    @DisplayName("Add Address Service Tests")
    class AddAddressTests {

    	@Test
    	void add_address_success() {

    	    Customer customer = new Customer();
    	    customer.setCustomerId(customerId);

    	    CustomerAddress address = new CustomerAddress();

    	    when(customerRepository.findById(customerId))
    	            .thenReturn(Optional.of(customer));

    	    when(addressMapper.toEntity(request, customer))
    	            .thenReturn(address);

    	    var response = service.addAddress(customerId, request);

    	    assertEquals(201, response.getStatus());
    	    assertEquals("Address added successfully", response.getMessage());

    	    verify(addressRepository, times(1))
    	            .save(address);
    	}

        @Test
        void add_address_customer_not_found() {

            when(customerRepository.findById(customerId))
                    .thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> {
                service.addAddress(customerId, request);
            });
        }
    }

    // GET ADDRESSES TESTS
    @Nested
    @DisplayName("Get Addresses Service Tests")
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

    	    when(customerRepository.existsById(customerId)).thenReturn(true);

    	    when(addressRepository.findByCustomerCustomerId(customerId))
    	            .thenReturn(List.of(address));

    	    var response = service.getAddresses(customerId);

    	    assertEquals(200, response.getStatus());
    	    assertEquals("Addresses fetched successfully", response.getMessage());
    	    assertEquals(1, response.getData().size());
    	}
    	@Test
    	void get_addresses_customer_not_found() {

    	    when(customerRepository.existsById(customerId)).thenReturn(false);

    	    assertThrows(ResourceNotFoundException.class,
    	            () -> service.getAddresses(customerId));

    	    verify(customerRepository).existsById(customerId);
    	}
    	@Test
    	void get_addresses_empty_list() {

    	    when(customerRepository.existsById(customerId)).thenReturn(true);

    	    when(addressRepository.findByCustomerCustomerId(customerId))
    	            .thenReturn(List.of());

    	    var response = service.getAddresses(customerId);

    	    assertEquals(200, response.getStatus());
    	    assertTrue(response.getData().isEmpty());
    	}
    }

    // DELETE ADDRESS TESTS
    @Nested
    @DisplayName("Delete Address Service Tests")
    class DeleteAddressTests {

        @Test
        void delete_address_success() {

            CustomerAddress address = new CustomerAddress();
            address.setAddressId(addressId);

            when(addressRepository.findById(addressId))
                    .thenReturn(Optional.of(address));

            var response = service.deleteAddress(addressId);

            assertEquals(200, response.getStatus());
            assertEquals("Address deleted successfully", response.getMessage());

            verify(addressRepository, times(1)).delete(address);
        }

        @Test
        void delete_address_not_found() {

            when(addressRepository.findById(addressId))
                    .thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> service.deleteAddress(addressId));

            verify(addressRepository).findById(addressId);
        }
    }
}