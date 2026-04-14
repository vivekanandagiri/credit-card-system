package com.example.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.dto.request.*;
import com.example.dto.response.*;
import com.example.entity.*;
import com.example.enums.*;
import com.example.exception.*;
import com.example.mapper.CreditAccountApplicationMapper;
import com.example.repository.CreditCardApplicationRepository;
import com.example.service.ServiceImpl.CreditAccountApplicationServiceImpl;
import com.example.underwriting.UnderwritingService;
import com.example.underwriting.model.UnderwritingDecision;

@ExtendWith(MockitoExtension.class)
class CreditAccountApplicationServiceImplTest {

    @Mock private CreditCardApplicationRepository repository;
    @Mock private CustomerService customerService;
    @Mock private KycService kycService;
    @Mock private CreditProductService productService;
    @Mock private CreditAccountService accountService;
    @Mock private ActiveAccountChecker activeAccountChecker;
    @Mock private UnderwritingService underwritingService;
    @Mock private CreditAccountApplicationMapper mapper;

    @InjectMocks
    private CreditAccountApplicationServiceImpl service;

    private UUID userId;
    private Customer customer;
    private CreditProduct product;

    @BeforeEach
    void setup() {
        userId = UUID.randomUUID();

        customer = new Customer();
        customer.setCustomerId(UUID.randomUUID());

        product = new CreditProduct();
        product.setCreditProductId(1L);
    }

    private CreditCardApplicationRequest validRequest() {
        CreditCardApplicationRequest req = new CreditCardApplicationRequest();
        req.setCreditProductId(product.getCreditProductId());
        req.setCreditScoreAtApplication(700);
        req.setEmploymentType(EmploymentType.SALARIED);
        req.setEmployerName("ABC");
        req.setMonthlyIncome(BigDecimal.valueOf(50000));
        req.setRequestedCreditLimit(BigDecimal.valueOf(100000));
        return req;
    }

    // ================= APPLY =================

    @Test
    void apply_success_autoApproved() {
        CreditCardApplicationRequest req = validRequest();

        when(customerService.getCustomerByUserId(userId)).thenReturn(customer);
        when(kycService.isKycVerified(any())).thenReturn(true);
        when(productService.getActiveCreditProduct(any())).thenReturn(product);

        when(repository.existsByCustomerCustomerIdAndCreditProductCreditProductIdAndApplicationStatusIn(any(), any(), any()))
                .thenReturn(false);
        when(repository.countByCustomerCustomerIdAndApplicationStatusIn(any(), any()))
                .thenReturn(0);
        when(repository.existsByCustomerCustomerIdAndCreditProductCreditProductIdAndApplicationStatusAndSubmittedAtAfter(any(), any(), any(), any()))
                .thenReturn(false);
        when(activeAccountChecker.hasActiveAccount(any(), any())).thenReturn(false);

        UnderwritingDecision decision = mock(UnderwritingDecision.class);
        when(decision.getDecision()).thenReturn(DecisionType.AUTO_APPROVED);
        when(decision.getApprovedLimit()).thenReturn(BigDecimal.TEN);
        when(decision.getApprovedApr()).thenReturn(BigDecimal.ONE);
        when(underwritingService.evaluate(any())).thenReturn(decision);

        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(mapper.toSummaryResponse(any())).thenReturn(new CreditCardApplicationSummaryResponse());

        CreditCardApplicationSummaryResponse res = service.apply(userId, req);

        assertNotNull(res);
        verify(accountService).createAccount(any());
    }

    @Test
    void apply_kycNotVerified_shouldThrow() {
        when(customerService.getCustomerByUserId(userId)).thenReturn(customer);
        when(kycService.isKycVerified(any())).thenReturn(false);

        assertThrows(BusinessRuleException.class,
                () -> service.apply(userId, validRequest()));
    }

    @Test
    void apply_invalidCreditScore_shouldThrow() {
        CreditCardApplicationRequest req = validRequest();
        req.setCreditScoreAtApplication(100);

        when(customerService.getCustomerByUserId(userId)).thenReturn(customer);
        when(kycService.isKycVerified(any())).thenReturn(true);

        assertThrows(BusinessRuleException.class,
                () -> service.apply(userId, req));
    }

