package com.example.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.dto.request.ApplicationDecisionRequest;
import com.example.dto.request.CreditCardApplicationRequest;
import com.example.dto.response.CreditCardApplicationResponse;
import com.example.dto.response.CreditCardApplicationSummaryResponse;
import com.example.entity.*;
import com.example.enums.*;
import com.example.exception.*;
import com.example.mapper.CreditAccountApplicationMapper;
import com.example.repository.CreditCardApplicationRepository;
import com.example.service.ServiceImpl.CreditAccountApplicationServiceImpl;
import com.example.testutil.TestFixtures;
import com.example.underwriting.UnderwritingService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class CreditAccountApplicationServiceImplTest {

    @Mock private CreditCardApplicationRepository repository;
    @Mock private CustomerService customerService;
    @Mock private KycService kycService;
    @Mock private CreditProductService creditProductService;
    @Mock private CreditAccountService creditAccountService;
    @Mock private ActiveAccountChecker activeAccountChecker;
    @Mock private UnderwritingService underwritingService;
    @Mock private CreditAccountApplicationMapper mapper;

    @InjectMocks
    private CreditAccountApplicationServiceImpl service;

    private UUID userId;
    private Customer customer;
    private CreditProduct product;
    private CreditCardApplicationRequest request;

    @BeforeEach
    void setup() {
        userId = UUID.randomUUID();
        customer = TestFixtures.validCustomer();
        product = TestFixtures.validCreditProductEntity();
        request = TestFixtures.validApplicationRequest(product.getCreditProductId());
    }

    // ================= APPLY =================

    @Nested
    @DisplayName("Apply Credit Card")
    class ApplyTests {

        @Test
        void shouldApplySuccessfully_whenAutoApproved() {
            // GIVEN
            when(customerService.getCustomerByUserId(userId)).thenReturn(customer);
            when(kycService.isKycVerified(customer.getCustomerId())).thenReturn(true);
            when(creditProductService.getActiveCreditProduct(any())).thenReturn(product);

            when(repository.existsByCustomerCustomerIdAndCreditProductCreditProductIdAndApplicationStatusIn(any(), any(), any()))
                    .thenReturn(false);

            when(repository.countByCustomerCustomerIdAndApplicationStatusIn(any(), any()))
                    .thenReturn(0);

            when(repository.existsByCustomerCustomerIdAndCreditProductCreditProductIdAndApplicationStatusAndSubmittedAtAfter(any(), any(), any(), any()))
                    .thenReturn(false);

            when(activeAccountChecker.hasActiveAccount(any(), any())).thenReturn(false);

            when(underwritingService.evaluate(any()))
                    .thenReturn(TestFixtures.approvedDecision());

            CreditCardApplication saved = TestFixtures.validApplication(customer, product);
            saved.setDecision(DecisionType.AUTO_APPROVED);

            when(repository.save(any())).thenReturn(saved);

            CreditCardApplicationSummaryResponse response = mock(CreditCardApplicationSummaryResponse.class);
            when(mapper.toSummaryResponse(saved)).thenReturn(response);

            // WHEN
            CreditCardApplicationSummaryResponse result = service.apply(userId, request);

            // THEN
            assertThat(result).isNotNull();
            verify(creditAccountService).createAccount(saved);
        }

        @Test
        void shouldNotCreateAccount_whenAutoRejected() {
            // GIVEN
            setupValidFlow();

            when(underwritingService.evaluate(any()))
                    .thenReturn(TestFixtures.rejectedDecision());

            when(repository.save(any()))
                    .thenReturn(TestFixtures.validApplication(customer, product));

            when(mapper.toSummaryResponse(any()))
                    .thenReturn(mock(CreditCardApplicationSummaryResponse.class));

            // WHEN
            service.apply(userId, request);

            // THEN
            verify(creditAccountService, never()).createAccount(any());
        }

        @Test
        void shouldThrowException_whenKycNotVerified() {
            when(customerService.getCustomerByUserId(userId)).thenReturn(customer);
            when(kycService.isKycVerified(customer.getCustomerId())).thenReturn(false);

            assertThatThrownBy(() -> service.apply(userId, request))
                    .isInstanceOf(BusinessRuleException.class);
        }

        @Test
        void shouldThrowException_whenDuplicateApplicationExists() {
            setupBasic();

            when(repository.existsByCustomerCustomerIdAndCreditProductCreditProductIdAndApplicationStatusIn(any(), any(), any()))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.apply(userId, request))
                    .isInstanceOf(ConflictException.class);
        }

        @Test
        void shouldThrowException_whenCreditScoreInvalid() {
            setupBasic();

            request.setCreditScoreAtApplication(100);

            assertThatThrownBy(() -> service.apply(userId, request))
                    .isInstanceOf(BusinessRuleException.class);
        }

        @Test
        void shouldThrowException_whenActiveApplicationLimitExceeded() {
            setupBasic();

            when(repository.existsByCustomerCustomerIdAndCreditProductCreditProductIdAndApplicationStatusIn(any(), any(), any()))
                    .thenReturn(false);

            when(repository.countByCustomerCustomerIdAndApplicationStatusIn(any(), any()))
                    .thenReturn(3);

            assertThatThrownBy(() -> service.apply(userId, request))
                    .isInstanceOf(BusinessRuleException.class);
        }

        @Test
        void shouldThrowException_whenRejectionCooldownActive() {
            setupBasic();

            when(repository.existsByCustomerCustomerIdAndCreditProductCreditProductIdAndApplicationStatusIn(any(), any(), any()))
                    .thenReturn(false);

            when(repository.countByCustomerCustomerIdAndApplicationStatusIn(any(), any()))
                    .thenReturn(0);

            when(repository.existsByCustomerCustomerIdAndCreditProductCreditProductIdAndApplicationStatusAndSubmittedAtAfter(any(), any(), any(), any()))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.apply(userId, request))
                    .isInstanceOf(BusinessRuleException.class);
        }

        @Test
        void shouldThrowException_whenActiveAccountExists() {
            setupValidFlow();

            when(activeAccountChecker.hasActiveAccount(any(), any())).thenReturn(true);

            assertThatThrownBy(() -> service.apply(userId, request))
                    .isInstanceOf(BusinessRuleException.class);
        }

        @Test
        void shouldThrowException_whenEmployerMissing() {
            setupBasic();
            request.setEmployerName("");

            assertThatThrownBy(() -> service.apply(userId, request))
                    .isInstanceOf(BusinessRuleException.class);
        }
    }

    // ================= GET =================

    @Test
    void shouldReturnCustomerApplications() {
        when(customerService.getCustomerByUserId(userId)).thenReturn(customer);
        when(repository.findAllByCustomerCustomerId(customer.getCustomerId()))
                .thenReturn(List.of(new CreditCardApplication()));

        when(mapper.toSummaryResponse(any()))
                .thenReturn(mock(CreditCardApplicationSummaryResponse.class));

        List<CreditCardApplicationSummaryResponse> result =
                service.getCustomerApplications(userId);

        assertThat(result).hasSize(1);
    }

    @Test
    void shouldThrowException_whenInvalidStatus() {
        assertThatThrownBy(() ->
                service.getApplicationsByStatus("INVALID"))
                .isInstanceOf(BadRequestException.class);
    }

    // ================= GET BY ID =================

    @Test
    void shouldThrowAccessDenied_whenNotOwner() {
        Customer another = TestFixtures.validCustomer();

        CreditCardApplication app = TestFixtures.validApplication(another, product);

        when(customerService.getCustomer(customer.getCustomerId())).thenReturn(customer);
        when(repository.findById(app.getApplicationId())).thenReturn(Optional.of(app));

        assertThatThrownBy(() ->
                service.getCustomerApplicationById(customer.getCustomerId(), app.getApplicationId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ================= DECISION =================

    @Test
    void shouldApproveApplication_manually() {
        CreditCardApplication app = TestFixtures.validApplication(customer, product);
        app.setApplicationStatus(ApplicationStatus.PENDING_REVIEW);

        when(repository.findById(app.getApplicationId())).thenReturn(Optional.of(app));
        when(repository.save(app)).thenReturn(app);

        ApplicationDecisionRequest req = TestFixtures.approveDecisionRequest();

        when(mapper.toResponse(app)).thenReturn(mock(CreditCardApplicationResponse.class));

        CreditCardApplicationResponse result =
                service.decide(app.getApplicationId(), req);

        assertThat(result).isNotNull();
    }

    @Test
    void shouldRejectApplication_manually() {
        CreditCardApplication app = TestFixtures.validApplication(customer, product);
        app.setApplicationStatus(ApplicationStatus.PENDING_REVIEW);

        when(repository.findById(app.getApplicationId())).thenReturn(Optional.of(app));
        when(repository.save(app)).thenReturn(app);

        ApplicationDecisionRequest req = TestFixtures.rejectDecisionRequest();

        when(mapper.toResponse(app)).thenReturn(mock(CreditCardApplicationResponse.class));

        service.decide(app.getApplicationId(), req);

        assertThat(app.getApplicationStatus()).isEqualTo(ApplicationStatus.REJECTED);
    }

    @Test
    void shouldThrowException_whenNotPendingReview() {
        CreditCardApplication app = TestFixtures.validApplication(customer, product);
        app.setApplicationStatus(ApplicationStatus.APPROVED);

        when(repository.findById(app.getApplicationId())).thenReturn(Optional.of(app));

        assertThatThrownBy(() ->
                service.decide(app.getApplicationId(), TestFixtures.approveDecisionRequest()))
                .isInstanceOf(BusinessRuleException.class);
    }
    
    @Test
    void shouldReturnApplicationsByStatus() {
        // GIVEN
        UUID userId = UUID.randomUUID();
        Customer customer = TestFixtures.validCustomer();

        CreditCardApplication app = new CreditCardApplication();

        when(customerService.getCustomerByUserId(userId)).thenReturn(customer);

        when(repository.findAllByCustomerCustomerIdAndApplicationStatus(
                eq(customer.getCustomerId()),
                eq(ApplicationStatus.APPROVED)))
                .thenReturn(List.of(app));

        when(mapper.toSummaryResponse(app))
                .thenReturn(mock(CreditCardApplicationSummaryResponse.class));

        // WHEN
        List<CreditCardApplicationSummaryResponse> result =
                service.getCustomerApplicationsByStatus(userId, "APPROVED");

        // THEN
        assertThat(result).hasSize(1);
    }
    @Test
    void shouldReturnEmptyList_whenNoApplicationsFoundByStatus() {
        // GIVEN
        UUID userId = UUID.randomUUID();
        Customer customer = TestFixtures.validCustomer();

        when(customerService.getCustomerByUserId(userId)).thenReturn(customer);

        when(repository.findAllByCustomerCustomerIdAndApplicationStatus(
                any(), any()))
                .thenReturn(List.of());

        // WHEN
        List<CreditCardApplicationSummaryResponse> result =
                service.getCustomerApplicationsByStatus(userId, "APPROVED");

        // THEN
        assertThat(result).isEmpty();
    }
    @Test
    void shouldThrowException_whenInvalidStatusInCustomerFilter() {
        // GIVEN
        UUID userId = UUID.randomUUID();
        Customer customer = TestFixtures.validCustomer();

        when(customerService.getCustomerByUserId(userId)).thenReturn(customer);

        // WHEN + THEN
        assertThatThrownBy(() ->
                service.getCustomerApplicationsByStatus(userId, "INVALID_STATUS"))
                .isInstanceOf(BadRequestException.class);
    }

    // ================= HELPERS =================

    private void setupBasic() {
        when(customerService.getCustomerByUserId(userId)).thenReturn(customer);
        when(kycService.isKycVerified(customer.getCustomerId())).thenReturn(true);
        when(creditProductService.getActiveCreditProduct(any())).thenReturn(product);
    }

    private void setupValidFlow() {
        setupBasic();

        when(repository.existsByCustomerCustomerIdAndCreditProductCreditProductIdAndApplicationStatusIn(any(), any(), any()))
                .thenReturn(false);

        when(repository.countByCustomerCustomerIdAndApplicationStatusIn(any(), any()))
                .thenReturn(0);

        when(repository.existsByCustomerCustomerIdAndCreditProductCreditProductIdAndApplicationStatusAndSubmittedAtAfter(any(), any(), any(), any()))
                .thenReturn(false);
    }
}