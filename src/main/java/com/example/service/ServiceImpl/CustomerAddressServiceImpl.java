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
 * Implementation of {@link CustomerAddressService} responsible for managing
 * customer address data.
 *
 * <p><b>Responsibilities:</b></p>
 * <ul>
 *     <li>Create and delete customer addresses</li>
 *     <li>Fetch addresses for a given user</li>
 *     <li>Enforce ownership validation (security)</li>
 * </ul>
 *
 * <p><b>Design Notes:</b></p>
 * <ul>
 *     <li>Uses {@link CustomerService} instead of directly accessing repository (good layering)</li>
 *     <li>Ensures that address operations are scoped to the authenticated user</li>
 *     <li>All write operations are explicitly transactional</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerAddressServiceImpl implements CustomerAddressService {

    private final CustomerAddressRepository addressRepository; 
    //Injecting the domain service rather than the repository directly.
    private final CustomerService customerService;             
    private final CustomerAddressMapper addressMapper;

    /**
     * Adds a new address for the given user.
     *
     * <p>Steps:</p>
     * <ol>
     *     <li>Resolve customer from user ID</li>
     *     <li>Map request to {@link CustomerAddress}</li>
     *     <li>Persist address</li>
     * </ol>
     *
     * @param userId  user identifier
     * @param request address creation payload
     * @return confirmation message
     * @throws ResourceNotFoundException if customer not found
     */
    @Override
    @Transactional // Override for write operations
    public String addAddress(UUID userId, AddressCreateRequest request) {

        Customer customer = customerService.getCustomerByUserId(userId);

        CustomerAddress address = addressMapper.toEntity(request, customer);
        addressRepository.save(address);

        return "Address created";
    }

    /**
     * Deletes an address belonging to the given user.
     *
     * <p>Security enforcement:</p>
     * <ul>
     *     <li>User must own the address</li>
     * </ul>
     *
     * @param userId    user identifier
     * @param addressId address identifier
     * @return confirmation message
     * @throws ResourceNotFoundException if address not found
     * @throws AccessDeniedException if user does not own the address
     */
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

    /**
     * Retrieves all addresses for a given user.
     *
     * @param userId user identifier
     * @return list of {@link AddressResponse}
     * @throws ResourceNotFoundException if customer not found
     */
	@Override
	 public List<AddressResponse> getAddresses(UUID userId) {

        Customer customer = customerService.getCustomerByUserId(userId);

        return addressRepository.findByCustomerCustomerId(customer.getCustomerId())
                .stream()
                .map(addressMapper::toResponse)
                .collect(Collectors.toList());
    }
	/**
     * Checks whether a customer has at least one address.
     *
     * @param customerId customer ID
     * @return true if at least one address exists
     */
	@Override
	public boolean hasAddress(UUID customerId) {
	    return addressRepository.existsByCustomerCustomerId(customerId);
	}
}