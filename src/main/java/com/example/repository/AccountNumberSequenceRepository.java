package com.example.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

/**
 * Fetches the next value from the PostgreSQL native sequence.
 * PostgreSQL guarantees uniqueness and handles concurrency natively.
 */
@Repository
public class AccountNumberSequenceRepository {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Returns the next sequence value (1 to 999999).
     * Each call increments the sequence atomically in PostgreSQL.
     * Thread-safe — no application-level locking needed.
     */
    public Long nextValue() {
        return (Long) entityManager
                .createNativeQuery("SELECT nextval('account_number_seq')")
                .getSingleResult();
    }
}