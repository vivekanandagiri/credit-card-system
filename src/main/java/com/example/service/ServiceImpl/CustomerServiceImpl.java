package com.example.service.ServiceImpl;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.entity.Customer;
import com.example.exception.ResourceNotFoundException;
import com.example.repository.CustomerRepository;
import com.example.service.CustomerService;

@Service
@Transactional
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    
    public CustomerServiceImpl(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }
	
    @Override
    @Transactional(readOnly = true)
    public Customer getCustomer(UUID customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer with id " + customerId + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean panNumberExists(String panNumber) {
        return customerRepository.existsByPanNumber(panNumber);
    }

    @Override
    public Customer saveCustomer(Customer customer) {
    	customer.setTimezone("Asia/Kolkata"); //For now default timezone is Asia/Kolkata
        return customerRepository.save(customer);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean customerExists(UUID customerId) {
        return customerRepository.existsById(customerId);
    }
    /**
     * For Customer 
     */
    @Override
    @Transactional(readOnly = true)
    public Customer getCustomerByUserId(UUID userId) {

        return customerRepository.findByUserUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Customer profile not found for user " + userId));
    }
    /**
     * For Customer and Other User 
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<Customer> findCustomerByUserId(UUID userId) {
        return customerRepository.findByUserUserId(userId);
    }

}
