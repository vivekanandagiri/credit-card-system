package com.example.repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.entity.Authorization;

public interface AuthorizationRepository extends JpaRepository<Authorization, UUID> {

	Optional<Authorization> findByNetworkReference(String networkReference);
    @Query("""
        SELECT COALESCE(SUM(a.amount),0)
        FROM Authorization a
        WHERE a.accountId = :accountId
        AND a.status = 'AUTHORIZED'
    """)
    BigDecimal sumActiveHolds(UUID accountId);
}
