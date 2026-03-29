package com.example.service;

import com.example.entity.Customer;

import java.util.Optional;
import java.util.UUID;

/**
 * Internal service interface for customer entity operations.
 *
 * <p>Other services must go through this interface rather than injecting
 * {@code CustomerRepository} directly.
 */
public interface CustomerService {

    /**
     * Returns the {@link Customer} entity for the given customer ID.
     *
     * @throws com.example.exception.ResourceNotFoundException if the customer does not exist
     */
    Customer getCustomer(UUID customerId);

    /**
     * Returns {@code true} if a customer with the given PAN number already exists.
     */
    boolean panNumberExists(String panNumber);

    /**
     * Persists a new {@link Customer} entity and returns the saved instance.
     */
    Customer saveCustomer(Customer customer);

    /**
     * Returns {@code true} if a customer with the given ID exists.
     * Used by {@link com.example.service.CustomerAddressService} to validate the customer
     * before performing address operations, without exposing the repository directly.
     */
    boolean customerExists(UUID customerId);
    

    Customer getCustomerByUserId(UUID userId);  
    // strict
    Optional<Customer> findCustomerByUserId(UUID userId); // safe
}