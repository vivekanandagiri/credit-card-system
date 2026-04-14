package com.example.service;

import com.example.dto.request.CreditAccountStatusUpdateRequest;
import com.example.dto.response.CreditAccountResponse;
import com.example.entity.*;
import com.example.enums.*;
import com.example.exception.*;
import com.example.mapper.CreditAccountMapper;
import com.example.repository.CreditAccountRepository;
import com.example.service.ServiceImpl.CreditAccountServiceImpl;
import com.example.util.AccountNumberGenerator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import org.mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreditAccountServiceImplTest {

    @Mock private CreditAccountRepository accountRepository;
    @Mock private AccountNumberGenerator accountNumberGenerator;
    @Mock private CreditAccountMapper accountMapper;
    @Mock private CustomerService customerService;

    @InjectMocks
    private CreditAccountServiceImpl service;

    private CreditCardApplication approvedApp;
    private CreditAccount account;
    private UUID accountId;

    @BeforeEach
    void setup() {

        accountId = UUID.randomUUID();

        Customer customer = new Customer();
        customer.setCustomerId(UUID.randomUUID());

        CreditProduct product = new CreditProduct();
        product.setProductCode("P1");
        product.setGracePeriodDays(10);
        product.setMinimumDuePercent(BigDecimal.TEN);
        product.setLateFeeAmount(BigDecimal.TEN);
        product.setCreditProductId(1L);

        approvedApp = new CreditCardApplication();
        approvedApp.setApplicationId(UUID.randomUUID());
        approvedApp.setApplicationStatus(ApplicationStatus.APPROVED);
        approvedApp.setApprovedCreditLimit(BigDecimal.valueOf(10000));
        approvedApp.setApprovedApr(BigDecimal.TEN);
        approvedApp.setCustomer(customer);
        approvedApp.setCreditProduct(product);

        account = new CreditAccount();
        account.setAccountStatus(AccountStatus.ACTIVE);
        account.setAvailableBalance(BigDecimal.valueOf(1000));
        account.setCurrentBalance(BigDecimal.ZERO);
        account.setCreditLimit(BigDecimal.valueOf(1000));
        account.setCustomer(customer);
    }

    // ================= CREATE ACCOUNT =================

    @Test
    void shouldCreateAccountSuccessfully() {

        when(accountRepository.existsByApplicationApplicationId(any())).thenReturn(false);
        when(accountNumberGenerator.generate(any())).thenReturn("ACC123");

        CreditAccount saved = new CreditAccount();
        when(accountRepository.save(any())).thenReturn(saved);
        when(accountMapper.toResponse(saved)).thenReturn(mock(CreditAccountResponse.class));

        CreditAccountResponse result = service.createAccount(approvedApp);

        assertNotNull(result);
    }

    @Test
    void shouldThrowException_whenApplicationNotApproved() {
        approvedApp.setApplicationStatus(ApplicationStatus.REJECTED);

        assertThrows(BusinessRuleException.class,
                () -> service.createAccount(approvedApp));
    }

    @Test
    void shouldThrowConflict_whenDuplicateAccountExists() {

        when(accountRepository.existsByApplicationApplicationId(any())).thenReturn(true);

        assertThrows(ConflictException.class,
                () -> service.createAccount(approvedApp));
    }

    @Test
    void shouldThrowException_whenInvalidCreditLimit() {

        approvedApp.setApprovedCreditLimit(BigDecimal.ZERO);

        when(accountRepository.existsByApplicationApplicationId(any())).thenReturn(false);

        assertThrows(BusinessRuleException.class,
                () -> service.createAccount(approvedApp));
    }

    // ================= GET ACCOUNTS =================

    @Test
    void shouldReturnCustomerAccounts() {

        UUID userId = UUID.randomUUID();

        when(customerService.getCustomerByUserId(userId)).thenReturn(account.getCustomer());
        when(accountRepository.findAllByCustomerCustomerId(any()))
                .thenReturn(List.of(account));

        when(accountMapper.toResponse(any())).thenReturn(mock(CreditAccountResponse.class));

        List<CreditAccountResponse> result =
                service.getAccounts(userId, UserRole.CUSTOMER, null);

        assertEquals(1, result.size());
    }

    @Test
    void shouldReturnAdminAccounts() {

        when(accountRepository.findAll()).thenReturn(List.of(account));
        when(accountMapper.toResponse(any())).thenReturn(mock(CreditAccountResponse.class));

        List<?> result =
                service.getAccounts(UUID.randomUUID(), UserRole.ADMIN, null);

        assertEquals(1, result.size());
    }

    // ================= GET ACCOUNT BY ID =================

    @Test
    void shouldReturnAccountForAdmin() {

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(accountMapper.toResponse(any())).thenReturn(mock(CreditAccountResponse.class));

        CreditAccountResponse result =
                service.getAccountById(UUID.randomUUID(), UserRole.ADMIN, accountId);

        assertNotNull(result);
    }

    @Test
    void shouldThrowAccessDenied_forCustomer() {

        UUID userId = UUID.randomUUID();

        Customer other = new Customer();
        other.setCustomerId(UUID.randomUUID());

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(customerService.getCustomerByUserId(userId)).thenReturn(other);

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.getAccountById(userId, UserRole.CUSTOMER, accountId));
    }

    // ================= STATUS UPDATE =================

    @Test
    void shouldUpdateStatusSuccessfully() {

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(accountRepository.save(any())).thenReturn(account);
        when(accountMapper.toResponse(any())).thenReturn(mock(CreditAccountResponse.class));

        CreditAccountStatusUpdateRequest req = new CreditAccountStatusUpdateRequest();
        req.setStatus(AccountStatus.BLOCKED);

        service.updateAccountStatus(accountId, req);

        assertEquals(AccountStatus.BLOCKED, account.getAccountStatus());
    }

    @Test
    void shouldThrowException_whenStatusNull() {

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        CreditAccountStatusUpdateRequest req = new CreditAccountStatusUpdateRequest();

        assertThrows(BusinessRuleException.class,
                () -> service.updateAccountStatus(accountId, req));
    }

    @Test
    void shouldThrowException_whenSameStatus() {

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        CreditAccountStatusUpdateRequest req = new CreditAccountStatusUpdateRequest();
        req.setStatus(AccountStatus.ACTIVE);

        assertThrows(BusinessRuleException.class,
                () -> service.updateAccountStatus(accountId, req));
    }

    @Test
    void shouldThrowException_whenClosedAccountTransition() {

        account.setAccountStatus(AccountStatus.CLOSED);

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        CreditAccountStatusUpdateRequest req = new CreditAccountStatusUpdateRequest();
        req.setStatus(AccountStatus.ACTIVE);

        assertThrows(BusinessRuleException.class,
                () -> service.updateAccountStatus(accountId, req));
    }

    @Test
    void shouldSetClosedAt_whenClosed() {

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(accountRepository.save(any())).thenReturn(account);
        when(accountMapper.toResponse(any())).thenReturn(mock(CreditAccountResponse.class));

        CreditAccountStatusUpdateRequest req = new CreditAccountStatusUpdateRequest();
        req.setStatus(AccountStatus.CLOSED);

        service.updateAccountStatus(accountId, req);

        assertNotNull(account.getClosedAt());
    }

    // ================= BALANCE =================

    @Test
    void shouldDeductBalanceSuccessfully() {

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        service.deductBalance(accountId, BigDecimal.valueOf(200));

        assertEquals(BigDecimal.valueOf(800), account.getAvailableBalance());
    }

    @Test
    void shouldThrowException_whenInsufficientBalance() {

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        assertThrows(BusinessRuleException.class,
                () -> service.deductBalance(accountId, BigDecimal.valueOf(2000)));
    }

    @Test
    void shouldThrowException_whenInvalidAmount() {

        assertThrows(BusinessRuleException.class,
                () -> service.deductBalance(accountId, BigDecimal.ZERO));
    }

    @Test
    void shouldAddBalanceSuccessfully() {

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        service.addBalance(accountId, BigDecimal.valueOf(200));

        assertNotNull(account.getLastPaymentDate());
    }

    @Test
    void shouldThrowConflict_whenDuplicatePayment() {

        account.setLastPaymentAmount(BigDecimal.TEN);
        account.setLastPaymentDate(Instant.now());

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        assertThrows(ConflictException.class,
                () -> service.addBalance(accountId, BigDecimal.valueOf(100)));
    }

    // ================= APPLY PAYMENT =================

    @Test
    void shouldApplyPaymentSuccessfully() {

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        service.applyPayment(accountId, BigDecimal.valueOf(100), Instant.now());

        assertNotNull(account.getLastPaymentAmount());
    }

    @Test
    void shouldThrowException_whenInvalidPaymentAmount() {

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        assertThrows(BadRequestException.class,
                () -> service.applyPayment(accountId, BigDecimal.ZERO, Instant.now()));
    }

    // ================= BILLING =================

    @Test
    void shouldUpdateAccountAfterBilling() {

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        Instant now = Instant.now();

        service.updateAccountAfterBilling(accountId, now,
                BigDecimal.valueOf(1000), now.plusSeconds(1000), BigDecimal.TEN);

        assertEquals(now, account.getLastStatementDate());
    }

    // ================= EXISTS =================

    @Test
    void shouldReturnAccountExists() {

        when(accountRepository.existsByApplicationApplicationId(any())).thenReturn(true);

        assertTrue(service.accountExistsForApplication(UUID.randomUUID()));
    }

    @Test
    void shouldReturnHasActiveAccount() {

        when(accountRepository.existsByCustomerCustomerIdAndCreditProductCreditProductIdAndAccountStatus(any(), any(), any()))
                .thenReturn(true);

        assertTrue(service.hasActiveAccountForProduct(UUID.randomUUID(), 1L));
    }

    // ================= ENTITY =================

    @Test
    void shouldReturnAccountEntity() {

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        CreditAccount result = service.getAccountEntity(accountId);

        assertNotNull(result);
    }

    @Test
    void shouldThrowException_whenAccountNotFound() {

        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getAccountEntity(accountId));
    }
}