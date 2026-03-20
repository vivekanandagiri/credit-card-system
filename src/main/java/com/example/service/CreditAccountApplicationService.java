package com.example.service;

import com.example.dto.request.ApplicationDecisionRequest;
import com.example.dto.request.CreditCardApplicationRequest;
import com.example.dto.response.ApiResponse;
import com.example.dto.response.CreditCardApplicationCreateResponse;
import com.example.dto.response.CreditCardApplicationResponse;
import java.util.List;
import java.util.UUID;

public interface CreditAccountApplicationService {

    // Customer — view available card products before applying
	// ApiResponse<List<CreditProductResponse>> getAvailableCreditProducts();

    // Customer — submit application
    ApiResponse<CreditCardApplicationCreateResponse> apply(UUID userId,
                                                     CreditCardApplicationRequest request);

    // Customer — view own applications
    ApiResponse<List<CreditCardApplicationResponse>> getMyApplications(UUID userId);

    // Customer — view single application
    ApiResponse<CreditCardApplicationResponse> getMyApplicationById(UUID userId,
                                                                    UUID applicationId);
    //Customer -view single Application
    ApiResponse<CreditCardApplicationResponse> getApplicationById(UUID applicationId);
    
    // Admin — view all applications
    ApiResponse<List<CreditCardApplicationResponse>> getAllApplications();

    // Admin — view applications by status
    ApiResponse<List<CreditCardApplicationResponse>> getApplicationsByStatus(String status);

    // Admin — manually approve or reject (for PENDING_REVIEW cases)
    ApiResponse<CreditCardApplicationResponse> decide(UUID applicationId,
                                                      ApplicationDecisionRequest request);

	
}