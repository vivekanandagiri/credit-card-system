package com.example.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import com.example.dto.request.AddressCreateRequest;
import com.example.dto.response.AddressResponse;
import com.example.entity.Customer;
import com.example.entity.CustomerAddress;
import com.example.repository.CustomerAddressRepository;
import com.example.repository.CustomerRepository;
import com.example.service.CustomerAddressService;
import com.example.testutil.TestFixtures;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CustomerAddressIntegrationTest {

    @Autowired
    private CustomerAddressService service;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerAddressRepository addressRepository;

    // ================= ADD ADDRESS =================

    @Test
    void shouldAddAddressSuccessfully() {
        // GIVEN
        Customer customer = TestFixtures.validCustomerWithUser();

        customerRepository.save(customer);   // ✅ SAVE FIRST

        UUID userId = customer.getUser().getUserId();  // ✅ NOW ID EXISTS

        AddressCreateRequest request = TestFixtures.validAddressRequest();

        // WHEN
        String result = service.addAddress(userId, request);

        // THEN
        assertThat(result).isEqualTo("Address created");
    }

    // ================= GET ADDRESSES =================

    @Test
    void shouldGetAddressesSuccessfully() {
        // GIVEN
        Customer customer = TestFixtures.validCustomerWithUser();
        customerRepository.save(customer);

        UUID userId = customer.getUser().getUserId();

        AddressCreateRequest request = TestFixtures.validAddressRequest();
        service.addAddress(userId, request);

        // WHEN
        List<AddressResponse> result = service.getAddresses(userId);

        // THEN
        assertThat(result).isNotEmpty();
        assertThat(result.get(0).getCity()).isEqualTo("Bangalore");
    }

    // ================= DELETE ADDRESS =================

    @Test
    void shouldDeleteAddressSuccessfully() {
        // GIVEN
        Customer customer = TestFixtures.validCustomerWithUser();
        customerRepository.save(customer);

        UUID userId = customer.getUser().getUserId();

        AddressCreateRequest request = TestFixtures.validAddressRequest();
        service.addAddress(userId, request);

        CustomerAddress address = addressRepository
                .findByCustomerCustomerId(customer.getCustomerId())
                .get(0);

        // WHEN
        String result = service.deleteAddress(userId, address.getAddressId());

        // THEN
        assertThat(result).isEqualTo("Address deleted");

        List<CustomerAddress> remaining =
                addressRepository.findByCustomerCustomerId(customer.getCustomerId());

        assertThat(remaining).isEmpty();
    }

    // ================= ACCESS DENIED =================

    @Test
    void shouldThrowAccessDenied_whenDeletingOtherUsersAddress() {
        // GIVEN
        Customer customer1 = TestFixtures.validCustomerWithUser();
        Customer customer2 = TestFixtures.validCustomerWithUser();

        customerRepository.save(customer1);
        customerRepository.save(customer2);

        UUID user1 = customer1.getUser().getUserId();
        UUID user2 = customer2.getUser().getUserId();

        service.addAddress(user1, TestFixtures.validAddressRequest());

        CustomerAddress address = addressRepository
                .findByCustomerCustomerId(customer1.getCustomerId())
                .get(0);

        // WHEN + THEN
        assertThatThrownBy(() -> service.deleteAddress(user2, address.getAddressId()))
                .isInstanceOf(Exception.class);
    }
}