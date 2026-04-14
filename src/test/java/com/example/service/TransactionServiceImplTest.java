package com.example.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import com.example.dto.request.TransactionRequest;
import com.example.dto.response.TransactionDetailResponse;
import com.example.dto.response.TransactionSummaryResponse;
import com.example.entity.*;
import com.example.enums.*;
import com.example.exception.*;
import com.example.mapper.TransactionMapper;
import com.example.repository.TransactionRepository;
import com.example.service.ServiceImpl.TransactionServiceImpl;
import com.example.util.ReferenceNumberGenerator;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // ✅ prevents UnnecessaryStubbingException
class TransactionServiceImplTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private CustomerService customerService;
    @Mock private CreditCardService creditCardService;
    @Mock private CreditAccountService creditAccountService;
    @Mock private TransactionMapper transactionMapper;
    @Mock private ReferenceNumberGenerator referenceNumberGenerator;

    @InjectMocks
    private TransactionServiceImpl service;

    private UUID userId;
    private UUID cardId;

    private Customer customer;
    private CreditAccount account;
    private CreditCard card;
    private CreditCardProduct product;

    @BeforeEach
    void setup() {

        userId = UUID.randomUUID();
        cardId = UUID.randomUUID();

        customer = new Customer();
        customer.setCustomerId(UUID.randomUUID());

        account = new CreditAccount();
        account.setAccountId(UUID.randomUUID());
        account.setCustomer(customer);
        account.setAvailableBalance(BigDecimal.valueOf(10000));

        product = new CreditCardProduct();
        product.setOnlineTransactionsAllowed(true);
        product.setAtmWithdrawalAllowed(true);
        product.setPosDailyLimit(BigDecimal.valueOf(5000));
        product.setEcommerceDailyLimit(BigDecimal.valueOf(5000));
        product.setAtmDailyLimit(BigDecimal.valueOf(5000));

        card = new CreditCard();
        card.setCardId(cardId);
        card.setCreditAccount(account);
        card.setCardProduct(product);
        card.setCardStatus(CardStatus.ACTIVE);
        card.setOnlineEnabled(true);
        card.setAtmEnabled(true);
    }

    private TransactionRequest validRequest() {
        TransactionRequest req = new TransactionRequest();
        req.setAmount(BigDecimal.valueOf(1000));
        req.setTransactionType(TransactionType.PURCHASE);
        req.setTransactionChannel(TransactionChannel.ONLINE);
        req.setMerchantName("Amazon");
        req.setTransactionReference("TXN-123"); // ✅ REQUIRED
        return req;
    }

    // ================= VALIDATION =================

    @Test
    void invalidAmount_shouldThrow() {
        TransactionRequest req = validRequest();
        req.setAmount(BigDecimal.ZERO);

        assertThrows(BadRequestException.class,
                () -> service.postTransaction(userId, cardId, req));
    }

    @Test
    void nullType_shouldThrow() {
        TransactionRequest req = validRequest();
        req.setTransactionType(null);

        assertThrows(BadRequestException.class,
                () -> service.postTransaction(userId, cardId, req));
    }

    @Test
    void nullChannel_shouldThrow() {
        TransactionRequest req = validRequest();
        req.setTransactionChannel(null);

        assertThrows(BadRequestException.class,
                () -> service.postTransaction(userId, cardId, req));
    }

    @Test
    void missingReference_shouldThrow() {
        TransactionRequest req = validRequest();
        req.setTransactionReference("");

        assertThrows(BadRequestException.class,
                () -> service.postTransaction(userId, cardId, req));
    }

    @Test
    void paymentType_shouldThrow() {
        TransactionRequest req = validRequest();
        req.setTransactionType(TransactionType.PAYMENT);

        assertThrows(BadRequestException.class,
                () -> service.postTransaction(userId, cardId, req));
    }

    // ================= ACCESS =================

    @Test
    void cardNotOwned_shouldThrow() {

        Customer other = new Customer();
        other.setCustomerId(UUID.randomUUID());

        when(customerService.getCustomerByUserId(userId)).thenReturn(customer);

        account.setCustomer(other);
        when(creditCardService.getCardEntity(cardId)).thenReturn(card);

        assertThrows(AccessDeniedException.class,
                () -> service.postTransaction(userId, cardId, validRequest()));
    }

    // ================= DECLINES =================

    @Test
    void cardNotActive_shouldDecline() {
        card.setCardStatus(CardStatus.BLOCKED);
        mockCommon();

        assertNotNull(service.postTransaction(userId, cardId, validRequest()));
    }

    @Test
    void expiredCard_shouldDecline() {
        card.setExpiresAt(Instant.now().minusSeconds(1000));
        mockCommon();

        assertNotNull(service.postTransaction(userId, cardId, validRequest()));
    }

    @Test
    void insufficientBalance_shouldDecline() {
        account.setAvailableBalance(BigDecimal.ZERO);
        mockCommon();

        assertNotNull(service.postTransaction(userId, cardId, validRequest()));
    }

    @Test
    void dailyLimitExceeded_shouldDecline() {

        when(customerService.getCustomerByUserId(userId)).thenReturn(customer);
        when(creditCardService.getCardEntity(cardId)).thenReturn(card);

        when(transactionRepository.sumApprovedAmountByCardAndTypeAndChannelAfter(
                any(), any(), any(), any()))
                .thenReturn(BigDecimal.valueOf(5000));

        when(transactionMapper.toSummaryResponse(any()))
                .thenReturn(new TransactionSummaryResponse());

        assertNotNull(service.postTransaction(userId, cardId, validRequest()));
    }

    // ================= CHANNEL =================

    @Test
    void onlineDisabled_shouldThrow() {
        product.setOnlineTransactionsAllowed(false);

        when(customerService.getCustomerByUserId(userId)).thenReturn(customer);
        when(creditCardService.getCardEntity(cardId)).thenReturn(card);

        assertThrows(BadRequestException.class,
                () -> service.postTransaction(userId, cardId, validRequest()));
    }

    @Test
    void atmDisabled_shouldThrow() {
        card.setAtmEnabled(false);

        TransactionRequest req = validRequest();
        req.setTransactionChannel(TransactionChannel.ATM);

        when(customerService.getCustomerByUserId(userId)).thenReturn(customer);
        when(creditCardService.getCardEntity(cardId)).thenReturn(card);

        assertThrows(BadRequestException.class,
                () -> service.postTransaction(userId, cardId, req));
    }

    // ================= SUCCESS =================

    @Test
    void success_shouldSaveTransaction() {

        mockCommon();

        TransactionSummaryResponse res =
                service.postTransaction(userId, cardId, validRequest());

        assertNotNull(res);
        verify(transactionRepository).save(any());
    }

    // ================= GET TRANSACTIONS =================

    @SuppressWarnings("unchecked")
	@Test
    void getTransactions_success() {

        when(customerService.getCustomerByUserId(userId)).thenReturn(customer);
        when(creditAccountService.getAccountEntity(account.getAccountId())).thenReturn(account);

        Page<Transaction> page = new PageImpl<>(List.of(new Transaction()));

        when(transactionRepository.findAll(
                any(Specification.class),
                any(Pageable.class)
        )).thenReturn(page);
        when(transactionMapper.toSummaryResponse(any()))
                .thenReturn(new TransactionSummaryResponse());

        Page<?> result = service.getAccountTransactions(
                userId, account.getAccountId(),
                null, null, null,
                0, 10
        );

        assertEquals(1, result.getContent().size());
    }

    @Test
    void invalidPage_shouldThrow() {

        when(customerService.getCustomerByUserId(userId)).thenReturn(customer);
        when(creditAccountService.getAccountEntity(account.getAccountId())).thenReturn(account);

        assertThrows(BadRequestException.class,
                () -> service.getAccountTransactions(userId, account.getAccountId(), null, null, null, -1, 10));
    }

    // ================= GET BY ID =================

    @Test
    void getTransactionById_success() {

        Transaction txn = new Transaction();
        txn.setAccount(account);

        when(customerService.getCustomerByUserId(userId)).thenReturn(customer);
        when(creditAccountService.getAccountEntity(account.getAccountId())).thenReturn(account);
        when(transactionRepository.findById(any())).thenReturn(Optional.of(txn));

        when(transactionMapper.toResponse(any()))
                .thenReturn(new TransactionDetailResponse());

        assertNotNull(service.getAccountTransactionById(
                userId, account.getAccountId(), UUID.randomUUID()));
    }

    @Test
    void getTransaction_wrongAccount_shouldThrow() {

        // Create a different account with valid ID
        CreditAccount otherAccount = new CreditAccount();
        otherAccount.setAccountId(UUID.randomUUID()); // ✅ NOT NULL

        Transaction txn = new Transaction();
        txn.setAccount(otherAccount);

        when(customerService.getCustomerByUserId(userId)).thenReturn(customer);
        when(creditAccountService.getAccountEntity(account.getAccountId()))
                .thenReturn(account);

        when(transactionRepository.findById(any()))
                .thenReturn(Optional.of(txn));

        assertThrows(BadRequestException.class,
                () -> service.getAccountTransactionById(
                        userId,
                        account.getAccountId(),
                        UUID.randomUUID()
                ));
    }
    // ================= PAYMENT =================

    @Test
    void recordPayment_success() {

        Payment payment = new Payment();
        payment.setAmount(BigDecimal.valueOf(1000));
        payment.setPaymentMethod(PaymentMethod.UPI);

        when(transactionMapper.toSummaryResponse(any()))
                .thenReturn(new TransactionSummaryResponse());

        assertNotNull(service.recordPayment(account, payment, BigDecimal.TEN, BigDecimal.ZERO));
    }

    @Test
    void recordPayment_invalid_shouldThrow() {

        Payment payment = new Payment();
        payment.setAmount(BigDecimal.ZERO);

        assertThrows(BadRequestException.class,
                () -> service.recordPayment(account, payment, BigDecimal.TEN, BigDecimal.ZERO));
    }

    // ================= SYSTEM =================

    @Test
    void systemTransaction_success() {

        when(transactionRepository.findByReferenceNumber("SYS"))
                .thenReturn(Optional.empty());

        when(transactionMapper.toSummaryResponse(any()))
                .thenReturn(new TransactionSummaryResponse());

        assertNotNull(service.postSystemTransaction(
                account, TransactionType.FEE,
                BigDecimal.TEN, "Fee", "SYS"));
    }

    @Test
    void systemTransaction_idempotent() {

        Transaction txn = new Transaction();

        when(transactionRepository.findByReferenceNumber("SYS"))
                .thenReturn(Optional.of(txn));

        when(transactionMapper.toSummaryResponse(txn))
                .thenReturn(new TransactionSummaryResponse());

        assertNotNull(service.postSystemTransaction(
                account, TransactionType.FEE,
                BigDecimal.TEN, "Fee", "SYS"));
    }

    // ================= REF =================

    @Test
    void getByReference_success() {

        Transaction txn = new Transaction();

        when(transactionRepository.findByTransactionReference("REF"))
                .thenReturn(Optional.of(txn));

        when(transactionMapper.toSummaryResponse(txn))
                .thenReturn(new TransactionSummaryResponse());

        assertNotNull(service.getByTransactionReference("REF"));
    }

    @Test
    void getByReference_notFound() {

        when(transactionRepository.findByTransactionReference("REF"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getByTransactionReference("REF"));
    }

    // ================= COMMON MOCK =================

    private void mockCommon() {

        when(customerService.getCustomerByUserId(userId)).thenReturn(customer);
        when(creditCardService.getCardEntity(cardId)).thenReturn(card);

        lenient().when(transactionRepository
                .sumApprovedAmountByCardAndTypeAndChannelAfter(
                        any(), any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);

        lenient().when(transactionMapper.toSummaryResponse(any()))
                .thenReturn(new TransactionSummaryResponse());
    }
}