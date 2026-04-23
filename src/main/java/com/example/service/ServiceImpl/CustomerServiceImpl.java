package com.example.service.ServiceImpl;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.entity.Customer;
import com.example.exception.ResourceNotFoundException;
import com.example.repository.CustomerRepository;
import com.example.service.CustomerService;

import lombok.RequiredArgsConstructor;

/**
 * Implementation of {@link CustomerService} responsible for managing
 * customer profile data.
 *
 * <p><b>Responsibilities:</b></p>
 * <ul>
 *     <li>Retrieve customer information</li>
 *     <li>Validate existence of customer records</li>
 *     <li>Persist customer data</li>
 * </ul>
 *
 * <p><b>Notes:</b></p>
 * <ul>
 *     <li>This service acts as a thin abstraction over {@link CustomerRepository}</li>
 *     <li>Business logic related to onboarding or validation should reside in higher layers</li>
 *     <li>Timezone is currently defaulted during persistence</li>
 * </ul>
 */
@Service
@Transactional
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;

    /**
     * Retrieves a customer by their unique identifier.
     *
     * @param customerId customer ID
     * @return {@link Customer}
     * @throws ResourceNotFoundException if customer does not exist
     */
    @Override
    @Transactional(readOnly = true)
    public Customer getCustomer(UUID customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer with id " + customerId + " not found"));
    }

    /**
     * Checks if a PAN number already exists in the system.
     *
     * @param panNumber PAN number
     * @return true if exists, false otherwise
     */
    @Override
    @Transactional(readOnly = true)
    public boolean panNumberExists(String panNumber) {
        return customerRepository.existsByPanNumber(panNumber);
    }

    /**
     * Persists a new or existing customer.
     *
     * <p>Currently assigns a default timezone of <b>Asia/Kolkata</b>.</p>
     *
     * @param customer customer entity
     * @return saved {@link Customer}
     */
    @Override
    public Customer saveCustomer(Customer customer) {
    	customer.setTimezone("Asia/Kolkata"); //For now default timezone is Asia/Kolkata
        return customerRepository.save(customer);
    }

    /**
     * Checks whether a customer exists by ID.
     *
     * @param customerId customer ID
     * @return true if exists, false otherwise
     */
    @Override
    @Transactional(readOnly = true)
    public boolean customerExists(UUID customerId) {
        return customerRepository.existsById(customerId);
    }
    /**
     * Retrieves customer associated with a given user ID.
     *
     * <p>Used when customer profile is mandatory.</p>
     *
     * @param userId user ID
     * @return {@link Customer}
     * @throws ResourceNotFoundException if no customer is linked to the user
     */
    @Override
    @Transactional(readOnly = true)
    public Customer getCustomerByUserId(UUID userId) {

        return customerRepository.findByUserUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer profile not found for user " + userId));
    }
    /**
     * Retrieves customer associated with a given user ID (optional).
     *
     * <p>Used when customer presence is not guaranteed.</p>
     *
     * @param userId user ID
     * @return {@link Optional} containing customer if present
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<Customer> findCustomerByUserId(UUID userId) {
        return customerRepository.findByUserUserId(userId);
    }

}
