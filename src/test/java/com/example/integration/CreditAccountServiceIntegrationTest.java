package com.example.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.example.entity.CreditCardApplication;
import com.example.entity.CreditProduct;
import com.example.entity.Customer;
import com.example.enums.ApplicationStatus;
import com.example.enums.UserRole;
import com.example.exception.BusinessRuleException;
import com.example.repository.CreditCardApplicationRepository;
import com.example.repository.CreditProductRepository;
import com.example.repository.CustomerRepository;
import com.example.service.CreditAccountService;
import com.example.testutil.TestFixtures;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CreditAccountServiceIntegrationTest {

    @Autowired private CreditAccountService accountService;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private CreditProductRepository productRepository;
    @Autowired
    private CreditCardApplicationRepository applicationRepository;
    // ================= CREATE ACCOUNT =================

    @Test
    void shouldCreateAccountSuccessfully() {
    	Customer customer = customerRepository.save(
    	        TestFixtures.validCustomerWithUser()
    	);

    	// 🔥 SAVE PRODUCT FIRST
    	CreditProduct product =
    	        productRepository.save(TestFixtures.validCreditProductEntity());

    	CreditCardApplication application =
    	        TestFixtures.validApplication(customer, product);

    	application.setApplicationStatus(ApplicationStatus.APPROVED);
    	application.setApprovedCreditLimit(BigDecimal.valueOf(100000));
    	application.setApprovedApr(BigDecimal.valueOf(12));

    	// NOW WORKS ✅
    	accountService.createAccount(application);
    }

    // ================= INVALID =================

    @Test
    void shouldThrowException_whenApplicationNotApproved() {
        // GIVEN
        Customer customer = customerRepository.save(
                TestFixtures.validCustomerWithUser()
        );

        CreditProduct product =
                TestFixtures.validCreditProductEntity();

        CreditCardApplication application =
                TestFixtures.validApplication(customer, product);

        application.setApplicationStatus(ApplicationStatus.REJECTED);

        // WHEN + THEN
        assertThatThrownBy(() ->
                accountService.createAccount(application))
                .isInstanceOf(BusinessRuleException.class);
    }

    // ================= FETCH =================

    @Test
    void shouldReturnCustomerAccounts() {
        // GIVEN
        Customer customer = customerRepository.save(
                TestFixtures.validCustomerWithUser()
        );

        CreditProduct product =
                productRepository.save(TestFixtures.validCreditProductEntity());

        CreditCardApplication application =
                TestFixtures.validApplication(customer, product);

        // 🔥 SAVE APPLICATION
        application = applicationRepository.save(application);

        application.setApplicationStatus(ApplicationStatus.APPROVED);
        application.setApprovedCreditLimit(BigDecimal.valueOf(100000));
        application.setApprovedApr(BigDecimal.valueOf(12));

        // NOW WORKS ✅
        accountService.createAccount(application);

        // WHEN
        List<?> accounts =
                accountService.getAccounts(
                        customer.getUser().getUserId(),
                        UserRole.CUSTOMER,
                        null
                );

        // THEN
        assertThat(accounts).hasSize(1);
    }
}