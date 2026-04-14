package com.example.mapper;

import com.example.dto.response.KycResponse;
import com.example.entity.Customer;
import com.example.entity.KycRecord;
import com.example.enums.KycStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;

/**
 * Structural mapper for the Identity Verification (KYC) domain.
 * <p>
 * Architecture Note: Manual mapping is strictly enforced here to safely handle 
 * I/O operations (MultipartFile) and to control the application of default 
 * compliance states.
 */
@Component
public class KycMapper {
	
	/**
     * Translates an incoming KYC submission into a  domain entity.
     *
     * @param customer the verified parent entity
     * @param documentType the category of the document (e.g., PASSPORT, DRIVERS_LICENSE)
     * @param documentNumber the parsed identification number
     * @param file the raw multipart upload
     * @return an active KYC record ready for compliance review
     * @throws IOException if the file stream cannot be read
     */
    public KycRecord toEntity(Customer customer,
                              String documentType,
                              String documentNumber,
                              MultipartFile file) throws IOException {

        KycRecord kyc = new KycRecord();

        kyc.setCustomer(customer);
        kyc.setDocumentType(documentType);
        kyc.setDocumentNumber(documentNumber);


        kyc.setDocumentFile(file.getBytes());
        kyc.setFileName(file.getOriginalFilename());
        kyc.setContentType(file.getContentType());

        // All new uploads immediately enter the review queue.
        kyc.setStatus(KycStatus.SUBMITTED);
        kyc.setSubmittedAt(Instant.now());
        // This is the active record. The KycService is responsible for deactivating 
        // any older records before saving this one.
        kyc.setActive(true);

        return kyc;
    }

    /**
     * Projects the KYC entity into a safe, lightweight API response.
     */
    public KycResponse toResponse(KycRecord kyc) {

        return new KycResponse(
                kyc.getKycId(),
                kyc.getStatus(),
                kyc.getSubmittedAt()
        );
    }
}