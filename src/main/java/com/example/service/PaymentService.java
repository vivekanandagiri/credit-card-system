package com.example.service;

import java.util.UUID;

import org.springframework.data.domain.Page;

import com.example.dto.request.PaymentRequest;
import com.example.dto.response.PaymentResponse;

public interface PaymentService {

	PaymentResponse makePayment(UUID accountId, PaymentRequest request);

	/**
	 * Retrieves a paginated details of payments applied to a specific statement.
	 * <p>
	 * Pagination is strictly enforced here to guarantee bounded 
	 * JVM memory consumption and predictable API response times. 
	 * Even if an enterprise account has thousands of micro-transactions, this ensures 
	 * the database never attempts to load them all into memory at once.
	 *
	 * @param statementId for which we will get the payment details
	 * @param page the zero-based page index requested by the client
	 * @param size the maximum number of records per page to return
	 * @return a memory-safe, paginated response payload
	 */
	Page<PaymentResponse> getPayments(UUID accountId, int page, int size);

	/**
	 * Fetches a specific payment record .
	 * <p>
	 * By querying with both the paymentId AND the statementId, A malicious user cannot 
	 * simply guess another user's payment UUID to access their financial data, because the 
	 * query will enforce that the payment belongs to the authorized statement context.
	 */
	PaymentResponse getPaymentById(UUID accountId, UUID paymentId);

	PaymentResponse getByReferenceId(String referenceId);
	


    
    
}