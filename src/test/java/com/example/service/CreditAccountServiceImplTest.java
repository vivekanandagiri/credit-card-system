//package com.example.service;
//
//import com.example.dto.request.CreditAccountStatusUpdateRequest;
//import com.example.dto.response.CreditAccountResponse;
//import com.example.entity.CreditAccount;
//import com.example.entity.CreditCardApplication;
//import com.example.entity.CreditProduct;
//import com.example.entity.Customer;
//import com.example.enums.AccountStatus;
//import com.example.enums.ApplicationStatus;
//import com.example.exception.BusinessRuleException;
//import com.example.exception.ConflictException;
//import com.example.exception.ResourceNotFoundException;
//import com.example.mapper.CreditAccountMapper;
//import com.example.repository.CreditAccountRepository;
//import com.example.service.ServiceImpl.CreditAccountServiceImpl;
//import com.example.util.AccountNumberGenerator;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Nested;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.math.BigDecimal;
//import java.util.Optional;
//import java.util.UUID;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.junit.jupiter.api.Assertions.assertThrows;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class CreditAccountServiceImplTest {
//
//    @Mock private CreditAccountRepository repository;
//    @Mock private AccountNumberGenerator generator;
//    @Mock private CreditAccountMapper mapper;
//    @Mock private CustomerService customerService;
//
//    @InjectMocks
//    private CreditAccountServiceImpl service;
//
//    private UUID accountId;
//    private CreditAccount account;
//    private CreditCardApplication application;
//    
//    
//
//    @BeforeEach
//    void setUp() {
//        accountId = UUID.randomUUID();
//
//        account = new CreditAccount();
//        account.setAccountStatus(AccountStatus.ACTIVE);
//        account.setAvailableBalance(new BigDecimal("1000"));
//        account.setCurrentBalance(BigDecimal.ZERO);
//        account.setCreditLimit(new BigDecimal("1000"));
//
//        Customer customer = new Customer();
//        customer.setCustomerId(UUID.randomUUID());
//
//        application = new CreditCardApplication();
//        application.setApplicationId(UUID.randomUUID());
//        application.setApplicationStatus(ApplicationStatus.APPROVED);
//        application.setCustomer(customer);
//        application.setApprovedCreditLimit(new BigDecimal("1000"));
//        
//        CreditProduct creditProduct = new CreditProduct();
//        creditProduct.setProductCode("PROD123");
//
//        application.setCreditProduct(creditProduct);
//    }
//
//    // ---------------- CREATE ACCOUNT ----------------
//    @Nested
//    class CreateAccountTests {
//
//        @Test
//        void shouldCreateAccountSuccessfully() {
//            when(repository.existsByApplicationApplicationId(application.getApplicationId()))
//                    .thenReturn(false);
//            when(generator.generate(any())).thenReturn("ACC123");
//            when(repository.save(any())).thenReturn(account);
//            when(mapper.toResponse(any())).thenReturn(new CreditAccountResponse());
//
//            CreditAccountResponse response = service.createAccount(application);
//
//            assertThat(response).isNotNull();
//            verify(repository).save(any());
//        }
//
//        @Test
//        void shouldThrowException_whenApplicationNotApproved() {
//            application.setApplicationStatus(ApplicationStatus.REJECTED);
//
//            assertThrows(BusinessRuleException.class,
//                    () -> service.createAccount(application));
//        }
//
//        @Test
//        void shouldThrowException_whenDuplicateAccountExists() {
//            when(repository.existsByApplicationApplicationId(application.getApplicationId()))
//                    .thenReturn(true);
//
//            assertThrows(ConflictException.class,
//                    () -> service.createAccount(application));
//        }
//    }
//
//    // ---------------- GET ACCOUNT BY ID ----------------
//    @Test
//    void shouldReturnAccount_whenFound() {
//        when(repository.findById(accountId)).thenReturn(Optional.of(account));
//        when(mapper.toResponse(account)).thenReturn(new CreditAccountResponse());
//
//        var response = service.getAccountById(accountId);
//
//        assertThat(response.getData()).isNotNull();
//    }
//
//    @Test
//    void shouldThrowException_whenAccountNotFound() {
//        when(repository.findById(accountId)).thenReturn(Optional.empty());
//
//        assertThrows(ResourceNotFoundException.class,
//                () -> service.getAccountById(accountId));
//    }
//
//    // ---------------- UPDATE STATUS ----------------
//    @Nested
//    class UpdateStatusTests {
//
//        @Test
//        void shouldUpdateStatusSuccessfully() {
//            when(repository.findById(accountId)).thenReturn(Optional.of(account));
//            when(repository.save(account)).thenReturn(account);
//            when(mapper.toResponse(account)).thenReturn(new CreditAccountResponse());
//
//            CreditAccountStatusUpdateRequest request = new CreditAccountStatusUpdateRequest();
//            request.setStatus("SUSPENDED");
//
//            var response = service.updateAccountStatus(accountId, request);
//
//            assertThat(response.getData()).isNotNull();
//        }
//
//        @Test
//        void shouldThrowException_whenSameStatus() {
//            when(repository.findById(accountId)).thenReturn(Optional.of(account));
//
//            CreditAccountStatusUpdateRequest request = new CreditAccountStatusUpdateRequest();
//            request.setStatus("ACTIVE");
//
//            assertThrows(BusinessRuleException.class,
//                    () -> service.updateAccountStatus(accountId, request));
//        }
//    }
//
//    // ---------------- DEDUCT BALANCE ----------------
//    @Nested
//    class DeductBalanceTests {
//
//        @Test
//        void shouldDeductBalanceSuccessfully() {
//            when(repository.findById(accountId)).thenReturn(Optional.of(account));
//
//            service.deductBalance(accountId, new BigDecimal("100"));
//
//            verify(repository).save(account);
//        }
//
//        @Test
//        void shouldThrowException_whenInsufficientBalance() {
//            when(repository.findById(accountId)).thenReturn(Optional.of(account));
//
//            assertThrows(BusinessRuleException.class,
//                    () -> service.deductBalance(accountId, new BigDecimal("2000")));
//        }
//    }
//
//    // ---------------- ADD BALANCE ----------------
//    @Nested
//    class AddBalanceTests {
//
//        @Test
//        void shouldAddBalanceSuccessfully() {
//            when(repository.findById(accountId)).thenReturn(Optional.of(account));
//
//            service.addBalance(accountId, new BigDecimal("100"));
//
//            verify(repository).save(account);
//        }
//
//        @Test
//        void shouldThrowException_whenAmountInvalid() {
//            assertThrows(BusinessRuleException.class,
//                    () -> service.addBalance(accountId, BigDecimal.ZERO));
//        }
//    }
//}