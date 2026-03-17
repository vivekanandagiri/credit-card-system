package com.example.service;


import com.example.dto.request.CreditAccountStatusUpdateRequest;
import com.example.dto.response.ApiResponse;
import com.example.dto.response.CreditAccountResponse;
import com.example.entity.CreditCardApplication;

import java.util.List;
import java.util.UUID;

public interface CreditAccountService {

    // Called internally after application approval — not exposed as API
    CreditAccountResponse createAccount(CreditCardApplication application);

    // Customer end points
    ApiResponse<List<CreditAccountResponse>> getMyAccounts(UUID userId);
    ApiResponse<CreditAccountResponse> getMyAccountById(UUID userId, UUID accountId);

    // Admin endpoints
    ApiResponse<List<CreditAccountResponse>> getAllAccounts();
    ApiResponse<List<CreditAccountResponse>> getAccountsByStatus(String status);
    ApiResponse<CreditAccountResponse> getAccountById(UUID accountId);
    ApiResponse<CreditAccountResponse> updateAccountStatus(UUID accountId,
                                                     CreditAccountStatusUpdateRequest request);
}