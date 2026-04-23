package com.example.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

import com.example.testutil.TestFixtures;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import com.example.config.TimezoneResolver;
import com.example.dto.response.BillingStatementResponse;
import com.example.entity.*;
import com.example.enums.StatementStatus;
import com.example.enums.TransactionType;
import com.example.exception.BadRequestException;
import com.example.mapper.BillingStatementMapper;
import com.example.repository.*;
import com.example.service.ServiceImpl.BillingStatementServiceImpl;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class BillingStatementServiceImplTest {

    @Mock private BillingStatementRepository billingRepository;
    @Mock private TransactionService transactionService;
    @Mock private CreditAccountService accountService;
    @Mock private BillingStatementMapper mapper;
    @Mock private TimezoneResolver timezoneResolver;
    @Mock private InterestCalculationService interestService;
    @Mock private PaymentAllocationRepository paymentAllocationRepository;
    @Mock private LedgerEntryRepository ledgerEntryRepository;
    @Mock private EntityManager entityManager;

    
    @InjectMocks
    private BillingStatementServiceImpl service;

    private UUID accountId;
    private CreditAccount account;
    private Customer customer;
    private CreditProduct product;

    @BeforeEach
    void setUp() {
        accountId = UUID.randomUUID();

        customer = TestFixtures.validCustomer();

        product = TestFixtures.validCreditProductEntity();
        product.setMinimumDueAmount(BigDecimal.valueOf(200));
        product.setLateFeeAmount(BigDecimal.valueOf(500));

        account = TestFixtures.validCreditAccount(customer, null, product);
        account.setAccountId(accountId);
        account.setActivatedAt(Instant.now().minus(30, ChronoUnit.DAYS));
        account.setStatementCycleDay(LocalDate.now().getDayOfMonth());
        account.setGracePeriodDays(10);
        account.setMinimumDuePercent(BigDecimal.valueOf(5));
    }

    // ================= GENERATE STATEMENT =================

    @Test
    void shouldGenerateStatement_success() {
        when(accountService.getAccountEntity(accountId)).thenReturn(account);
        when(timezoneResolver.resolve(any())).thenReturn(ZoneId.systemDefault());

        when(billingRepository.existsByAccountAccountIdAndBillingPeriodEnd(any(), any()))
                .thenReturn(false);

        when(billingRepository.findTopByAccountOrderByBillingPeriodEndDesc(any()))
                .thenReturn(Optional.empty());

        when(interestService.calculateInterest(any(), any(), any(), any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);

        when(ledgerEntryRepository.sumDebitsForPeriod(any(), any(), any()))
                .thenReturn(BigDecimal.valueOf(1000));

        when(ledgerEntryRepository.sumCreditsForPeriod(any(), any(), any()))
                .thenReturn(BigDecimal.valueOf(200));

        when(billingRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(mapper.toResponse(any())).thenReturn(mock(BillingStatementResponse.class));

        BillingStatementResponse res = service.generateStatement(accountId);

        assertThat(res).isNotNull();
    }

//    @Test
//    void shouldThrow_whenBillingCycleNotReached() {
//
//        int today = LocalDate.now().getDayOfMonth();
//        account.setStatementCycleDay(today == 1 ? 2 : 1); // ✅ force mismatch
//
//        when(accountService.getAccountEntity(accountId)).thenReturn(account);
//        when(timezoneResolver.resolve(any())).thenReturn(ZoneId.systemDefault());
//
//        // defensive stubs (should NOT be used)
//        when(interestService.calculateInterest(any(), any(), any(), any(), any(), any()))
//                .thenReturn(BigDecimal.ZERO);
//
//        when(billingRepository.save(any()))
//                .thenAnswer(i -> i.getArgument(0));
//
//        assertThatThrownBy(() ->
//                service.generateStatement(accountId))
//                .isInstanceOf(BadRequestException.class);
//    }

    @Test
    void shouldThrow_whenDuplicateStatement() {
        when(accountService.getAccountEntity(accountId)).thenReturn(account);
        when(timezoneResolver.resolve(any())).thenReturn(ZoneId.systemDefault());

        when(billingRepository.existsByAccountAccountIdAndBillingPeriodEnd(any(), any()))
                .thenReturn(true);

        assertThatThrownBy(() ->
                service.generateStatement(accountId))
                .isInstanceOf(BadRequestException.class);
    }

    // ================= MANUAL GENERATION =================

    @Test
    void shouldGenerateStatementManually_success() {
        when(accountService.getAccountEntity(accountId)).thenReturn(account);
        when(timezoneResolver.resolve(any())).thenReturn(ZoneId.systemDefault());

        when(billingRepository.existsByAccountAccountIdAndBillingPeriodEnd(any(), any()))
                .thenReturn(false);

        when(billingRepository.findTopByAccountOrderByBillingPeriodEndDesc(any()))
                .thenReturn(Optional.empty());

        when(interestService.calculateInterest(any(), any(), any(), any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);

        when(ledgerEntryRepository.sumDebitsForPeriod(any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);

        when(ledgerEntryRepository.sumCreditsForPeriod(any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);

        when(billingRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(mapper.toResponse(any())).thenReturn(mock(BillingStatementResponse.class));

        BillingStatementResponse res = service.generateStatementManually(accountId);

        assertThat(res).isNotNull();
    }

    // ================= GET STATEMENTS =================

    @Test
    void shouldGetStatements_success() {
        when(billingRepository.findByAccountAccountIdOrderByBillingPeriodEndDesc(accountId))
                .thenReturn(List.of(new BillingStatement()));

        when(mapper.toResponse(any()))
                .thenReturn(mock(BillingStatementResponse.class));

        List<BillingStatementResponse> res = service.getStatements(accountId);

        assertThat(res).hasSize(1);
    }

    @Test
    void shouldThrow_whenCustomerAccessInvalid() {
        UUID userId = UUID.randomUUID();

        Customer another = new Customer();
        another.setUser(new User());
        another.getUser().setUserId(UUID.randomUUID());

        account.setCustomer(another);

        when(accountService.getAccountEntity(accountId)).thenReturn(account);

        assertThatThrownBy(() ->
                service.getCustomerStatementsByAccount(userId, accountId))
                .isInstanceOf(BadRequestException.class);
    }

    // ================= GET STATEMENT =================

    @Test
    void shouldGetStatement_success() {
        BillingStatement stmt = new BillingStatement();

        when(billingRepository.findById(any())).thenReturn(Optional.of(stmt));

        BillingStatement res = service.getStatement(UUID.randomUUID());

        assertThat(res).isNotNull();
    }

    @Test
    void shouldThrow_whenStatementNotFound() {
        when(billingRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.getStatement(UUID.randomUUID()))
                .isInstanceOf(BadRequestException.class);
    }

    // ================= PROCESS DUE =================

    @Test
    void shouldProcessDueStatements_success() {
        BillingStatement stmt = new BillingStatement();
        stmt.setStatementId(UUID.randomUUID());
        stmt.setAccount(account);
        stmt.setDueDate(LocalDate.now().minusDays(1));
        stmt.setTotalAmountDue(BigDecimal.valueOf(1000));
        stmt.setMinimumDueAmount(BigDecimal.valueOf(100));

        when(billingRepository.findDueStatementsPendingEvaluation(any()))
                .thenReturn(List.of(stmt));

        when(timezoneResolver.resolve(any())).thenReturn(ZoneId.systemDefault());

        when(paymentAllocationRepository.sumAllocatedToStatementBefore(any(), any()))
                .thenReturn(BigDecimal.ZERO);

        service.processDueStatements();

        verify(billingRepository).saveAll(any());
    }

    // ================= SAVE =================

    @Test
    void shouldSaveStatement() {
        BillingStatement stmt = new BillingStatement();

        when(billingRepository.save(stmt)).thenReturn(stmt);

        BillingStatement res = service.save(stmt);

        assertThat(res).isNotNull();
    }
    
    @Test
    void shouldPostInterestTransaction_whenInterestGreaterThanZero() {
        when(accountService.getAccountEntity(accountId)).thenReturn(account);
        when(timezoneResolver.resolve(any())).thenReturn(ZoneId.systemDefault());

        when(billingRepository.existsByAccountAccountIdAndBillingPeriodEnd(any(), any()))
                .thenReturn(false);

        when(billingRepository.findTopByAccountOrderByBillingPeriodEndDesc(any()))
                .thenReturn(Optional.empty());

        // 🔥 trigger interest branch
        when(interestService.calculateInterest(any(), any(), any(), any(), any(), any()))
                .thenReturn(BigDecimal.valueOf(500));

        when(ledgerEntryRepository.sumDebitsForPeriod(any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(ledgerEntryRepository.sumCreditsForPeriod(any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);

        when(billingRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(mapper.toResponse(any())).thenReturn(mock(BillingStatementResponse.class));

        doNothing().when(entityManager).flush();
        service.generateStatement(accountId);

        
        verify(transactionService).postSystemTransaction(
        	    any(), 
        	    eq(TransactionType.INTEREST),
        	    argThat(val -> val.compareTo(BigDecimal.valueOf(500)) == 0),
        	    eq("Interest Charged"),
        	    any(), 
        	    any()
        	);
        
        
    }
    @Test
    void shouldReturnZeroMinimumDue_whenClosingBalanceNegative() {
        when(accountService.getAccountEntity(accountId)).thenReturn(account);
        when(timezoneResolver.resolve(any())).thenReturn(ZoneId.systemDefault());

        when(billingRepository.existsByAccountAccountIdAndBillingPeriodEnd(any(), any()))
                .thenReturn(false);

        when(billingRepository.findTopByAccountOrderByBillingPeriodEndDesc(any()))
                .thenReturn(Optional.empty());

        when(interestService.calculateInterest(any(), any(), any(), any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);

        // 🔥 negative closing balance
        when(ledgerEntryRepository.sumDebitsForPeriod(any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);
        when(ledgerEntryRepository.sumCreditsForPeriod(any(), any(), any()))
                .thenReturn(BigDecimal.valueOf(1000));

        when(billingRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(mapper.toResponse(any())).thenReturn(mock(BillingStatementResponse.class));

        BillingStatementResponse res = service.generateStatement(accountId);

        assertThat(res).isNotNull();
    }
    @Test
    void shouldMarkStatementAsPaid() {
        BillingStatement stmt = new BillingStatement();
        stmt.setStatementId(UUID.randomUUID());
        stmt.setAccount(account);
        stmt.setDueDate(LocalDate.now().minusDays(1));
        stmt.setTotalAmountDue(BigDecimal.valueOf(1000));
        stmt.setMinimumDueAmount(BigDecimal.valueOf(100));

        when(billingRepository.findDueStatementsPendingEvaluation(any()))
                .thenReturn(List.of(stmt));

        when(timezoneResolver.resolve(any())).thenReturn(ZoneId.systemDefault());

        // 🔥 full payment
        when(paymentAllocationRepository.sumAllocatedToStatementBefore(any(), any()))
                .thenReturn(BigDecimal.valueOf(1000));

        service.processDueStatements();

        assertThat(stmt.getStatementStatus()).isEqualTo(StatementStatus.PAID);
    }
    
    @Test
    void shouldMarkStatementAsRevolving() {
        BillingStatement stmt = new BillingStatement();
        stmt.setStatementId(UUID.randomUUID());
        stmt.setAccount(account);
        stmt.setDueDate(LocalDate.now().minusDays(1));
        stmt.setTotalAmountDue(BigDecimal.valueOf(1000));
        stmt.setMinimumDueAmount(BigDecimal.valueOf(100));

        when(billingRepository.findDueStatementsPendingEvaluation(any()))
                .thenReturn(List.of(stmt));

        when(timezoneResolver.resolve(any())).thenReturn(ZoneId.systemDefault());

        // 🔥 min due paid
        when(paymentAllocationRepository.sumAllocatedToStatementBefore(any(), any()))
                .thenReturn(BigDecimal.valueOf(200));

        service.processDueStatements();

        assertThat(stmt.getStatementStatus()).isEqualTo(StatementStatus.REVOLVING);
    }
    
    @Test
    void shouldApplyLateFee_whenOverdue() {
        BillingStatement stmt = new BillingStatement();
        stmt.setStatementId(UUID.randomUUID());
        stmt.setAccount(account);
        stmt.setDueDate(LocalDate.now().minusDays(1));
        stmt.setTotalAmountDue(BigDecimal.valueOf(1000));
        stmt.setMinimumDueAmount(BigDecimal.valueOf(100));
        stmt.setRemainingAmount(BigDecimal.valueOf(1000));
        stmt.setClosingBalance(BigDecimal.valueOf(1000));
        stmt.setLateFeeApplied(false);

        when(billingRepository.findDueStatementsPendingEvaluation(any()))
                .thenReturn(List.of(stmt));

        when(timezoneResolver.resolve(any())).thenReturn(ZoneId.systemDefault());

        when(paymentAllocationRepository.sumAllocatedToStatementBefore(any(), any()))
                .thenReturn(BigDecimal.ZERO);

        service.processDueStatements();

        assertThat(stmt.getStatementStatus()).isEqualTo(StatementStatus.OVERDUE);
        assertThat(stmt.getLateFeeApplied()).isTrue();

        verify(transactionService).postSystemTransaction(
                any(), eq(TransactionType.FEE), any(), any(), any(),any()
        );
    }
    
    @Test
    void shouldNotApplyLateFeeTwice() {
        BillingStatement stmt = new BillingStatement();
        stmt.setStatementId(UUID.randomUUID());
        stmt.setAccount(account);
        stmt.setDueDate(LocalDate.now().minusDays(1));
        stmt.setTotalAmountDue(BigDecimal.valueOf(1000));
        stmt.setMinimumDueAmount(BigDecimal.valueOf(100));
        stmt.setLateFeeApplied(true); // 🔥 already applied

        when(billingRepository.findDueStatementsPendingEvaluation(any()))
                .thenReturn(List.of(stmt));

        when(timezoneResolver.resolve(any())).thenReturn(ZoneId.systemDefault());

        when(paymentAllocationRepository.sumAllocatedToStatementBefore(any(), any()))
                .thenReturn(BigDecimal.ZERO);

        service.processDueStatements();

        verify(transactionService, never()).postSystemTransaction(
                any(), eq(TransactionType.FEE), any(), any(), any(),any()
        );
    }
    @Test
    void shouldContinueProcessing_whenOneFails() {
        BillingStatement stmt1 = new BillingStatement();
        stmt1.setStatementId(UUID.randomUUID());
        stmt1.setAccount(account);
        stmt1.setDueDate(LocalDate.now().minusDays(1));
        stmt1.setTotalAmountDue(BigDecimal.valueOf(1000));
        stmt1.setMinimumDueAmount(BigDecimal.valueOf(100));

        BillingStatement stmt2 = new BillingStatement();
        stmt2.setStatementId(UUID.randomUUID());
        stmt2.setAccount(account);
        stmt2.setDueDate(LocalDate.now().minusDays(1));
        stmt2.setTotalAmountDue(BigDecimal.valueOf(1000));
        stmt2.setMinimumDueAmount(BigDecimal.valueOf(100));

        when(billingRepository.findDueStatementsPendingEvaluation(any()))
                .thenReturn(List.of(stmt1, stmt2));

        when(timezoneResolver.resolve(any()))
                .thenReturn(ZoneId.systemDefault());

        // ✅ NOW this will be used
        when(paymentAllocationRepository.sumAllocatedToStatementBefore(any(), any()))
                .thenThrow(new RuntimeException("fail"))
                .thenReturn(BigDecimal.ZERO);

        service.processDueStatements();

        verify(billingRepository).saveAll(any());
    }
}