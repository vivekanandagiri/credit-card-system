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

import com.example.dto.request.CreditAccountStatusUpdateRequest;
import com.example.dto.response.CreditAccountResponse;
import com.example.entity.*;
import com.example.enums.*;
import com.example.exception.*;
import com.example.mapper.CreditAccountMapper;
import com.example.repository.CreditAccountRepository;
import com.example.service.ServiceImpl.CreditAccountServiceImpl;
import com.example.util.AccountNumberGenerator;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
class CreditAccountServiceImplTest {

    @Mock private CreditAccountRepository repository;
    @Mock private AccountNumberGenerator generator;
    @Mock private CreditAccountMapper mapper;
    @Mock private CustomerService customerService;

    @InjectMocks
    private CreditAccountServiceImpl service;

    private UUID userId;
    private UUID accountId;

    private Customer customer;
    private CreditAccount account;
    private CreditCardApplication application;
    private CreditProduct product;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        accountId = UUID.randomUUID();

        customer = TestFixtures.validCustomer();

        product = TestFixtures.validCreditProductEntity();

        application = TestFixtures.validApplication(customer, product);
        application.setApplicationStatus(ApplicationStatus.APPROVED);
        application.setApprovedCreditLimit(BigDecimal.valueOf(50000));
        application.setApprovedApr(BigDecimal.valueOf(12));

