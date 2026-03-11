//package com.example.service.ServiceImpl;
//
//import com.example.enums.CardStatus;
//import com.example.repository.IssuedCardRepository;
//import com.example.service.ActiveCardChecker;
//import org.springframework.stereotype.Component;
//
//import java.util.UUID;
//
///**
// * Real implementation of ActiveCardChecker.
// *
// * Queries the issued_cards table to check whether the customer
// * already holds an ACTIVE card for the given product.
// *
// * HOW TO ACTIVATE:
// * 1. Build the card issuance module (issued_cards table + IssuedCardRepository)
// * 2. Remove @Primary from NoOpActiveCardChecker
// * 3. Add @Primary to this class
// * 4. Uncomment this class (remove the block comment below)
// *
// * IssuedCardRepository must have this method:
// *
// *   boolean existsByCustomerCustomerIdAndCardProductCardProductIdAndCardStatus(
// *       UUID customerId,
// *       UUID cardProductId,
// *       CardStatus status
// *   );
// */
//
//// TODO: Uncomment this entire class once card issuance module is built
///*
//@Component
//public class IssuedCardActiveCardChecker implements ActiveCardChecker {
//
//    private final IssuedCardRepository issuedCardRepository;
//
//    public IssuedCardActiveCardChecker(IssuedCardRepository issuedCardRepository) {
//        this.issuedCardRepository = issuedCardRepository;
//    }
//
//    @Override
//    public boolean hasActiveCard(UUID customerId, UUID cardProductId) {
//        return issuedCardRepository
//                .existsByCustomerCustomerIdAndCardProductCardProductIdAndCardStatus(
//                        customerId,
//                        cardProductId,
//                        CardStatus.ACTIVE
//                );
//    }
//}
//*/