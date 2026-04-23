package com.example.integration;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.UUID;

import com.example.dto.request.CreditCardIssuanceRequest;
import com.example.dto.request.CreditCardStatusUpdateRequest;
import com.example.dto.response.CreditCardResponse;
import com.example.entity.*;
import com.example.enums.*;
import com.example.repository.*;
import com.example.security.CustomUserPrincipal;
import com.example.service.CreditCardService;
import com.example.testutil.TestFixtures;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CreditCardIntegrationTest {

    @Autowired private CreditCardService service;

    @Autowired private CustomerRepository customerRepository;
    @Autowired private CreditProductRepository creditProductRepository;
    @Autowired private CreditCardProductRepository cardProductRepository;
    @Autowired private CreditCardApplicationRepository applicationRepository;
    @Autowired private CreditAccountRepository accountRepository;
    @Autowired private CreditCardRepository cardRepository;

    // ================= ISSUE CARD =================

    @Test
    void shouldIssueCardSuccessfully() {

        // GIVEN
        Customer customer = TestFixtures.validCustomerWithUser();
        customerRepository.save(customer);

        UUID userId = customer.getUser().getUserId();

        CreditProduct product = TestFixtures.validCreditProductEntity();
        creditProductRepository.save(product);

        CreditCardApplication app =
                TestFixtures.validApplication(customer, product);
        applicationRepository.save(app);

        CreditAccount account =
                TestFixtures.validCreditAccount(customer, app, product);
        accountRepository.save(account);

        CreditCardProduct cardProduct =
                TestFixtures.validCreditCardProduct();
        cardProductRepository.save(cardProduct);

        CreditCardIssuanceRequest req = new CreditCardIssuanceRequest();
        req.setCardProductId(cardProduct.getCardProductId());
        req.setCardFormat(CardFormat.VIRTUAL);
        req.setIssuanceReason(CardIssuanceReason.NEW_CARD);

        // WHEN
        CreditCardResponse res =
                service.issueCard(userId, account.getAccountId(), req);

        // THEN
        assertThat(res).isNotNull();
        assertThat(res.getCardFormat()).isEqualTo(CardFormat.VIRTUAL);
    }

    // ================= GET CARDS =================

    @Test
    void shouldGetCardsByAccount() {

        // GIVEN (reuse setup)
        Customer customer = TestFixtures.validCustomerWithUser();
        customerRepository.save(customer);

        UUID userId = customer.getUser().getUserId();

        CreditProduct product = TestFixtures.validCreditProductEntity();
        creditProductRepository.save(product);

        CreditCardApplication app =
                TestFixtures.validApplication(customer, product);
        applicationRepository.save(app);

        CreditAccount account =
                TestFixtures.validCreditAccount(customer, app, product);
        accountRepository.save(account);

        CreditCardProduct cardProduct =
                TestFixtures.validCreditCardProduct();
        cardProductRepository.save(cardProduct);

        CreditCard card =
                TestFixtures.validCreditCard(account, cardProduct);
        cardRepository.save(card);

        // WHEN
        List<CreditCardResponse> cards =
                service.getCardsByAccount(userId, account.getAccountId());

        // THEN
        assertThat(cards).hasSize(1);
    }

    // ================= GET CARD BY ID =================

    @Test
    void shouldGetCardByIdSuccessfully() {

        // GIVEN
        Customer customer = TestFixtures.validCustomerWithUser();
        customerRepository.save(customer);

        UUID userId = customer.getUser().getUserId();

        CreditProduct product = TestFixtures.validCreditProductEntity();
        creditProductRepository.save(product);

        CreditCardApplication app =
                TestFixtures.validApplication(customer, product);
        applicationRepository.save(app);

        CreditAccount account =
                TestFixtures.validCreditAccount(customer, app, product);
        accountRepository.save(account);

        CreditCardProduct cardProduct =
                TestFixtures.validCreditCardProduct();
        cardProductRepository.save(cardProduct);

        CreditCard card =
                TestFixtures.validCreditCard(account, cardProduct);
        cardRepository.save(card);

        // WHEN
        CreditCardResponse res =
                service.getCardById(userId, account.getAccountId(), card.getCardId());

        // THEN
        assertThat(res).isNotNull();
        assertThat(res.getMaskedCardNumber()).isNotNull();
    }

    // ================= UPDATE STATUS =================

    @Test
    void shouldUpdateCardStatusSuccessfully() {

        // GIVEN
        Customer customer = TestFixtures.validCustomerWithUser();
        customerRepository.save(customer);

        CreditProduct product = TestFixtures.validCreditProductEntity();
        creditProductRepository.save(product);

        CreditCardApplication app =
                TestFixtures.validApplication(customer, product);
        applicationRepository.save(app);

        CreditAccount account =
                TestFixtures.validCreditAccount(customer, app, product);
        accountRepository.save(account);

        CreditCardProduct cardProduct =
                TestFixtures.validCreditCardProduct();
        cardProductRepository.save(cardProduct);

        CreditCard card =
                TestFixtures.validCreditCard(account, cardProduct);
        cardRepository.save(card);

        CreditCardStatusUpdateRequest req = new CreditCardStatusUpdateRequest();
        req.setStatus(CardStatus.BLOCKED);

        // WHEN
        CustomUserPrincipal principal = new CustomUserPrincipal(
                customer.getUser().getUserId(),
                customer.getCustomerId(),
                customer.getUser().getEmail(),
                customer.getUser().getPasswordHash(),
                customer.getUser().getRole()
        );

        service.updateCardStatusForUser(
                principal,
                account.getAccountId(),
                card.getCardId(),
                req
        );

        // THEN
        CreditCard updated =
                cardRepository.findById(card.getCardId()).orElseThrow();

        assertThat(updated.getCardStatus()).isEqualTo(CardStatus.BLOCKED);
        assertThat(updated.getBlockedAt()).isNotNull();
    }

    // ================= ACCESS DENIED =================

    @Test
    void shouldThrow_whenAccessingOtherUsersCard() {

        // GIVEN
        Customer c1 = TestFixtures.validCustomerWithUser();
        Customer c2 = TestFixtures.validCustomerWithUser();

        customerRepository.save(c1);
        customerRepository.save(c2);

        UUID user2 = c2.getUser().getUserId();

        CreditProduct product = TestFixtures.validCreditProductEntity();
        creditProductRepository.save(product);

        CreditCardApplication app =
                TestFixtures.validApplication(c1, product);
        applicationRepository.save(app);

        CreditAccount account =
                TestFixtures.validCreditAccount(c1, app, product);
        accountRepository.save(account);

        CreditCardProduct cardProduct =
                TestFixtures.validCreditCardProduct();
        cardProductRepository.save(cardProduct);

        CreditCard card =
                TestFixtures.validCreditCard(account, cardProduct);
        cardRepository.save(card);

        // WHEN + THEN
        assertThatThrownBy(() ->
                service.getCardById(user2, account.getAccountId(), card.getCardId()))
                .isInstanceOf(Exception.class);
    }
}