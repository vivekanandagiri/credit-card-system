package com.example.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.entity.PaymentAllocation;

public interface PaymentAllocationRepository
extends JpaRepository<PaymentAllocation, UUID> {

List<PaymentAllocation> findByPayment_PaymentId(UUID paymentId);

@Query("""
	    SELECT COALESCE(SUM(pa.allocatedAmount), 0)
	    FROM PaymentAllocation pa
	    WHERE pa.statement.statementId = :statementId
	      AND pa.payment.paidAt <= :cutoff
	""")
	BigDecimal sumAllocatedToStatementBefore(
	    UUID statementId,
	    Instant cutoff
	);



}
