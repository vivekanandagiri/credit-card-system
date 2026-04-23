package com.example.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import com.example.entity.*;
import com.example.enums.EntryType;
import com.example.repository.LedgerEntryRepository;
import com.example.service.ServiceImpl.InterestCalculationServiceImpl;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class InterestCalculationServiceImplTest {

    @Mock
    private LedgerEntryRepository ledgerRepo;

    @InjectMocks
    private InterestCalculationServiceImpl service;

    private UUID accountId;
    private CreditAccount account;
    private BillingStatement lastStatement;
    private ZoneId zone;

    private Instant start;
    private Instant end;

    @BeforeEach
    void setup() {
        accountId = UUID.randomUUID();

        account = new CreditAccount();
        account.setApr(BigDecimal.valueOf(36)); // 36% APR

        lastStatement = new BillingStatement();
        lastStatement.setClosingBalance(BigDecimal.valueOf(1000));
        lastStatement.setAmountPaid(BigDecimal.valueOf(200));
        lastStatement.setTotalAmountDue(BigDecimal.valueOf(1000));

        zone = ZoneId.systemDefault();

        start = Instant.now().minus(30, ChronoUnit.DAYS);
        end = Instant.now();
    }

    // ================= GRACE PERIOD =================

    @Test
    void shouldReturnZero_whenGracePeriod() {
        lastStatement.setAmountPaid(BigDecimal.valueOf(1000)); // fully paid

        BigDecimal result = service.calculateInterest(
                accountId, start, end, account, lastStatement, zone);

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void shouldReturnZero_whenNoPreviousStatement() {
        BigDecimal result = service.calculateInterest(
                accountId, start, end, account, null, zone);

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ================= NO ENTRIES =================

    @Test
    void shouldCalculateSimpleInterest_whenNoEntries() {
        when(ledgerRepo.findByAccountIdAndCreatedAtBetween(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        BigDecimal result = service.calculateInterest(
                accountId, start, end, account, lastStatement, zone);

        assertThat(result).isGreaterThan(BigDecimal.ZERO);
    }

    // ================= NULL ENTRIES =================

    @Test
    void shouldHandleNullEntries_asSimpleInterest() {
        when(ledgerRepo.findByAccountIdAndCreatedAtBetween(any(), any(), any()))
                .thenReturn(null);

        BigDecimal result = service.calculateInterest(
                accountId, start, end, account, lastStatement, zone);

        assertThat(result).isGreaterThan(BigDecimal.ZERO);
    }

    // ================= WITH ENTRIES =================

    @Test
    void shouldCalculateInterest_withDebitAndCreditEntries() {

        LedgerEntry debit = new LedgerEntry();
        debit.setCreatedAt(start.plus(5, ChronoUnit.DAYS));
        debit.setAmount(BigDecimal.valueOf(500));
        debit.setEntryType(EntryType.DEBIT);

        LedgerEntry credit = new LedgerEntry();
        credit.setCreatedAt(start.plus(10, ChronoUnit.DAYS));
        credit.setAmount(BigDecimal.valueOf(200));
        credit.setEntryType(EntryType.CREDIT);

        when(ledgerRepo.findByAccountIdAndCreatedAtBetween(any(), any(), any()))
        .thenReturn(new ArrayList<>(List.of(debit, credit))); 

        BigDecimal result = service.calculateInterest(
                accountId, start, end, account, lastStatement, zone);

        assertThat(result).isGreaterThan(BigDecimal.ZERO);
    }

    // ================= ENTRY BEFORE START =================

    @Test
    void shouldIgnoreEntries_beforeStartDate() {

        LedgerEntry oldEntry = new LedgerEntry();
        oldEntry.setCreatedAt(start.minus(1, ChronoUnit.DAYS));
        oldEntry.setAmount(BigDecimal.valueOf(500));
        oldEntry.setEntryType(EntryType.DEBIT);

        when(ledgerRepo.findByAccountIdAndCreatedAtBetween(any(), any(), any()))
        .thenReturn(new ArrayList<>(List.of(oldEntry)));

        BigDecimal result = service.calculateInterest(
                accountId, start, end, account, lastStatement, zone);

        assertThat(result).isGreaterThanOrEqualTo(BigDecimal.ZERO);
    }

    // ================= ZERO BALANCE =================

    @Test
    void shouldReturnZero_whenBalanceBecomesZero() {

        lastStatement.setClosingBalance(BigDecimal.ZERO);
        lastStatement.setAmountPaid(BigDecimal.ZERO);

        when(ledgerRepo.findByAccountIdAndCreatedAtBetween(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        BigDecimal result = service.calculateInterest(
                accountId, start, end, account, lastStatement, zone);

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ================= FINAL SEGMENT =================

    @Test
    void shouldCalculateFinalSegmentInterest() {

        LedgerEntry entry = new LedgerEntry();
        entry.setCreatedAt(start.plus(5, ChronoUnit.DAYS));
        entry.setAmount(BigDecimal.valueOf(500));
        entry.setEntryType(EntryType.DEBIT);

        when(ledgerRepo.findByAccountIdAndCreatedAtBetween(any(), any(), any()))
        .thenReturn(new ArrayList<>(List.of(entry)));

        BigDecimal result = service.calculateInterest(
                accountId, start, end, account, lastStatement, zone);

        assertThat(result).isGreaterThan(BigDecimal.ZERO);
    }

    // ================= DAYS EDGE =================

    @Test
    void shouldReturnZero_whenNoDaysBetween() {
        Instant same = Instant.now();

        when(ledgerRepo.findByAccountIdAndCreatedAtBetween(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        BigDecimal result = service.calculateInterest(
                accountId, same, same, account, lastStatement, zone);

        assertThat(result).isEqualByComparingTo(BigDecimal.ZERO);
    }
}