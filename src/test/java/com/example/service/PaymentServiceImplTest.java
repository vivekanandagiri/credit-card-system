package com.example.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.*;

import com.example.testutil.TestFixtures;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import org.springframework.data.domain.*;

import com.example.dto.request.PaymentRequest;
import com.example.dto.response.PaymentResponse;
import com.example.entity.*;
import com.example.enums.*;
import com.example.exception.*;
import com.example.mapper.PaymentMapper;
import com.example.repository.*;
import com.example.service.ServiceImpl.PaymentServiceImpl;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock private BillingStatementService billingService;
    @Mock private CreditAccountService accountService;
    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentAllocationRepository allocationRepository;
    @Mock private TransactionService transactionService;
    @Mock private PaymentMapper mapper;

    @InjectMocks
    private PaymentServiceImpl service;

    private UUID accountId;
    private CreditAccount account;

    @BeforeEach
    void setUp() {
        accountId = UUID.randomUUID();

        account = TestFixtures.validCreditAccount(
                TestFixtures.validCustomer(),
                null,
                TestFixtures.validCreditProductEntity()
        );
        account.setAccountId(accountId);
    }

    // ================= MAKE PAYMENT =================

    @Test
    void shouldProcessPayment_success_singleStatement() {
        PaymentRequest req = new PaymentRequest();
        req.setAmount(BigDecimal.valueOf(1000));
        req.setPaymentMethod(PaymentMethod.UPI);
        req.setReferenceId("REF1");

        BillingStatement stmt = new BillingStatement();
        stmt.setRemainingAmount(BigDecimal.valueOf(1000));
        stmt.setAmountPaid(BigDecimal.ZERO);
        stmt.setStatementStatus(StatementStatus.GENERATED);

        when(accountService.getAccountForUpdate(accountId)).thenReturn(account);
        when(paymentRepository.findByReferenceId("REF1")).thenReturn(Optional.empty());
        when(billingService.getUnpaidStatementsOldestFirst(accountId))
                .thenReturn(List.of(stmt));

        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(mapper.toResponse(any())).thenReturn(mock(PaymentResponse.class));

        PaymentResponse res = service.makePayment(accountId, req);

        assertThat(res).isNotNull();

        verify(transactionService).recordPayment(any(), any());
        verify(allocationRepository).save(any());
        verify(accountService).applyPayment(any(), any(), any());
    }

    @Test
    void shouldReturnExistingPayment_idempotency() {
        Payment existing = new Payment();

        PaymentRequest req = new PaymentRequest();
        req.setAmount(BigDecimal.valueOf(100));
        req.setPaymentMethod(PaymentMethod.UPI);
        req.setReferenceId("REF1");

        when(paymentRepository.findByReferenceId("REF1"))
                .thenReturn(Optional.of(existing));

        when(mapper.toResponse(existing))
                .thenReturn(mock(PaymentResponse.class));

        PaymentResponse res = service.makePayment(accountId, req);

        assertThat(res).isNotNull();
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void shouldAllocateAcrossMultipleStatements() {
        PaymentRequest req = new PaymentRequest();
        req.setAmount(BigDecimal.valueOf(1500));
        req.setPaymentMethod(PaymentMethod.UPI);
        req.setReferenceId("REF2");

        BillingStatement s1 = new BillingStatement();
        s1.setRemainingAmount(BigDecimal.valueOf(1000));
        s1.setAmountPaid(BigDecimal.ZERO);

        BillingStatement s2 = new BillingStatement();
        s2.setRemainingAmount(BigDecimal.valueOf(1000));
        s2.setAmountPaid(BigDecimal.ZERO);

        when(accountService.getAccountForUpdate(accountId)).thenReturn(account);
        when(paymentRepository.findByReferenceId(any())).thenReturn(Optional.empty());
        when(billingService.getUnpaidStatementsOldestFirst(accountId))
                .thenReturn(List.of(s1, s2));

        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(mapper.toResponse(any())).thenReturn(mock(PaymentResponse.class));

        service.makePayment(accountId, req);

        assertThat(s1.getRemainingAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(s2.getRemainingAmount()).isEqualByComparingTo(BigDecimal.valueOf(500));
    }

    @Test
    void shouldMarkStatementPaid_whenFullyCleared() {
        PaymentRequest req = new PaymentRequest();
        req.setAmount(BigDecimal.valueOf(1000));
        req.setPaymentMethod(PaymentMethod.UPI);
        req.setReferenceId("REF3");

        BillingStatement stmt = new BillingStatement();
        stmt.setRemainingAmount(BigDecimal.valueOf(1000));
        stmt.setAmountPaid(BigDecimal.ZERO);
        stmt.setStatementStatus(StatementStatus.GENERATED);

        when(accountService.getAccountForUpdate(accountId)).thenReturn(account);
        when(paymentRepository.findByReferenceId(any())).thenReturn(Optional.empty());
        when(billingService.getUnpaidStatementsOldestFirst(accountId))
                .thenReturn(List.of(stmt));

        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(mapper.toResponse(any())).thenReturn(mock(PaymentResponse.class));

        service.makePayment(accountId, req);

        assertThat(stmt.getStatementStatus()).isEqualTo(StatementStatus.PAID);
    }

    @Test
    void shouldHandleOverpayment() {
        PaymentRequest req = new PaymentRequest();
        req.setAmount(BigDecimal.valueOf(2000));
        req.setPaymentMethod(PaymentMethod.UPI);
        req.setReferenceId("REF4");

        BillingStatement stmt = new BillingStatement();
        stmt.setRemainingAmount(BigDecimal.valueOf(500));

        when(accountService.getAccountForUpdate(accountId)).thenReturn(account);
        when(paymentRepository.findByReferenceId(any())).thenReturn(Optional.empty());
        when(billingService.getUnpaidStatementsOldestFirst(accountId))
                .thenReturn(List.of(stmt));

        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(mapper.toResponse(any())).thenReturn(mock(PaymentResponse.class));

        service.makePayment(accountId, req);

        verify(accountService).applyPayment(eq(accountId), eq(BigDecimal.valueOf(2000)), any());
    }

    // ================= VALIDATION =================

    @Test
    void shouldThrow_whenInvalidAmount() {
        PaymentRequest req = new PaymentRequest();
        req.setAmount(BigDecimal.ZERO);

        assertThatThrownBy(() -> service.makePayment(accountId, req))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void shouldThrow_whenMissingMethod() {
        PaymentRequest req = new PaymentRequest();
        req.setAmount(BigDecimal.valueOf(100));

        assertThatThrownBy(() -> service.makePayment(accountId, req))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void shouldThrow_whenMissingReference() {
        PaymentRequest req = new PaymentRequest();
        req.setAmount(BigDecimal.valueOf(100));
        req.setPaymentMethod(PaymentMethod.UPI);

        assertThatThrownBy(() -> service.makePayment(accountId, req))
                .isInstanceOf(BadRequestException.class);
    }

    // ================= GET PAYMENTS =================

    @Test
    void shouldGetPayments_success() {
        Payment p = new Payment();
        p.setPaymentId(UUID.randomUUID());

        Page<Payment> page = new PageImpl<>(List.of(p));

        when(paymentRepository.findByAccount_AccountId(eq(accountId), any()))
                .thenReturn(page);

        when(paymentRepository.findAllWithAllocationsByIds(any()))
                .thenReturn(List.of(p));

        when(mapper.toResponse(any()))
                .thenReturn(mock(PaymentResponse.class));

        Page<PaymentResponse> res = service.getPayments(accountId, 0, 10);

        assertThat(res.getContent()).hasSize(1);
    }

    @Test
    void shouldReturnEmptyPage_whenNoPayments() {
        when(paymentRepository.findByAccount_AccountId(eq(accountId), any()))
                .thenReturn(Page.empty());

        Page<PaymentResponse> res = service.getPayments(accountId, 0, 10);

        assertThat(res.getContent()).isEmpty();
    }

    // ================= GET BY ID =================

    @Test
    void shouldGetPaymentById_success() {
        Payment payment = new Payment();

        when(paymentRepository.findDetailedByPaymentIdAndAccountId(any(), any()))
                .thenReturn(Optional.of(payment));

        when(mapper.toResponse(any()))
                .thenReturn(mock(PaymentResponse.class));

        PaymentResponse res = service.getPaymentById(accountId, UUID.randomUUID());

        assertThat(res).isNotNull();
    }

    @Test
    void shouldThrow_whenPaymentNotFound() {
        when(paymentRepository.findDetailedByPaymentIdAndAccountId(any(), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.getPaymentById(accountId, UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ================= GET BY REFERENCE =================

    @Test
    void shouldGetByReference_success() {
        Payment payment = new Payment();

        when(paymentRepository.findByReferenceId("REFX"))
                .thenReturn(Optional.of(payment));

        when(mapper.toResponse(any()))
                .thenReturn(mock(PaymentResponse.class));

        PaymentResponse res = service.getByReferenceId("REFX");

        assertThat(res).isNotNull();
    }

    @Test
    void shouldThrow_whenReferenceNotFound() {
        when(paymentRepository.findByReferenceId("REFX"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.getByReferenceId("REFX"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
    @Test
    void shouldSkipStatement_whenRemainingIsZero() {
        PaymentRequest req = new PaymentRequest();
        req.setAmount(BigDecimal.valueOf(500));
        req.setPaymentMethod(PaymentMethod.UPI);
        req.setReferenceId("REF_SKIP");

        BillingStatement stmt = new BillingStatement();
        stmt.setRemainingAmount(BigDecimal.ZERO); // 🔥 triggers skip
        stmt.setAmountPaid(BigDecimal.ZERO);

        when(accountService.getAccountForUpdate(accountId)).thenReturn(account);
        when(paymentRepository.findByReferenceId(any())).thenReturn(Optional.empty());
        when(billingService.getUnpaidStatementsOldestFirst(accountId))
                .thenReturn(List.of(stmt));

        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(mapper.toResponse(any())).thenReturn(mock(PaymentResponse.class));

        service.makePayment(accountId, req);

        // ❗ allocation should NOT happen
        verify(allocationRepository, never()).save(any());
    }
    @Test
    void shouldHandleNullRemainingAmount_usingDefaultZero() {
        PaymentRequest req = new PaymentRequest();
        req.setAmount(BigDecimal.valueOf(500));
        req.setPaymentMethod(PaymentMethod.UPI);
        req.setReferenceId("REF_NULL");

        BillingStatement stmt = new BillingStatement();
        stmt.setRemainingAmount(null); // 🔥 triggers defaultZero
        stmt.setAmountPaid(null);

        when(accountService.getAccountForUpdate(accountId)).thenReturn(account);
        when(paymentRepository.findByReferenceId(any())).thenReturn(Optional.empty());
        when(billingService.getUnpaidStatementsOldestFirst(accountId))
                .thenReturn(List.of(stmt));

        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(mapper.toResponse(any())).thenReturn(mock(PaymentResponse.class));

        service.makePayment(accountId, req);

        // ❗ should not crash and should skip allocation
        verify(allocationRepository, never()).save(any());
    }
    @Test
    void shouldNotMarkPaid_whenStatementIsOverdue() {
        PaymentRequest req = new PaymentRequest();
        req.setAmount(BigDecimal.valueOf(1000));
        req.setPaymentMethod(PaymentMethod.UPI);
        req.setReferenceId("REF_OVERDUE");

        BillingStatement stmt = new BillingStatement();
        stmt.setRemainingAmount(BigDecimal.valueOf(1000));
        stmt.setAmountPaid(BigDecimal.ZERO);
        stmt.setStatementStatus(StatementStatus.OVERDUE); // 🔥 critical

        when(accountService.getAccountForUpdate(accountId)).thenReturn(account);
        when(paymentRepository.findByReferenceId(any())).thenReturn(Optional.empty());
        when(billingService.getUnpaidStatementsOldestFirst(accountId))
                .thenReturn(List.of(stmt));

        when(paymentRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(mapper.toResponse(any())).thenReturn(mock(PaymentResponse.class));

        service.makePayment(accountId, req);

        // ❗ should remain OVERDUE
        assertThat(stmt.getStatementStatus()).isEqualTo(StatementStatus.OVERDUE);
    }
}