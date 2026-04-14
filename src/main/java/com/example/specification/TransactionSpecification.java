package com.example.specification;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


import org.springframework.data.jpa.domain.Specification;

import com.example.entity.Transaction;
import com.example.enums.TransactionStatus;
import com.example.enums.TransactionType;

import jakarta.persistence.criteria.Predicate;

public class TransactionSpecification {

    public static Specification<Transaction> withFilters(
            UUID accountId,
            TransactionStatus status,
            TransactionType type,
            UUID cardId
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(
                cb.equal(root.get("account").get("accountId"), accountId)
            );

            if (status != null) {
                predicates.add(
                    cb.equal(root.get("transactionStatus"), status)
                );
            }

            if (type != null) {
                predicates.add(
                    cb.equal(root.get("transactionType"), type)
                );
            }

            if (cardId != null) {
                predicates.add(
                    cb.equal(root.get("card").get("cardId"), cardId)
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}