package com.example.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import com.example.dto.request.CustomerProfileUpdateRequest;
import com.example.dto.response.CustomerProfileResponse;
import com.example.entity.Customer;
import com.example.repository.CustomerRepository;
import com.example.service.CustomerProfileService;
import com.example.testutil.TestFixtures;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CustomerProfileIntegrationTest {

    @Autowired
    private CustomerProfileService service;

    @Autowired
    private CustomerRepository customerRepository;

    // ================= GET PROFILE =================

    @Test
    void shouldGetProfileSuccessfully() {
        // GIVEN
        Customer customer = TestFixtures.validCustomerWithUser();
        customerRepository.save(customer);

        UUID userId = customer.getUser().getUserId(); // adjust if needed

        // WHEN
        CustomerProfileResponse response = service.getProfile(userId);

        // THEN
        assertThat(response).isNotNull();
        assertThat(response.getFirstName()).isEqualTo(customer.getFirstName());
    }

    // ================= UPDATE PROFILE =================

    @Test
    void shouldUpdateProfileSuccessfully() {
        // GIVEN
    	Customer customer = TestFixtures.validCustomerWithUser();
        customerRepository.save(customer);

        UUID userId = customer.getUser().getUserId(); // adjust if needed

        CustomerProfileUpdateRequest request = new CustomerProfileUpdateRequest();
        request.setFirstName("Amit");
        request.setLastName("Sharma");

        // WHEN
        String result = service.updateProfile(userId, request);

        // THEN
        assertThat(result).isEqualTo("Profile Updated Successfully");

        Customer updated = customerRepository.findById(customer.getCustomerId()).orElseThrow();

        assertThat(updated.getFirstName()).isEqualTo("Amit");
        assertThat(updated.getLastName()).isEqualTo("Sharma");
    }
    @Test
    void shouldThrowException_whenProfileNotFound() {
        UUID randomId = UUID.randomUUID();

        assertThatThrownBy(() -> service.getProfile(randomId))
                .isInstanceOf(Exception.class);
    }
    @Test
    void shouldThrowException_whenUpdateRequestEmpty() {
        Customer customer = TestFixtures.validCustomerWithUser();
        customerRepository.save(customer);

        CustomerProfileUpdateRequest request = new CustomerProfileUpdateRequest();

        assertThatThrownBy(() -> service.updateProfile(customer.getUser().getUserId(), request))
                .isInstanceOf(Exception.class);
    }
}
