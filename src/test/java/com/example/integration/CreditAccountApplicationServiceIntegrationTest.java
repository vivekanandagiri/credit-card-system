package com.example.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import com.example.dto.request.CreditCardApplicationRequest;
import com.example.dto.response.CreditCardApplicationSummaryResponse;
import com.example.entity.CreditProduct;
import com.example.entity.Customer;
import com.example.enums.ApplicationStatus;
import com.example.exception.BadRequestException;
import com.example.exception.BusinessRuleException;
import com.example.repository.CreditCardApplicationRepository;
import com.example.repository.CreditProductRepository;
import com.example.repository.CustomerRepository;
import com.example.service.CreditAccountApplicationService;
import com.example.service.KycService;
import com.example.testutil.TestFixtures;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CreditAccountApplicationServiceIntegrationTest {

    @Autowired private CreditAccountApplicationService service;
    @Autowired private CreditCardApplicationRepository repository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private CreditProductRepository productRepository;
    @Autowired private KycService kycService;

    // ================= APPLY =================

    @Test
    void shouldApplySuccessfully_andPersistApplication() {
        // GIVEN
    	Customer customer = customerRepository.save(
    	        TestFixtures.validCustomerWithUser()
    	);
        

        // simulate KYC verified
    	verifyKyc(customer);

        CreditProduct product =
                productRepository.save(TestFixtures.validCreditProductEntity());

        CreditCardApplicationRequest request =
                TestFixtures.validApplicationRequest(product.getCreditProductId());

        // WHEN
        CreditCardApplicationSummaryResponse response =
                service.apply(customer.getUser().getUserId(), request);

        // THEN
        assertThat(response).isNotNull();

        var saved = repository
                .findAllByCustomerCustomerId(customer.getCustomerId());

        assertThat(saved).hasSize(1);
    }

    // ================= DUPLICATE =================

    @Test
    void shouldThrowException_whenDuplicateApplicationExists() {
        // GIVEN
        Customer customer = customerRepository.save(
                TestFixtures.validCustomerWithUser()
        );

        verifyKyc(customer); // 🔥 ADD THIS

        CreditProduct product =
                productRepository.save(TestFixtures.validCreditProductEntity());

        CreditCardApplicationRequest request =
                TestFixtures.validApplicationRequest(product.getCreditProductId());

        service.apply(customer.getUser().getUserId(), request);

        // WHEN + THEN
        assertThatThrownBy(() ->
        service.apply(customer.getUser().getUserId(), request))
        .isInstanceOf(BusinessRuleException.class);
    }

    // ================= VALIDATION =================

    @Test
    void shouldThrowException_whenInvalidCreditScore() {
        // GIVEN
        Customer customer = customerRepository.save(
                TestFixtures.validCustomerWithUser()
        );

        verifyKyc(customer); // 🔥 ADD THIS

        CreditProduct product =
                productRepository.save(TestFixtures.validCreditProductEntity());

        CreditCardApplicationRequest request =
                TestFixtures.validApplicationRequest(product.getCreditProductId());

        request.setCreditScoreAtApplication(100);

        // WHEN + THEN
        assertThatThrownBy(() ->
                service.apply(customer.getUser().getUserId(), request))
                .isInstanceOf(BusinessRuleException.class);
    }

    // ================= GET =================

    @Test
    void shouldReturnCustomerApplications() {
        // GIVEN
        Customer customer = customerRepository.save(
                TestFixtures.validCustomerWithUser()
        );

        verifyKyc(customer); 

        CreditProduct product =
                productRepository.save(TestFixtures.validCreditProductEntity());

        CreditCardApplicationRequest request =
                TestFixtures.validApplicationRequest(product.getCreditProductId());

        service.apply(customer.getUser().getUserId(), request);

        // WHEN
        List<CreditCardApplicationSummaryResponse> result =
                service.getCustomerApplications(customer.getUser().getUserId());

        // THEN
        assertThat(result).hasSize(1);
    }

    // ================= FILTER BY STATUS =================

    @Test
    void shouldReturnApplicationsByStatus() {
        // GIVEN
        Customer customer = customerRepository.save(
                TestFixtures.validCustomerWithUser()
        );

        verifyKyc(customer); 

        CreditProduct product =
                productRepository.save(TestFixtures.validCreditProductEntity());

        CreditCardApplicationRequest request =
                TestFixtures.validApplicationRequest(product.getCreditProductId());

        service.apply(customer.getUser().getUserId(), request);

        // WHEN
        var saved = repository.findAllByCustomerCustomerId(customer.getCustomerId());
        assertThat(saved).hasSize(1);

        ApplicationStatus status = saved.get(0).getApplicationStatus();

        List<CreditCardApplicationSummaryResponse> result =
                service.getCustomerApplicationsByStatus(
                        customer.getUser().getUserId(),
                        status.name()
                );

        assertThat(result).hasSize(1);
    }

    // ================= INVALID STATUS =================

    @Test
    void shouldThrowException_whenInvalidStatus() {
        // GIVEN
    	Customer customer = customerRepository.save(
    	        TestFixtures.validCustomerWithUser()
    	);
        // WHEN + THEN
        assertThatThrownBy(() ->
                service.getCustomerApplicationsByStatus(
                        customer.getUser().getUserId(),
                        "INVALID_STATUS"))
                .isInstanceOf(BadRequestException.class);
    }
    

    private void verifyKyc(Customer customer) {

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "dummy-content".getBytes()
        );

        kycService.uploadKyc(
                customer.getCustomerId(),
                "PAN",
                "ABCDE1234F",
                file 
        );

        var kyc = kycService.getKycStatus(customer.getCustomerId());

        kycService.updateKycStatus(
                kyc.getKycId(),
                UUID.randomUUID(),
                new com.example.dto.request.KycStatusUpdateRequest(
                        com.example.enums.KycStatus.VERIFIED,
                        null
                )
        );
    }
}