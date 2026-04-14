package com.example.service;

import com.example.dto.request.ApplicationDecisionRequest;
import com.example.dto.request.CreditCardApplicationRequest;
import com.example.dto.response.CreditCardApplicationResponse;
import com.example.dto.response.CreditCardApplicationSummaryResponse;

import java.util.List;
import java.util.UUID;

public interface CreditAccountApplicationService {

	/**
	 * Submits a new credit card application.
	 *
	 * <p>Performs multiple validations including KYC, credit score, duplicate applications,
	 * and business rules before sending the application for underwriting.</p>
	 *
	 * <p>If auto-approved, a credit account is created immediately.</p>
	 *
	 * @param userId  the user ID of the applicant
	 * @param request the application request payload
	 * @return summary response of the created application
	 */
	CreditCardApplicationSummaryResponse apply(UUID userId, CreditCardApplicationRequest request);

	/**
	 * Retrieves all applications for a given user.
	 *
	 * @param userId user identifier
	 * @return list of application summaries
	 */
	List<CreditCardApplicationSummaryResponse> getCustomerApplications(UUID userId);
	

	/**
	 * Retrieves applications filtered by status for a customer.
	 *
	 * @param userId user identifier
	 * @param status application status (string)
	 * @return filtered list of applications
	 */
	List<CreditCardApplicationSummaryResponse> getCustomerApplicationsByStatus(UUID userId, String status);

	/**
	 * Retrieves a specific application belonging to a customer.
	 *
	 * @param customerId    customer ID
	 * @param applicationId application ID
	 * @return application details
	 * @throws AccessDeniedException if application does not belong to the customer
	 */
	CreditCardApplicationResponse getCustomerApplicationById(UUID customerId, UUID applicationId);

	/**
	 * Retrieves an application by ID (admin/internal use).
	 *
	 * @param applicationId application ID
	 * @return application details
	 */
	CreditCardApplicationResponse getApplicationById(UUID applicationId);

	/**
	 * Retrieves all applications (admin).
	 *
	 * @return list of all application summaries
	 */
	List<CreditCardApplicationSummaryResponse> getAllApplications();

	/**
	 * Retrieves applications filtered by status (admin).
	 *
	 * @param status application status
	 * @return filtered list
	 */
	List<CreditCardApplicationSummaryResponse> getApplicationsByStatus(String status);

	/**
	 * Performs manual decision on an application (admin only).
	 *
	 * <p>Only applications in {@code PENDING_REVIEW} state can be manually decided.</p>
	 *
	 * @param applicationId application ID
	 * @param request       decision request
	 * @return updated application response
	 */
	CreditCardApplicationResponse decide(UUID applicationId, ApplicationDecisionRequest request);

	
}