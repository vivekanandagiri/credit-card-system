package com.example.repository;


import com.example.entity.CustomerAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import java.util.UUID;

public interface CustomerAddressRepository
        extends JpaRepository<CustomerAddress, UUID> {

	List<CustomerAddress> findByCustomerCustomerId(UUID customerId);
	
}