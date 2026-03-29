package com.example.repository;

import com.example.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    boolean existsByPanNumber(String panNumber);

	Optional<Customer> findByUserUserId(UUID userId);
}