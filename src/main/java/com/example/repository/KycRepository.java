package com.example.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.entity.KycRecord;
import com.example.enums.KycStatus;

public interface KycRepository extends JpaRepository<KycRecord, UUID> {
	
	Optional<KycRecord> findByCustomerCustomerIdAndIsActiveTrue(UUID customerId);

    List<KycRecord> findByStatus(KycStatus status);

    int countByCustomerCustomerId(UUID customerId);

    List<KycRecord> findByCustomerCustomerId(UUID customerId);

	boolean existsByCustomerCustomerIdAndStatus(UUID customerId, KycStatus verified);
	 

}
