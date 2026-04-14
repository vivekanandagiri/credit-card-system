package com.example.repository;

import com.example.entity.Payment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {


	boolean existsByReferenceId(String referenceId);
	
    Page<Payment> findByAccount_AccountId(
            UUID accountId,
            Pageable pageable
    );

    @Query("""
        SELECT DISTINCT p
        FROM Payment p
        LEFT JOIN FETCH p.allocations a
        LEFT JOIN FETCH a.statement
        WHERE p.paymentId = :paymentId
          AND p.account.accountId = :accountId
    """)
    Optional<Payment> findDetailedByPaymentIdAndAccountId(
            UUID paymentId,
            UUID accountId
    );

    @Query("""
        SELECT DISTINCT p
        FROM Payment p
        LEFT JOIN FETCH p.allocations a
        LEFT JOIN FETCH a.statement
        WHERE p.paymentId IN :paymentIds
    """)
    List<Payment> findAllWithAllocationsByIds(
            List<UUID> paymentIds
    );

	Optional<Payment> findByReferenceId(String referenceId);
}