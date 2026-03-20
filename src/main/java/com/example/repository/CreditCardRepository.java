package com.example.repository;


import com.example.entity.CreditCard;
import com.example.enums.CardStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CreditCardRepository extends JpaRepository<CreditCard, UUID> {

    List<CreditCard> findAllByCustomerCustomerId(UUID customerId);

    List<CreditCard> findAllByCreditAccountAccountId(UUID accountId);

    List<CreditCard> findAllByCardStatus(CardStatus cardStatus);

    // max cards per account 
    int countByCreditAccountAccountIdAndCardStatusIn(UUID accountId, List<CardStatus> statuses);

    // check if a VIRTUAL card already exists on this account
    // (one virtual card per account at a time)
    boolean existsByCreditAccountAccountIdAndCardFormatAndCardStatusIn(
            UUID accountId,
            com.example.enums.CardFormat cardFormat,
            List<CardStatus> statuses
    );

    List<CreditCard> findAllByCustomerCustomerIdAndCardStatus(
            UUID customerId,
            CardStatus cardStatus
    );
}