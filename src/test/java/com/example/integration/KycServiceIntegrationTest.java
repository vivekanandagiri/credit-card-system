package com.example.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import com.example.dto.request.KycStatusUpdateRequest;
import com.example.entity.Customer;
import com.example.enums.KycStatus;
import com.example.exception.BusinessRuleException;
import com.example.repository.CustomerRepository;
import com.example.repository.KycRepository;
import com.example.service.KycService;
import com.example.testutil.TestFixtures;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.mock.web.MockMultipartFile;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class KycServiceIntegrationTest {

    @Autowired private KycService kycService;
    @Autowired private KycRepository kycRepository;
    @Autowired private CustomerRepository customerRepository;

    // ================= UPLOAD KYC =================

    @Test
    void shouldUploadKyc_andPersistInDatabase() throws Exception {
        // GIVEN
        Customer customer = customerRepository.save(TestFixtures.validCustomer());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "pan.jpg",
                "image/jpeg",
                "data".getBytes()
        );

        // WHEN
        String response = kycService.uploadKyc(
                customer.getCustomerId(),
                "PAN",
                "ABCDE1234F",
                file
        );

        // THEN
        assertThat(response).isEqualTo("SUBMITTED");

        var kyc = kycRepository
                .findByCustomerCustomerIdAndIsActiveTrue(customer.getCustomerId());

        assertThat(kyc).isPresent();
        assertThat(kyc.get().getStatus()).isEqualTo(KycStatus.SUBMITTED);
    }

    // ================= DUPLICATE KYC =================

    @Test
    void shouldThrowException_whenKycAlreadySubmitted() throws Exception {
        // GIVEN
        Customer customer = customerRepository.save(TestFixtures.validCustomer());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "pan.jpg",
                "image/jpeg",
                "data".getBytes()
        );

        kycService.uploadKyc(customer.getCustomerId(), "PAN", "ABCDE1234F", file);

        // WHEN + THEN
        assertThatThrownBy(() ->
                kycService.uploadKyc(customer.getCustomerId(), "PAN", "ABCDE1234F", file))
                .isInstanceOf(BusinessRuleException.class);
    }

    // ================= UPDATE KYC =================

    @Test
    void shouldUpdateKycStatus_toVerified() throws Exception {
        // GIVEN
        Customer customer = customerRepository.save(TestFixtures.validCustomer());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "pan.jpg",
                "image/jpeg",
                "data".getBytes()
        );

        kycService.uploadKyc(customer.getCustomerId(), "PAN", "ABCDE1234F", file);

        var kyc = kycRepository
                .findByCustomerCustomerIdAndIsActiveTrue(customer.getCustomerId())
                .orElseThrow();

        UUID adminId = UUID.randomUUID();

        KycStatusUpdateRequest request =
                new KycStatusUpdateRequest(KycStatus.VERIFIED, null);

        // WHEN
        String response = kycService.updateKycStatus(
                kyc.getKycId(),
                adminId,
                request
        );

        // THEN
        assertThat(response).isEqualTo("VERIFIED");

        var updated = kycRepository.findById(kyc.getKycId()).orElseThrow();

        assertThat(updated.getStatus()).isEqualTo(KycStatus.VERIFIED);
        assertThat(updated.getVerifiedBy()).isEqualTo(adminId);
        assertThat(updated.getVerifiedAt()).isNotNull();
    }

    // ================= GET KYC =================

    @Test
    void shouldReturnKycStatus_whenExists() throws Exception {
        // GIVEN
        Customer customer = customerRepository.save(TestFixtures.validCustomer());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "pan.jpg",
                "image/jpeg",
                "data".getBytes()
        );

        kycService.uploadKyc(customer.getCustomerId(), "PAN", "ABCDE1234F", file);

        // WHEN
        var response = kycService.getKycStatus(customer.getCustomerId());

        // THEN
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(KycStatus.SUBMITTED);
    }

    // ================= VERIFIED CHECK =================

    @Test
    void shouldReturnTrue_whenKycVerified() throws Exception {
        // GIVEN
        Customer customer = customerRepository.save(TestFixtures.validCustomer());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "pan.jpg",
                "image/jpeg",
                "data".getBytes()
        );

        kycService.uploadKyc(customer.getCustomerId(), "PAN", "ABCDE1234F", file);

        var kyc = kycRepository
                .findByCustomerCustomerIdAndIsActiveTrue(customer.getCustomerId())
                .orElseThrow();

        kycService.updateKycStatus(
                kyc.getKycId(),
                UUID.randomUUID(),
                new KycStatusUpdateRequest(KycStatus.VERIFIED, null)
        );

        // WHEN
        boolean result = kycService.isKycVerified(customer.getCustomerId());

        // THEN
        assertThat(result).isTrue();
    }
}