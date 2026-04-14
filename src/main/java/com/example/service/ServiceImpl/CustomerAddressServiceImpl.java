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

import lombok.RequiredArgsConstructor;

/**
 * Implementation of {@link CustomerAddressService}.
 * Handles address persistence and cross-domain validation with the Customer service.
 */

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerAddressServiceImpl implements CustomerAddressService {

    private final CustomerAddressRepository addressRepository; 
    
    // Good Architecture: Injecting the domain service rather than the repository directly.
    private final CustomerService customerService;             
    private final CustomerAddressMapper addressMapper;

    @Override
    @Transactional // Override for write operations
    public String addAddress(UUID userId, AddressCreateRequest request) {

        Customer customer = customerService.getCustomerByUserId(userId);

        CustomerAddress address = addressMapper.toEntity(request, customer);
        addressRepository.save(address);

        return "Address created";
    }

    @Override
    @Transactional // Override for write operations
    public String deleteAddress(UUID userId, UUID addressId) {

        Customer customer = customerService.getCustomerByUserId(userId);

        CustomerAddress address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        if (!address.getCustomer().getCustomerId().equals(customer.getCustomerId())) {
            throw new AccessDeniedException("You do not have permission to delete this address");
        }

        addressRepository.delete(address);

        return "Address deleted";
    }

	@Override
	 public List<AddressResponse> getAddresses(UUID userId) {

        Customer customer = customerService.getCustomerByUserId(userId);

        return addressRepository.findByCustomerCustomerId(customer.getCustomerId())
                .stream()
                .map(addressMapper::toResponse)
                .collect(Collectors.toList());
    }

	@Override
	public boolean hasAddress(UUID customerId) {
	    return addressRepository.existsByCustomerCustomerId(customerId);
	}
}