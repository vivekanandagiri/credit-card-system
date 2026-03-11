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

    boolean existsByCustomerCustomerIdAndCardProductCardProductIdAndApplicationStatusIn(
            UUID customerId, UUID cardProductId, List<ApplicationStatus> statuses);
    
    
    boolean findTopByCustomerCustomerIdAndCardProductCardProductIdAndApplicationStatusAndSubmittedAtAfter(
            UUID customerId,
            UUID cardProductId,
            ApplicationStatus status,
            Instant after
    );


    int countByCustomerCustomerIdAndApplicationStatusIn(
            UUID customerId,
            List<ApplicationStatus> statuses
    );
}
