package com.example.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.testutil.TestFixtures;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.example.dto.request.TransactionRequest;
import com.example.dto.response.*;
import com.example.entity.*;
import com.example.enums.*;
import com.example.exception.*;
import com.example.mapper.TransactionMapper;
import com.example.repository.TransactionRepository;
import com.example.service.ServiceImpl.LedgerServiceImpl;
import com.example.service.ServiceImpl.TransactionServiceImpl;
import com.example.util.ReferenceNumberGenerator;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock private TransactionRepository repository;
    @Mock private CustomerService customerService;
    @Mock private CreditCardService cardService;
    @Mock private CreditAccountService accountService;
    @Mock private TransactionMapper mapper;
    @Mock private ReferenceNumberGenerator refGenerator;
    @Mock private LedgerServiceImpl ledgerService;
    @Mock private AuthorizationService authorizationService;

    @InjectMocks
    private TransactionServiceImpl service;

    private UUID userId;
    private UUID cardId;
    private UUID accountId;

    private Customer customer;
    private CreditAccount account;
    private CreditCard card;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        cardId = UUID.randomUUID();
        accountId = UUID.randomUUID();

        customer = TestFixtures.validCustomer();

        account = TestFixtures.validCreditAccount(customer, null, TestFixtures.validCreditProductEntity());
        account.setAccountId(accountId);

        card = new CreditCard();
        card.setCardId(cardId);
        card.setCreditAccount(account);
        card.setCardStatus(CardStatus.ACTIVE);
        card.setOnlineEnabled(true);
        card.setAtmEnabled(true);
        card.setCardProduct(TestFixtures.validCreditCardProduct());
    }

    // ================= POST TRANSACTION =================

    
    @Nested
    class PostTransaction {

        @Test
        void shouldProcessTransaction_success() {
            TransactionRequest req = mock(TransactionRequest.class);

            when(req.getAmount()).thenReturn(BigDecimal.valueOf(1000));
            when(req.getTransactionReference()).thenReturn("REF123");
            when(req.getTransactionType()).thenReturn(TransactionType.PURCHASE);
            when(req.getTransactionChannel()).thenReturn(TransactionChannel.ONLINE);

            when(repository.findByNetworkReference("REF123")).thenReturn(Optional.empty());
            when(customerService.getCustomerByUserId(userId)).thenReturn(customer);
            when(cardService.getCardEntity(cardId)).thenReturn(card);

            when(ledgerService.getBalance(accountId)).thenReturn(BigDecimal.ZERO);

            Authorization auth = mock(Authorization.class);
            when(auth.getId()).thenReturn(UUID.randomUUID());

            when(authorizationService.authorize(any(), any(), any(), any())).thenReturn(auth);

            when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(mapper.toSummaryResponse(any())).thenReturn(mock(TransactionSummaryResponse.class));

            TransactionSummaryResponse res =
                    service.postTransaction(userId, cardId, req);

            assertThat(res).isNotNull();
        }

        @Test
        void shouldThrow_whenTransactionNotFound() {
            when(customerService.getCustomerByUserId(userId)).thenReturn(customer);

            when(accountService.getAccountEntity(accountId)).thenReturn(account);

            // ensure ownership passes
            account.setCustomer(customer);

            // actual test condition
            when(repository.findById(any())).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    service.getAccountTransactionById(userId, accountId, UUID.randomUUID()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
        @Test
        void shouldThrow_whenInvalidAmount() {
            TransactionRequest req = mock(TransactionRequest.class);
            when(req.getAmount()).thenReturn(BigDecimal.ZERO);

            assertThatThrownBy(() ->
                    service.postTransaction(userId, cardId, req))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        void shouldThrow_whenCardNotOwned() {
            Customer another = new Customer();
            another.setCustomerId(UUID.randomUUID());
            account.setCustomer(another);

            TransactionRequest req = mock(TransactionRequest.class);
            when(req.getAmount()).thenReturn(BigDecimal.valueOf(100));
            when(req.getTransactionReference()).thenReturn("REF");
            when(req.getTransactionType()).thenReturn(TransactionType.PURCHASE);

            when(repository.findByNetworkReference(any())).thenReturn(Optional.empty());
            when(customerService.getCustomerByUserId(userId)).thenReturn(customer);
            when(cardService.getCardEntity(cardId)).thenReturn(card);

            assertThatThrownBy(() ->
                    service.postTransaction(userId, cardId, req))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void shouldDecline_whenCardInactive() {
            card.setCardStatus(CardStatus.BLOCKED);

            TransactionRequest req = mock(TransactionRequest.class);
            when(req.getAmount()).thenReturn(BigDecimal.valueOf(100));
            when(req.getTransactionReference()).thenReturn("REF");
            when(req.getTransactionType()).thenReturn(TransactionType.PURCHASE);
            when(req.getTransactionChannel()).thenReturn(TransactionChannel.ONLINE);

            when(repository.findByNetworkReference(any())).thenReturn(Optional.empty());
            when(customerService.getCustomerByUserId(userId)).thenReturn(customer);
            when(cardService.getCardEntity(cardId)).thenReturn(card);

            when(mapper.toSummaryResponse(any())).thenReturn(mock(TransactionSummaryResponse.class));

            TransactionSummaryResponse res =
                    service.postTransaction(userId, cardId, req);

            assertThat(res).isNotNull();
        }
    }

    // ================= GET TRANSACTIONS =================

    @Test
    void shouldGetTransactions_success() {
        when(customerService.getCustomerByUserId(userId)).thenReturn(customer);
        when(accountService.getAccountEntity(accountId)).thenReturn(account);

        Page<Transaction> page = new PageImpl<>(List.of(new Transaction()));

        // ✅ FIXED STUB
        when(repository.findAll(
                ArgumentMatchers.<Specification<Transaction>>any(),
                ArgumentMatchers.any(Pageable.class)
        )).thenReturn(page);

        when(mapper.toSummaryResponse(any()))
                .thenReturn(mock(TransactionSummaryResponse.class));

        Page<TransactionSummaryResponse> res =
                service.getAccountTransactions(userId, accountId, null, null, null, 0, 10);

        assertThat(res.getContent()).hasSize(1);
    }

    @Test
    void shouldThrow_whenInvalidPage() {
        when(customerService.getCustomerByUserId(userId)).thenReturn(customer);
        when(accountService.getAccountEntity(accountId)).thenReturn(account);

        assertThatThrownBy(() ->
                service.getAccountTransactions(userId, accountId, null, null, null, -1, 10))
                .isInstanceOf(BadRequestException.class);
    }

    // ================= GET BY ID =================

    @Test
    void shouldGetTransactionById_success() {
        Transaction txn = new Transaction();
        txn.setAccount(account);

        when(customerService.getCustomerByUserId(userId)).thenReturn(customer);
        when(accountService.getAccountEntity(accountId)).thenReturn(account);
        when(repository.findById(any())).thenReturn(Optional.of(txn));
        when(mapper.toResponse(any())).thenReturn(mock(TransactionDetailResponse.class));

        TransactionDetailResponse res =
                service.getAccountTransactionById(userId, accountId, UUID.randomUUID());

        assertThat(res).isNotNull();
    }

    @Test
    void shouldThrow_whenTransactionNotFound() {

        UUID transactionId = UUID.randomUUID();

        when(customerService.getCustomerByUserId(userId)).thenReturn(customer);

        when(accountService.getAccountEntity(accountId)).thenReturn(account);

        account.setCustomer(customer);

        when(repository.findById(transactionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.getAccountTransactionById(userId, accountId, transactionId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ================= PAYMENT =================

    @Test
    void shouldRecordPayment_success() {
        Payment payment = new Payment();
        payment.setAmount(BigDecimal.valueOf(1000));
        payment.setReferenceId("PAY123");
        payment.setPaidAt(Instant.now());
        payment.setPaymentMethod(PaymentMethod.UPI);

        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(mapper.toSummaryResponse(any())).thenReturn(mock(TransactionSummaryResponse.class));

        TransactionSummaryResponse res =
                service.recordPayment(account, payment);

        assertThat(res).isNotNull();
        verify(ledgerService).credit(any(), any(), any(), any(),any());
    }

    @Test
    void shouldThrow_whenInvalidPaymentAmount() {
        Payment payment = new Payment();
        payment.setAmount(BigDecimal.ZERO);

        assertThatThrownBy(() ->
                service.recordPayment(account, payment))
                .isInstanceOf(BadRequestException.class);
    }

    // ================= SYSTEM TXN =================

    @Test
    void shouldPostSystemTransaction_success() {
        when(repository.findByInternalReference(any())).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(mapper.toSummaryResponse(any())).thenReturn(mock(TransactionSummaryResponse.class));

        TransactionSummaryResponse res =
                service.postSystemTransaction(
                        account,
                        TransactionType.FEE,
                        BigDecimal.valueOf(100),
                        "Fee",
                        "REF1",
                        Instant.now()
                );

        assertThat(res).isNotNull();
        verify(ledgerService).debit(any(), any(), any(), any(),any());
    }
    
    @Test
    void shouldReturnExistingTransaction_whenIdempotent() {
        Transaction txn = new Transaction();

        TransactionRequest req = mock(TransactionRequest.class);
        when(req.getAmount()).thenReturn(BigDecimal.TEN);
        when(req.getTransactionReference()).thenReturn("REF");
        when(req.getTransactionType()).thenReturn(TransactionType.PURCHASE);

        when(repository.findByNetworkReference("REF"))
                .thenReturn(Optional.of(txn));

        when(mapper.toSummaryResponse(txn))
                .thenReturn(mock(TransactionSummaryResponse.class));

        TransactionSummaryResponse res =
                service.postTransaction(userId, cardId, req);

        assertThat(res).isNotNull();
    }
    @Test
    void shouldThrow_whenReferenceMissing() {
        TransactionRequest req = mock(TransactionRequest.class);

        when(req.getAmount()).thenReturn(BigDecimal.TEN);
        when(req.getTransactionReference()).thenReturn(null);

        assertThatThrownBy(() ->
                service.postTransaction(userId, cardId, req))
                .isInstanceOf(BadRequestException.class);
    }
    @Test
    void shouldThrow_whenPaymentTypeUsed() {
        TransactionRequest req = mock(TransactionRequest.class);

        when(req.getAmount()).thenReturn(BigDecimal.TEN);
        when(req.getTransactionReference()).thenReturn("REF");
        when(req.getTransactionType()).thenReturn(TransactionType.PAYMENT);

        assertThatThrownBy(() ->
                service.postTransaction(userId, cardId, req))
                .isInstanceOf(BadRequestException.class);
    }
    @Test
    void shouldDecline_whenCardExpired() {
        card.setExpiresAt(Instant.now().minusSeconds(10));

        TransactionRequest req = baseRequest();

        when(repository.findByNetworkReference(any())).thenReturn(Optional.empty());
        when(customerService.getCustomerByUserId(userId)).thenReturn(customer);
        when(cardService.getCardEntity(cardId)).thenReturn(card);
        when(mapper.toSummaryResponse(any())).thenReturn(mock(TransactionSummaryResponse.class));

        TransactionSummaryResponse res =
                service.postTransaction(userId, cardId, req);

        assertThat(res).isNotNull();
    }
    
    @Test
    void shouldDecline_whenLimitExceeded() {
        TransactionRequest req = baseRequest();

        when(repository.findByNetworkReference(any())).thenReturn(Optional.empty());
        when(customerService.getCustomerByUserId(userId)).thenReturn(customer);
        when(cardService.getCardEntity(cardId)).thenReturn(card);

        when(ledgerService.getBalance(accountId))
                .thenReturn(BigDecimal.valueOf(1000));

        account.setCreditLimit(BigDecimal.valueOf(500));

        when(mapper.toSummaryResponse(any())).thenReturn(mock(TransactionSummaryResponse.class));

        TransactionSummaryResponse res =
                service.postTransaction(userId, cardId, req);

        assertThat(res).isNotNull();
    }
    @Test
    void shouldDecline_whenDailyLimitExceeded() {
        TransactionRequest req = baseRequest();

        card.getCardProduct().setEcommerceDailyLimit(BigDecimal.valueOf(100));

        when(repository.findByNetworkReference(any())).thenReturn(Optional.empty());
        when(customerService.getCustomerByUserId(userId)).thenReturn(customer);
        when(cardService.getCardEntity(cardId)).thenReturn(card);

        when(ledgerService.getBalance(accountId)).thenReturn(BigDecimal.ZERO);
        when(repository.sumApprovedAmountByCardAndTypeAndChannelAfter(any(), any(), any(), any()))
                .thenReturn(BigDecimal.valueOf(200));

        when(mapper.toSummaryResponse(any())).thenReturn(mock(TransactionSummaryResponse.class));

        TransactionSummaryResponse res =
                service.postTransaction(userId, cardId, req);

        assertThat(res).isNotNull();
    }
    
    @Test
    void shouldHandleAuthorizationFailure() {
        TransactionRequest req = baseRequest();

        when(repository.findByNetworkReference(any())).thenReturn(Optional.empty());
        when(customerService.getCustomerByUserId(userId)).thenReturn(customer);
        when(cardService.getCardEntity(cardId)).thenReturn(card);

        when(ledgerService.getBalance(accountId)).thenReturn(BigDecimal.ZERO);

        Authorization auth = mock(Authorization.class);
        when(auth.getId()).thenReturn(UUID.randomUUID());

        when(authorizationService.authorize(any(), any(), any(), any())).thenReturn(auth);

        when(repository.save(any())).thenThrow(new RuntimeException("fail"));

        assertThatThrownBy(() ->
                service.postTransaction(userId, cardId, req))
                .isInstanceOf(RuntimeException.class);

        verify(authorizationService).expire(any());
    }
    @Test
    void shouldThrow_whenOnlineDisabledAtProduct() {
        card.getCardProduct().setOnlineTransactionsAllowed(false);

        TransactionRequest req = baseRequest();

        when(repository.findByNetworkReference(any())).thenReturn(Optional.empty());
        when(customerService.getCustomerByUserId(userId)).thenReturn(customer);
        when(cardService.getCardEntity(cardId)).thenReturn(card);

        assertThatThrownBy(() ->
                service.postTransaction(userId, cardId, req))
                .isInstanceOf(BadRequestException.class);
    }
    @Test
    void shouldThrow_whenInvalidSize() {
        when(customerService.getCustomerByUserId(userId)).thenReturn(customer);
        when(accountService.getAccountEntity(accountId)).thenReturn(account);

        assertThatThrownBy(() ->
                service.getAccountTransactions(userId, accountId, null, null, null, 0, 200))
                .isInstanceOf(BadRequestException.class);
    }
    @Test
    void shouldThrow_whenReferenceNotFound() {
        when(repository.findByNetworkReference("REF"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.getByTransactionReference("REF"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
    @Test
    void shouldReplaceExistingSystemTransaction() {

        Transaction existingTxn = new Transaction();
        existingTxn.setTransactionId(UUID.randomUUID());

        when(repository.findByInternalReference("REF"))
                .thenReturn(Optional.of(existingTxn));

        when(repository.save(any()))
                .thenAnswer(i -> i.getArgument(0));

        when(mapper.toSummaryResponse(any()))
                .thenReturn(mock(TransactionSummaryResponse.class));

        doNothing().when(ledgerService).deleteByReferenceId(any());

        TransactionSummaryResponse res =
                service.postSystemTransaction(
                        account,
                        TransactionType.FEE,
                        BigDecimal.TEN,
                        "fee",
                        "REF",
                        Instant.now()
                );

        assertThat(res).isNotNull();

        verify(ledgerService).deleteByReferenceId(existingTxn.getTransactionId());
        verify(repository).delete(existingTxn);
        verify(repository).save(any()); // new txn
    }
    @Test
    void shouldCredit_whenRefundSystemTxn() {
        when(repository.findByInternalReference(any())).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(mapper.toSummaryResponse(any())).thenReturn(mock(TransactionSummaryResponse.class));

        service.postSystemTransaction(account, TransactionType.REFUND, BigDecimal.TEN, "refund", "REF",Instant.now());

        verify(ledgerService).credit(any(), any(), any(), any(),any());
    }
    
    private TransactionRequest baseRequest() {
        TransactionRequest req = new TransactionRequest();

        req.setAmount(BigDecimal.valueOf(100));                // valid > 0
        req.setTransactionReference("REF_" + UUID.randomUUID()); // unique ref
        req.setTransactionType(TransactionType.PURCHASE);      // valid (not PAYMENT)
        req.setTransactionChannel(TransactionChannel.ONLINE);  // default safe channel

        req.setMerchantName("Amazon");
        req.setMerchantCategoryCode("5411");
        req.setMerchantCategoryName("GROCERY");

        return req;
    }
}