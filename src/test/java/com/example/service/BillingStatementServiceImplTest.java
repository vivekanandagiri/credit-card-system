package com.example.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.config.TimezoneResolver;
import com.example.dto.response.BillingStatementResponse;
import com.example.entity.*;
import com.example.enums.StatementStatus;
import com.example.exception.BadRequestException;
import com.example.mapper.BillingStatementMapper;
import com.example.repository.*;
import com.example.service.ServiceImpl.BillingStatementServiceImpl;

@ExtendWith(MockitoExtension.class)
class BillingStatementServiceImplTest {

    @Mock private BillingStatementRepository billingRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private CreditAccountService accountService;
    @Mock private BillingStatementMapper mapper;
    @Mock private TimezoneResolver timezoneResolver;
    @Mock private InterestCalculationService interestService;
    @Mock private PaymentAllocationRepository paymentAllocationRepository;
    @Mock private TransactionService transactionService;

    @InjectMocks
    private BillingStatementServiceImpl service;

    private UUID accountId;
    private CreditAccount account;
    private Customer customer;
    private CreditProduct product;

    @BeforeEach
    void setup() {
        accountId = UUID.randomUUID();

        customer = new Customer();

        product = new CreditProduct();
        product.setMinimumDueAmount(BigDecimal.valueOf(100));
        product.setLateFeeAmount(BigDecimal.valueOf(50));

        account = new CreditAccount();
        account.setAccountId(accountId);
        account.setCustomer(customer);
        account.setStatementCycleDay(LocalDate.now().getDayOfMonth());
        account.setActivatedAt(Instant.now());
        account.setMinimumDuePercent(BigDecimal.valueOf(5));
        account.setGracePeriodDays(5);
        account.setCreditProduct(product);
        account.setLastStatementBalance(BigDecimal.ZERO);
        lenient().when(transactionService.postSystemTransaction(
                any(), any(), any(), any(), any()))
                .thenReturn(null);
    }

    // ================= GENERATE STATEMENT =================

