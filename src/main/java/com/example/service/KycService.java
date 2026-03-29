package com.example.service;

import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import com.example.dto.request.KycStatusUpdateRequest;
import com.example.dto.response.KycResponse;

/**
 * Orchestrates Identity Verification (KYC) operations.
 * Handles document ingestion, back-office reviews, and compliance gating.
 */
public interface KycService {

    /** * Ingests a new KYC document payload from a customer.
     * <p>
     * Note: This will logically invalidate any previously active KYC records 
     * to ensure only one active review exists per customer.
     */
    String uploadKyc(UUID customerId, String documentType, String documentNumber, MultipartFile file);
    
    /** * (Back-Office) Finalizes or rejects a pending KYC record.
     * Requires an explicit rejection reason if the application is declined.
     */
    String updateKycStatus(UUID kycId, UUID adminId, KycStatusUpdateRequest request);

    /** * Retrieves the current, active KYC status for the authenticated customer. 
     */
    KycResponse getKycStatus(UUID customerId);
    
    /** * (Back-Office) Retrieves the queue of all KYC records awaiting manual review. 
     */
    List<KycResponse> getPendingKyc();

    /**
     * Fast-path compliance gate.
     * Evaluates if the customer has successfully cleared identity verification 
     * without hydrating full KYC entities or document blobs into memory.
     */
    boolean isKycVerified(UUID customerId);
}