        account = TestFixtures.validCreditAccount(customer, application, product);
        account.setAccountId(accountId);
    }

    // ================= CREATE ACCOUNT =================

    @Nested
    class CreateAccount {

        @Test
        void shouldCreateAccount_success() {
            when(repository.existsByApplicationApplicationId(any())).thenReturn(false);
            when(generator.generate(any())).thenReturn("123456789012");
            when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(mapper.toResponse(any())).thenReturn(mock(CreditAccountResponse.class));

            CreditAccountResponse res = service.createAccount(application);

            assertThat(res).isNotNull();
        }

        @Test
        void shouldThrow_whenApplicationNotApproved() {
            application.setApplicationStatus(ApplicationStatus.REJECTED);

            assertThatThrownBy(() -> service.createAccount(application))
                    .isInstanceOf(BusinessRuleException.class);
        }

        @Test
        void shouldThrow_whenDuplicateAccount() {
            when(repository.existsByApplicationApplicationId(any())).thenReturn(true);

            assertThatThrownBy(() -> service.createAccount(application))
                    .isInstanceOf(ConflictException.class);
        }

        @Test
        void shouldThrow_whenInvalidCreditLimit() {
            application.setApprovedCreditLimit(BigDecimal.ZERO);

            when(repository.existsByApplicationApplicationId(any())).thenReturn(false);
            when(generator.generate(any())).thenReturn("123");

            assertThatThrownBy(() -> service.createAccount(application))
                    .isInstanceOf(BusinessRuleException.class);
        }
    }

    // ================= GET ACCOUNTS =================

    @Nested
    class GetAccounts {

        @Test
        void shouldReturnCustomerAccounts_withStatus() {
            when(customerService.getCustomerByUserId(userId)).thenReturn(customer);
            when(repository.findAllByCustomerCustomerIdAndAccountStatus(any(), any()))
                    .thenReturn(List.of(account));
            when(mapper.toResponse(any())).thenReturn(mock(CreditAccountResponse.class));

            List<CreditAccountResponse> res =
                    service.getAccounts(userId, UserRole.CUSTOMER, AccountStatus.ACTIVE);

            assertThat(res).hasSize(1);
        }

        @Test
        void shouldReturnCustomerAccounts_withoutStatus() {
            when(customerService.getCustomerByUserId(userId)).thenReturn(customer);
            when(repository.findAllByCustomerCustomerId(any()))
                    .thenReturn(List.of(account));
            when(mapper.toResponse(any())).thenReturn(mock(CreditAccountResponse.class));

            List<CreditAccountResponse> res =
                    service.getAccounts(userId, UserRole.CUSTOMER, null);

            assertThat(res).hasSize(1);
        }

        @Test
        void shouldReturnAdminAccounts_withStatus() {
            when(repository.findAllByAccountStatus(any()))
                    .thenReturn(List.of(account));
            when(mapper.toResponse(any())).thenReturn(mock(CreditAccountResponse.class));

            List<CreditAccountResponse> res =
                    service.getAccounts(userId, UserRole.ADMIN, AccountStatus.ACTIVE);

            assertThat(res).hasSize(1);
        }

        @Test
        void shouldReturnAllAccounts_admin() {
            when(repository.findAll()).thenReturn(List.of(account));
            when(mapper.toResponse(any())).thenReturn(mock(CreditAccountResponse.class));

            List<CreditAccountResponse> res =
                    service.getAccounts(userId, UserRole.ADMIN, null);

            assertThat(res).hasSize(1);
        }
    }

    // ================= GET ACCOUNT BY ID =================

    @Nested
    class GetAccountById {

        @Test
        void shouldReturnAccount_forAdmin() {
            when(repository.findById(accountId)).thenReturn(Optional.of(account));
            when(mapper.toResponse(any())).thenReturn(mock(CreditAccountResponse.class));

            CreditAccountResponse res =
                    service.getAccountById(userId, UserRole.ADMIN, accountId);

            assertThat(res).isNotNull();
        }

        @Test
        void shouldReturnAccount_forCustomer() {
            when(repository.findById(accountId)).thenReturn(Optional.of(account));
            when(customerService.getCustomerByUserId(userId)).thenReturn(customer);
            when(mapper.toResponse(any())).thenReturn(mock(CreditAccountResponse.class));

            CreditAccountResponse res =
                    service.getAccountById(userId, UserRole.CUSTOMER, accountId);

            assertThat(res).isNotNull();
        }

        @Test
        void shouldThrow_whenAccessDenied() {
            Customer another = new Customer();
            another.setCustomerId(UUID.randomUUID());
            account.setCustomer(another);

            when(repository.findById(accountId)).thenReturn(Optional.of(account));
            when(customerService.getCustomerByUserId(userId)).thenReturn(customer);

            assertThatThrownBy(() ->
                    service.getAccountById(userId, UserRole.CUSTOMER, accountId))
                    .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        }

        @Test
        void shouldThrow_whenNotFound() {
            when(repository.findById(accountId)).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    service.getAccountById(userId, UserRole.ADMIN, accountId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ================= UPDATE STATUS =================

    @Nested
    class UpdateStatus {

        @Test
        void shouldUpdateStatus_success() {
            CreditAccountStatusUpdateRequest req = mock(CreditAccountStatusUpdateRequest.class);

            when(repository.findById(accountId)).thenReturn(Optional.of(account));
            when(req.getStatus()).thenReturn(AccountStatus.BLOCKED);
            when(repository.save(any())).thenReturn(account);
            when(mapper.toResponse(any())).thenReturn(mock(CreditAccountResponse.class));

            CreditAccountResponse res =
                    service.updateAccountStatus(accountId, req);

            assertThat(res).isNotNull();
        }

        @Test
        void shouldThrow_whenNullStatus() {
            CreditAccountStatusUpdateRequest req = mock(CreditAccountStatusUpdateRequest.class);

            when(repository.findById(accountId)).thenReturn(Optional.of(account));
            when(req.getStatus()).thenReturn(null);

            assertThatThrownBy(() ->
                    service.updateAccountStatus(accountId, req))
                    .isInstanceOf(BusinessRuleException.class);
        }

        @Test
        void shouldThrow_whenSameStatus() {
            CreditAccountStatusUpdateRequest req = mock(CreditAccountStatusUpdateRequest.class);

            when(repository.findById(accountId)).thenReturn(Optional.of(account));
            when(req.getStatus()).thenReturn(account.getAccountStatus());

            assertThatThrownBy(() ->
                    service.updateAccountStatus(accountId, req))
                    .isInstanceOf(BusinessRuleException.class);
        }

        @Test
        void shouldThrow_whenClosedAccountTransition() {
            account.setAccountStatus(AccountStatus.CLOSED);

            CreditAccountStatusUpdateRequest req = mock(CreditAccountStatusUpdateRequest.class);

            when(repository.findById(accountId)).thenReturn(Optional.of(account));
            when(req.getStatus()).thenReturn(AccountStatus.ACTIVE);

            assertThatThrownBy(() ->
                    service.updateAccountStatus(accountId, req))
                    .isInstanceOf(BusinessRuleException.class);
        }
    }

    // ================= APPLY PAYMENT =================

    @Nested
    class ApplyPayment {

        @Test
        void shouldApplyPayment_success() {
            account.setCreditLimit(BigDecimal.valueOf(100000));
            account.setCurrentBalance(BigDecimal.valueOf(20000));

            when(repository.findById(accountId)).thenReturn(Optional.of(account));

            service.applyPayment(accountId, BigDecimal.valueOf(5000), Instant.now());

            verify(repository).save(account);
        }

        @Test
        void shouldThrow_whenInvalidAmount() {
            when(repository.findById(accountId)).thenReturn(Optional.of(account));

            assertThatThrownBy(() ->
                    service.applyPayment(accountId, BigDecimal.ZERO, Instant.now()))
                    .isInstanceOf(BadRequestException.class);
        }
    }
}