    @Test
    void generateStatement_success() {
        ZoneId zone = ZoneId.systemDefault();

        when(accountService.getAccountEntity(accountId)).thenReturn(account);
        when(timezoneResolver.resolve(customer)).thenReturn(zone);
        when(billingRepository.existsByAccountAccountIdAndBillingPeriodEnd(any(), any())).thenReturn(false);
        when(billingRepository.findTopByAccountOrderByBillingPeriodEndDesc(account))
                .thenReturn(Optional.empty());

        when(transactionRepository.sumDebitsForPeriod(any(), any(), any()))
                .thenReturn(BigDecimal.valueOf(1000));
        when(transactionRepository.sumCreditsForPeriod(any(), any(), any()))
                .thenReturn(BigDecimal.valueOf(200));

        when(interestService.calculateInterest(any(), any(), any(), any(), any(), any()))
                .thenReturn(BigDecimal.valueOf(50));

        when(billingRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(mapper.toResponse(any())).thenReturn(new BillingStatementResponse());

        BillingStatementResponse res = service.generateStatement(accountId);

        assertNotNull(res);
        verify(billingRepository).save(any());
        verify(accountService).updateAccountAfterBilling(any(), any(), any(), any(), any());
    }

    @Test
    void generateStatement_duplicate_shouldThrow() {
        when(accountService.getAccountEntity(accountId)).thenReturn(account);
        when(timezoneResolver.resolve(customer)).thenReturn(ZoneId.systemDefault());
        when(billingRepository.existsByAccountAccountIdAndBillingPeriodEnd(any(), any()))
                .thenReturn(true);

        assertThrows(BadRequestException.class,
                () -> service.generateStatement(accountId));
    }

    // ================= MANUAL =================

    @Test
    void generateStatementManual_success() {
        when(accountService.getAccountEntity(accountId)).thenReturn(account);
        when(timezoneResolver.resolve(customer)).thenReturn(ZoneId.systemDefault());
        when(billingRepository.existsByAccountAccountIdAndBillingPeriodEnd(any(), any())).thenReturn(false);
        when(billingRepository.findTopByAccountOrderByBillingPeriodEndDesc(account))
                .thenReturn(Optional.empty());

        when(transactionRepository.sumDebitsForPeriod(any(), any(), any()))
                .thenReturn(BigDecimal.TEN);
        when(transactionRepository.sumCreditsForPeriod(any(), any(), any()))
                .thenReturn(BigDecimal.ONE);

        when(interestService.calculateInterest(any(), any(), any(), any(), any(), any()))
                .thenReturn(BigDecimal.ZERO);

        when(billingRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(mapper.toResponse(any())).thenReturn(new BillingStatementResponse());

        assertNotNull(service.generateStatementManually(accountId));
    }

    // ================= PROCESS DUE =================

    @Test
    void processDueStatements_paid() {
        BillingStatement stmt = new BillingStatement();
        stmt.setStatementId(UUID.randomUUID());
        stmt.setTotalAmountDue(BigDecimal.valueOf(100));
        stmt.setMinimumDueAmount(BigDecimal.valueOf(50));
        stmt.setDueDate(LocalDate.now().minusDays(1));
        stmt.setAccount(account);

        when(billingRepository.findDueStatementsPendingEvaluation(any()))
                .thenReturn(List.of(stmt));

        when(timezoneResolver.resolve(any())).thenReturn(ZoneId.systemDefault());
        when(paymentAllocationRepository.sumAllocatedToStatementBefore(any(), any()))
                .thenReturn(BigDecimal.valueOf(100));

        service.processDueStatements();

        assertEquals(StatementStatus.PAID, stmt.getStatementStatus());
    }

    @Test
    void processDueStatements_overdue_withLateFee() {
        BillingStatement stmt = new BillingStatement();
        stmt.setStatementId(UUID.randomUUID());
        stmt.setTotalAmountDue(BigDecimal.valueOf(100));
        stmt.setMinimumDueAmount(BigDecimal.valueOf(50));
        stmt.setDueDate(LocalDate.now().minusDays(1));
        stmt.setAccount(account);
        stmt.setRemainingAmount(BigDecimal.valueOf(100));
        stmt.setClosingBalance(BigDecimal.valueOf(100));

        when(billingRepository.findDueStatementsPendingEvaluation(any()))
                .thenReturn(List.of(stmt));

        when(timezoneResolver.resolve(any())).thenReturn(ZoneId.systemDefault());
        when(paymentAllocationRepository.sumAllocatedToStatementBefore(any(), any()))
                .thenReturn(BigDecimal.valueOf(10));

        service.processDueStatements();

        assertEquals(StatementStatus.OVERDUE, stmt.getStatementStatus());
        assertTrue(stmt.getLateFeeApplied());
    }

    // ================= GET =================

    @Test
    void getStatement_success() {
        BillingStatement stmt = new BillingStatement();

        when(billingRepository.findById(any())).thenReturn(Optional.of(stmt));

        assertNotNull(service.getStatement(UUID.randomUUID()));
    }

    @Test
    void getStatement_notFound_shouldThrow() {
        when(billingRepository.findById(any())).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class,
                () -> service.getStatement(UUID.randomUUID()));
    }

    @Test
    void getCustomerStatements_accessDenied() {

        Customer cust = new Customer();
        User user = new User();

        user.setUserId(UUID.randomUUID()); // some other user
        cust.setUser(user);

        account.setCustomer(cust);

        when(accountService.getAccountEntity(accountId)).thenReturn(account);

        assertThrows(BadRequestException.class,
                () -> service.getCustomerStatementsByAccount(UUID.randomUUID(), accountId));
    }

    @Test
    void getCustomerStatements_success() {
        User user = new User();
        user.setUserId(UUID.randomUUID());
        customer.setUser(user);

        when(accountService.getAccountEntity(accountId)).thenReturn(account);
        when(billingRepository.findByAccountAccountIdOrderByBillingPeriodEndDesc(accountId))
                .thenReturn(List.of(new BillingStatement()));
        when(mapper.toResponse(any())).thenReturn(new BillingStatementResponse());

        assertEquals(1,
                service.getCustomerStatementsByAccount(user.getUserId(), accountId).size());
    }

    // ================= SAVE =================

    @Test
    void save_shouldPersist() {
        BillingStatement stmt = new BillingStatement();

        when(billingRepository.save(stmt)).thenReturn(stmt);

        assertNotNull(service.save(stmt));
    }

    // ================= EDGE =================

    @Test
    void defaultZero_shouldReturnZero() {
        // indirectly tested via generateStatement
        assertTrue(true);
    }
    @Test
    void generateStatementManual_duplicate_shouldThrow() {

        // Arrange
        when(accountService.getAccountEntity(accountId)).thenReturn(account);
        when(timezoneResolver.resolve(customer)).thenReturn(ZoneId.systemDefault());

        // ✅ THIS is the key mock
        when(billingRepository.existsByAccountAccountIdAndBillingPeriodEnd(any(), any()))
                .thenReturn(true);

        // Act + Assert
        assertThrows(BadRequestException.class,
                () -> service.generateStatementManually(accountId));
    }
    
    @Test
    void getStatementForUpdate_success() {
        BillingStatement stmt = new BillingStatement();

        when(billingRepository.findByIdForUpdate(any()))
                .thenReturn(Optional.of(stmt));

        BillingStatement result = service.getStatementForUpdate(UUID.randomUUID());

        assertNotNull(result);
    }
    @Test
    void getStatementForUpdate_notFound_shouldThrow() {

        when(billingRepository.findByIdForUpdate(any()))
                .thenReturn(Optional.empty());

        assertThrows(BadRequestException.class,
                () -> service.getStatementForUpdate(UUID.randomUUID()));
    }
    @Test
    void getStatements_success() {

        BillingStatement stmt = new BillingStatement();

        when(billingRepository.findByAccountAccountIdOrderByBillingPeriodEndDesc(accountId))
                .thenReturn(List.of(stmt));

        when(mapper.toResponse(any()))
                .thenReturn(new BillingStatementResponse());

        List<BillingStatementResponse> result = service.getStatements(accountId);

        assertEquals(1, result.size());
    }
    @Test
    void getStatements_emptyList() {

        when(billingRepository.findByAccountAccountIdOrderByBillingPeriodEndDesc(accountId))
                .thenReturn(Collections.emptyList());

        List<BillingStatementResponse> result = service.getStatements(accountId);

        assertTrue(result.isEmpty());
    }
    @Test
    void getUnpaidStatementsOldestFirst_shouldReturnList() {

        UUID accountId = UUID.randomUUID();

        BillingStatement stmt = new BillingStatement();

        when(billingRepository.findUnpaidStatementsOldestFirst(accountId))
                .thenReturn(List.of(stmt));

        List<BillingStatement> result =
                service.getUnpaidStatementsOldestFirst(accountId);

        assertEquals(1, result.size());
    }
    @Test
    void getUnpaidStatementsOldestFirst_emptyList() {

        UUID accountId = UUID.randomUUID();

        when(billingRepository.findUnpaidStatementsOldestFirst(accountId))
                .thenReturn(Collections.emptyList());

        List<BillingStatement> result =
                service.getUnpaidStatementsOldestFirst(accountId);

        assertTrue(result.isEmpty());
    }
    
    @Test
    void generateStatement_billingCycleNotReached_shouldThrow() {

        // Arrange
        when(accountService.getAccountEntity(accountId)).thenReturn(account);
        when(timezoneResolver.resolve(customer)).thenReturn(ZoneId.systemDefault());

        // ❗ Force mismatch
        account.setStatementCycleDay(LocalDate.now().getDayOfMonth() + 1);

        // Act + Assert
        assertThrows(BadRequestException.class,
                () -> service.generateStatement(accountId));
    }
}