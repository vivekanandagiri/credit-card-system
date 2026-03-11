package com.example.mapper;

import com.example.dto.response.KycResponse;
import com.example.entity.Customer;
import com.example.entity.KycRecord;
import com.example.enums.KycStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;

@Component
public class KycMapper {

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

        kyc.setStatus(KycStatus.SUBMITTED);
        kyc.setSubmittedAt(Instant.now());
        kyc.setActive(true);

        return kyc;
    }


    public KycResponse toResponse(KycRecord kyc) {

        return new KycResponse(
                kyc.getKycId(),
                kyc.getStatus(),
                kyc.getSubmittedAt()
        );
    }
}