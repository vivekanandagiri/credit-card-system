package com.example.service.ServiceImpl;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.dto.request.AddressCreateRequest;
import com.example.dto.response.AddressResponse;
import com.example.dto.response.ApiResponse;

import com.example.entity.Customer;
import com.example.entity.CustomerAddress;

import com.example.exception.ResourceNotFoundException;
import com.example.mapper.CustomerAddressMapper;
import com.example.repository.CustomerAddressRepository;
import com.example.repository.CustomerRepository;

import com.example.service.CustomerAddressService;

@Service
@Transactional
public class CustomerAddressServiceImpl implements CustomerAddressService {

    private final CustomerRepository customerRepository;
    private final CustomerAddressRepository addressRepository;
    private final CustomerAddressMapper addressMapper;

    public CustomerAddressServiceImpl(
            CustomerRepository customerRepository,
            CustomerAddressRepository addressRepository, CustomerAddressMapper addressMapper) {

        this.customerRepository = customerRepository;
        this.addressRepository = addressRepository;
		this.addressMapper = addressMapper;
    }


    // ADD ADDRESS

    @Override
    public ApiResponse<String> addAddress(
            UUID customerId,
            AddressCreateRequest request) {

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Customer not found"));

        CustomerAddress address = addressMapper.toEntity(request, customer);

        addressRepository.save(address);

        return new ApiResponse<>(
                Instant.now(),
                HttpStatus.CREATED.value(),
                "Address added successfully",
                "Address created"
        );
    }


    // GET ADDRESSES

    @Override
    public ApiResponse<List<AddressResponse>> getAddresses(UUID customerId) {
    	
    	if (!customerRepository.existsById(customerId)) {
    	    throw new ResourceNotFoundException("Customer not found");
    	}

    	List<AddressResponse> addresses =
    	        addressRepository.findByCustomerCustomerId(customerId)
    	                .stream()
    	                .map(addressMapper::toResponse)
    	                .collect(Collectors.toList());

        return new ApiResponse<>(
                Instant.now(),
                HttpStatus.OK.value(),
                "Addresses fetched successfully",
                addresses
        );
    }


    // DELETE ADDRESS

    @Override
    public ApiResponse<String> deleteAddress(UUID addressId) {

        CustomerAddress address = addressRepository.findById(addressId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Address not found"));

        addressRepository.delete(address);

        return new ApiResponse<>(
                Instant.now(),
                HttpStatus.OK.value(),
                "Address deleted successfully",
                "Deleted"
        );
    }
}