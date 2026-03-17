package com.example.repository;

import com.example.entity.CreditCardApplication;
import com.example.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface CreditCardApplicationRepository
        extends JpaRepository<CreditCardApplication, UUID> {

    List<CreditCardApplication> findAllByCustomerCustomerId(UUID customerId);

    List<CreditCardApplication> findAllByApplicationStatus(ApplicationStatus status);

    //active application for same customer + same credit product
    boolean existsByCustomerCustomerIdAndCreditProductCreditProductIdAndApplicationStatusIn(
            UUID customerId,
            Long creditProductId,
            List<ApplicationStatus> statuses
    );
    
    
    // Rejection cooldown — recent rejection for same customer + same credit product
    boolean existsByCustomerCustomerIdAndCreditProductCreditProductIdAndApplicationStatusAndSubmittedAtAfter(
            UUID customerId,
            Long creditProductId,
            ApplicationStatus status,
            Instant after
    );


    // Max active applications limit across all products
    int countByCustomerCustomerIdAndApplicationStatusIn(
            UUID customerId,
            List<ApplicationStatus> statuses
    );
}
