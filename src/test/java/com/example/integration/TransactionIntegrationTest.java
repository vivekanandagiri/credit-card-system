package com.example.integration;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.util.UUID;

import com.example.dto.request.TransactionRequest;
import com.example.dto.response.TransactionDetailResponse;
import com.example.dto.response.TransactionSummaryResponse;
import com.example.entity.*;
import com.example.enums.*;
import com.example.repository.*;
import com.example.service.TransactionService;
import com.example.testutil.TestFixtures;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TransactionIntegrationTest {

    @Autowired private TransactionService service;

    @Autowired private CustomerRepository customerRepository;
    @Autowired private CreditProductRepository creditProductRepository;
    @Autowired private CreditCardApplicationRepository applicationRepository;
    @Autowired private CreditAccountRepository accountRepository;
    @Autowired private CreditCardProductRepository cardProductRepository;
    @Autowired private CreditCardRepository cardRepository;
    @Autowired private TransactionRepository transactionRepository;

    // ================= HELPER =================



    private TransactionRequest validRequest() {
        TransactionRequest req = new TransactionRequest();
        req.setAmount(BigDecimal.valueOf(500));
        req.setTransactionReference("REF_" + UUID.randomUUID());
        req.setTransactionType(TransactionType.PURCHASE);
        req.setTransactionChannel(TransactionChannel.ONLINE);
        req.setMerchantName("Amazon");
        req.setMerchantCategoryCode("5411");
        req.setMerchantCategoryName("GROCERY");
        return req;
    }

    // ================= POST TRANSACTION =================

    @Test
    void shouldPostTransactionSuccessfully() {

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

        TransactionRequest req = validRequest();

        // WHEN
        TransactionSummaryResponse res =
                service.postTransaction(customer.getUser().getUserId(), card.getCardId(), req);

        // THEN
        assertThat(res).isNotNull();
        assertThat(res.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(500));
    }

    // ================= IDEMPOTENCY =================

    @Test
    void shouldReturnSameTransaction_whenDuplicateReference() {

        // GIVEN (reuse setup)
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

        TransactionRequest req = validRequest();
        req.setTransactionReference("FIXED_REF");

        // first call
        service.postTransaction(customer.getUser().getUserId(), card.getCardId(), req);

        // second call
        TransactionSummaryResponse res =
                service.postTransaction(customer.getUser().getUserId(), card.getCardId(), req);

        // THEN
        assertThat(res).isNotNull();
    }

    // ================= GET TRANSACTIONS =================

    @Test
    void shouldGetTransactionsSuccessfully() {

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

        TransactionRequest req = validRequest();
        service.postTransaction(customer.getUser().getUserId(), card.getCardId(), req);

        // WHEN
        Page<TransactionSummaryResponse> res =
                service.getAccountTransactions(
                        customer.getUser().getUserId(),
                        account.getAccountId(),
                        null, null, null,
                        0, 10
                );

        assertThat(res.getContent()).isNotEmpty();
    }

    // ================= GET BY ID =================

    @Test
    void shouldGetTransactionByIdSuccessfully() {

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

        TransactionRequest req = validRequest();
        service.postTransaction(customer.getUser().getUserId(), card.getCardId(), req);

        Transaction txn = transactionRepository.findAll().get(0);

        // WHEN
        TransactionDetailResponse res =
                service.getAccountTransactionById(
                        customer.getUser().getUserId(),
                        account.getAccountId(),
                        txn.getTransactionId()
                );

        // THEN
        assertThat(res).isNotNull();
    }

    // ================= ACCESS DENIED =================

    @Test
    void shouldThrow_whenAccessingOtherUsersTransaction() {

        // GIVEN
        Customer c1 = TestFixtures.validCustomerWithUser();
        Customer c2 = TestFixtures.validCustomerWithUser();

        customerRepository.save(c1);
        customerRepository.save(c2);

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

        TransactionRequest req = validRequest();
        service.postTransaction(c1.getUser().getUserId(), card.getCardId(), req);

        Transaction txn = transactionRepository.findAll().get(0);

        // WHEN + THEN
        assertThatThrownBy(() ->
                service.getAccountTransactionById(
                        c2.getUser().getUserId(),
                        account.getAccountId(),
                        txn.getTransactionId()
                )
        ).isInstanceOf(Exception.class);
    }
}