    @Test
    void apply_duplicateApplication_shouldThrow() {
        when(customerService.getCustomerByUserId(userId)).thenReturn(customer);
        when(kycService.isKycVerified(any())).thenReturn(true);
        when(productService.getActiveCreditProduct(any())).thenReturn(product);

        when(repository.existsByCustomerCustomerIdAndCreditProductCreditProductIdAndApplicationStatusIn(any(), any(), any()))
                .thenReturn(true);

        assertThrows(ConflictException.class,
                () -> service.apply(userId, validRequest()));
    }

    @Test
    void apply_activeLimitExceeded_shouldThrow() {
        when(customerService.getCustomerByUserId(userId)).thenReturn(customer);
        when(kycService.isKycVerified(any())).thenReturn(true);
        when(productService.getActiveCreditProduct(any())).thenReturn(product);

        when(repository.existsByCustomerCustomerIdAndCreditProductCreditProductIdAndApplicationStatusIn(any(), any(), any()))
                .thenReturn(false);
        when(repository.countByCustomerCustomerIdAndApplicationStatusIn(any(), any()))
                .thenReturn(3);

        assertThrows(BusinessRuleException.class,
                () -> service.apply(userId, validRequest()));
    }

    @Test
    void apply_employmentMissing_shouldThrow() {
        CreditCardApplicationRequest req = validRequest();
        req.setEmployerName(null);

        when(customerService.getCustomerByUserId(userId)).thenReturn(customer);
        when(kycService.isKycVerified(any())).thenReturn(true);
        when(productService.getActiveCreditProduct(any())).thenReturn(product);

        when(repository.existsByCustomerCustomerIdAndCreditProductCreditProductIdAndApplicationStatusIn(any(), any(), any()))
                .thenReturn(false);
        when(repository.countByCustomerCustomerIdAndApplicationStatusIn(any(), any()))
                .thenReturn(0);

        assertThrows(BusinessRuleException.class,
                () -> service.apply(userId, req));
    }

    // ================= DECIDE =================

    @Test
    void decide_success_manualApprove() {
        CreditCardApplication app = new CreditCardApplication();
        app.setApplicationStatus(ApplicationStatus.PENDING_REVIEW);

        when(repository.findById(any())).thenReturn(Optional.of(app));
        when(repository.save(any())).thenReturn(app);
        when(mapper.toResponse(any())).thenReturn(new CreditCardApplicationResponse());
        when(accountService.accountExistsForApplication(any())).thenReturn(false);

        ApplicationDecisionRequest req = new ApplicationDecisionRequest();
        req.setApproved(true);
        req.setApprovedCreditLimit(BigDecimal.TEN);
        req.setApprovedApr(BigDecimal.ONE);

        CreditCardApplicationResponse res = service.decide(UUID.randomUUID(), req);

        assertNotNull(res);
        verify(accountService).createAccount(any());
    }

    @Test
    void decide_notPending_shouldThrow() {
        CreditCardApplication app = new CreditCardApplication();
        app.setApplicationStatus(ApplicationStatus.APPROVED);

        when(repository.findById(any())).thenReturn(Optional.of(app));

        assertThrows(BusinessRuleException.class,
                () -> service.decide(UUID.randomUUID(), new ApplicationDecisionRequest()));
    }

    // ================= FETCH =================

    @Test
    void getCustomerApplicationById_accessDenied() {

        // App customer (different user)
        Customer appCustomer = new Customer();
        appCustomer.setCustomerId(UUID.randomUUID()); // ✅ IMPORTANT

        CreditCardApplication app = new CreditCardApplication();
        app.setCustomer(appCustomer);

        // Requesting customer
        Customer requestCustomer = new Customer();
        requestCustomer.setCustomerId(UUID.randomUUID()); // different ID

        when(customerService.getCustomer(any())).thenReturn(requestCustomer);
        when(repository.findById(any())).thenReturn(Optional.of(app));

        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.getCustomerApplicationById(
                        requestCustomer.getCustomerId(),
                        UUID.randomUUID()
                ));
    }

    @Test
    void getApplicationById_notFound_shouldThrow() {
        when(repository.findById(any())).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.getApplicationById(UUID.randomUUID()));
    }

    @Test
    void getApplicationsByStatus_invalid_shouldThrow() {
        assertThrows(BadRequestException.class,
                () -> service.getApplicationsByStatus("invalid"));
    }
}