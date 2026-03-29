package com.example.service.ServiceImpl;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.dto.request.AddressCreateRequest;
import com.example.dto.response.AddressResponse;
import com.example.entity.Customer;
import com.example.entity.CustomerAddress;
import com.example.exception.ResourceNotFoundException;
import com.example.mapper.CustomerAddressMapper;
import com.example.repository.CustomerAddressRepository;
import com.example.service.CustomerAddressService;
import com.example.service.CustomerService;

/**
 * Implementation of {@link CustomerAddressService}.
 * Handles address persistence and cross-domain validation with the Customer service.
 */
// SENIOR OPTIMIZATION: Default to read-only transactions to improve GET performance.
@Service
@Transactional(readOnly = true)
public class CustomerAddressServiceImpl implements CustomerAddressService {

    private final CustomerAddressRepository addressRepository; 
    
    // Good Architecture: Injecting the domain service rather than the repository directly.
    private final CustomerService customerService;             
    private final CustomerAddressMapper addressMapper;
 
    public CustomerAddressServiceImpl(
            CustomerAddressRepository addressRepository,
            CustomerService customerService,
            CustomerAddressMapper addressMapper) {
        this.addressRepository = addressRepository;
        this.customerService = customerService;
        this.addressMapper = addressMapper;
    }

    @Override
    @Transactional // Override for write operations
    public String addAddress(UUID customerId, AddressCreateRequest request) {

        Customer customer = customerService.getCustomer(customerId);
        CustomerAddress address = addressMapper.toEntity(request, customer);
        
        addressRepository.save(address);

        return "Address created";
    }

    @Override
    public List<AddressResponse> getAddresses(UUID customerId) {
        
        // Fail-fast domain validation
        if (!customerService.customerExists(customerId)) {
            throw new ResourceNotFoundException("Customer not found");
        }

        return addressRepository.findByCustomerCustomerId(customerId)
                .stream()
                .map(addressMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional // Override for write operations
    public String deleteAddress(UUID customerId, UUID addressId) {

        CustomerAddress address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        // SECURITY ARCHITECTURE: Prevent IDOR attacks.
        // Ensure the address being deleted actually belongs to the user making the request.
        if (!address.getCustomer().getCustomerId().equals(customerId)) {
            throw new AccessDeniedException("You do not have permission to delete this address");
            // Alternatively, you can throw a ResourceNotFoundException here to completely 
            // mask the existence of the address from attackers.
        }

        addressRepository.delete(address);

        return "Address Deleted";
    }
}