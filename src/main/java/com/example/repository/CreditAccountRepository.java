package com.example.repository;

import com.example.entity.CreditAccount;
import com.example.enums.AccountStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CreditAccountRepository extends JpaRepository<CreditAccount, UUID> {

    // Customer's own accounts
    List<CreditAccount> findAllByCustomerCustomerId(UUID customerId);

    // Check if account exists for application (prevent duplicate account creation)
    boolean existsByApplicationApplicationId(UUID applicationId);

    // Check active account for a product — used by ActiveAccountCheker
    boolean existsByCustomerCustomerIdAndCreditProductCreditProductIdAndAccountStatus(
            UUID customerId,
            Long creditProductId,
            AccountStatus status
    );

    // Find by account number
    Optional<CreditAccount> findByAccountNumber(String accountNumber);

    // Admin — filter by status
    List<CreditAccount> findAllByAccountStatus(AccountStatus status);
}