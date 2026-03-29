package com.example.service;

import com.example.dto.request.ApplicationDecisionRequest;
import com.example.dto.request.CreditCardApplicationRequest;
import com.example.dto.response.CreditCardApplicationResponse;
import com.example.dto.response.CreditCardApplicationSummaryResponse;

import java.util.List;
import java.util.UUID;

public interface CreditAccountApplicationService {

	CreditCardApplicationSummaryResponse apply(UUID userId, CreditCardApplicationRequest request);

    List<CreditCardApplicationSummaryResponse> getCustomerApplications(UUID userId);

    List<CreditCardApplicationSummaryResponse> getCustomerApplicationsByStatus(UUID userId, String status);

    CreditCardApplicationResponse getCustomerApplicationById(UUID customerId, UUID applicationId);

    CreditCardApplicationResponse getApplicationById(UUID applicationId);

    List<CreditCardApplicationSummaryResponse> getAllApplications();

    List<CreditCardApplicationSummaryResponse> getApplicationsByStatus(String status);

    CreditCardApplicationResponse decide(UUID applicationId, ApplicationDecisionRequest request);
	
}