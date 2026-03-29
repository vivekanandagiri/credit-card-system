package com.example.service.ServiceImpl;

import com.example.dto.request.ApplicationDecisionRequest;
import com.example.dto.request.CreditCardApplicationRequest;
import com.example.dto.response.CreditCardApplicationSummaryResponse;
import com.example.dto.response.CreditCardApplicationResponse;
import com.example.entity.CreditCardApplication;
import com.example.entity.CreditProduct;
import com.example.entity.Customer;
import com.example.enums.*;
import com.example.exception.BadRequestException;
import com.example.exception.BusinessRuleException;
import com.example.exception.ConflictException;
import com.example.exception.ResourceNotFoundException;
import com.example.mapper.CreditAccountApplicationMapper;
import com.example.repository.*;
import com.example.service.ActiveAccountChecker;
import com.example.service.CreditAccountService;
import com.example.service.CreditProductService;
import com.example.service.CustomerService;
import com.example.service.KycService;
import com.example.service.CreditAccountApplicationService;
import com.example.underwriting.UnderwritingService;
import com.example.underwriting.model.UnderwritingDecision;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of {@link CreditAccountApplicationService}.
 *
 */
@Service
@Transactional
public class CreditAccountApplicationServiceImpl implements CreditAccountApplicationService {
	
	// Maximum active applications a customer can have at any time
    private static final int MAX_ACTIVE_APPLICATIONS = 3;
    // Cool down period in days after rejection before re-applying for same product
    private static final int REJECTION_COOLDOWN_DAYS = 30;
    
    private static final int CREDIT_SCORE_MIN = 300;
    private static final int CREDIT_SCORE_MAX = 900;
    
    private static final List<ApplicationStatus> ACTIVE_APPLICATION_STATUSES = List.of(
            ApplicationStatus.SUBMITTED,
            ApplicationStatus.UNDER_REVIEW,
            ApplicationStatus.PENDING_REVIEW);
    
    private final CreditCardApplicationRepository applicationRepository;
    private final CustomerService customerService;
    private final KycService kycService;
    private final CreditProductService creditProductService;
    private final CreditAccountService creditAccountService;
    private final ActiveAccountChecker activeAccountChecker;
    private final UnderwritingService underwritingService;
    private final CreditAccountApplicationMapper applicationMapper;
 
    public CreditAccountApplicationServiceImpl(
            CreditCardApplicationRepository applicationRepository,
            CustomerService customerService,
            KycService kycService,
            CreditProductService creditProductService,
            CreditAccountService creditAccountService,
            ActiveAccountChecker activeAccountChecker,
            UnderwritingService underwritingService,
            CreditAccountApplicationMapper applicationMapper) {
        this.applicationRepository = applicationRepository;
        this.customerService = customerService;
        this.kycService = kycService;
        this.creditProductService = creditProductService;
        this.creditAccountService = creditAccountService;
        this.activeAccountChecker = activeAccountChecker;
        this.underwritingService = underwritingService;
        this.applicationMapper = applicationMapper;
    }


	/**
	 * CREATE APPLICATION
	 */
	@Override
	public CreditCardApplicationSummaryResponse apply(UUID userId, CreditCardApplicationRequest request) {
		// 1. User and Profile check
		Customer customer = customerService.getCustomerByUserId(userId);
		
		// 4. KYC check
		validateKycApproved(customer.getCustomerId());
		// 3.Credit product validation check
		CreditProduct creditProduct = creditProductService.getActiveCreditProduct(request.getCreditProductId());
		// 5.Credit score validation check
		validateCreditScore(request.getCreditScoreAtApplication());
		validateNoDuplicateActiveApplication(customer, creditProduct);
		validateActiveApplicationLimit(customer);
		validateEmploymentDetails(request);
		validateRejectionCooldown(customer, creditProduct);
		validateNoActiveAccount(customer, creditProduct);
		
	
		

		// . Build application entity
		
		CreditCardApplication application = buildApplication(request, customer, creditProduct);

		// Run underwriting on unsaved entity 
        // ApplicationContext only reads field values
        UnderwritingDecision decision = underwritingService.evaluate(application);

        // Apply decision to entity 
        applyDecisionToApplication(application, decision);

        // Single save with final decided state ──
        CreditCardApplication saved = applicationRepository.save(application);
        
        // ── Auto-create account if approved ──
        if (saved.getDecision() == DecisionType.AUTO_APPROVED) {
            creditAccountService.createAccount(saved);
        }

        return applicationMapper.toSummaryResponse(saved);
	}

	/**
	 *  GET Customer  Application
	 */

	@Override
	@Transactional(readOnly = true)
	public List<CreditCardApplicationSummaryResponse> getCustomerApplications(UUID userId) {

		Customer customer = customerService.getCustomerByUserId(userId);

		List<CreditCardApplicationSummaryResponse> list = applicationRepository
				.findAllByCustomerCustomerId(customer.getCustomerId()).stream().map(applicationMapper::toSummaryResponse)
				.collect(Collectors.toList());

		return list;
	}
	
	/*
	 * Get application by status(Customer)
	 */
	@Override
	@Transactional(readOnly = true)
	public List<CreditCardApplicationSummaryResponse> getCustomerApplicationsByStatus(
	        UUID userId, String status) {

		Customer customer = customerService.getCustomerByUserId(userId);
	    ApplicationStatus applicationStatus = parseApplicationStatus(status);
	    
	    List<CreditCardApplicationSummaryResponse> list = applicationRepository
	            .findAllByCustomerCustomerIdAndApplicationStatus(
	                    customer.getCustomerId(), applicationStatus)
	            .stream()
	            .map(applicationMapper::toSummaryResponse)
	            .collect(Collectors.toList());

	    return list;
	}

	/**
	 *  GET CUSTOMER APPLICATION BY ID
	 */
	@Override
	@Transactional(readOnly = true)
	public CreditCardApplicationResponse getCustomerApplicationById(UUID customerId, UUID applicationId) {
		Customer customer = customerService.getCustomer(customerId);

		CreditCardApplication application = findApplicationById(applicationId);
		
		if (!application.getCustomer().getCustomerId().equals(customer.getCustomerId())) {
			throw new AccessDeniedException("Access denied to this application");
		}

		return applicationMapper.toResponse(application);
	}
	
	/**
	 * GET Application By Id 
	 */
	@Override
	@Transactional(readOnly = true)
	public CreditCardApplicationResponse getApplicationById(UUID applicationId) {

	    CreditCardApplication application = findApplicationById(applicationId);
	    return applicationMapper.toResponse(application);
	}

	/**
	 *  GET ALL APPLICATIONS (Admin)
	 */
	@Override
	@Transactional(readOnly = true)
	public List<CreditCardApplicationSummaryResponse> getAllApplications() {

		List<CreditCardApplicationSummaryResponse> list = applicationRepository.findAll().stream()
				.map(applicationMapper::toSummaryResponse).collect(Collectors.toList());

		return  list;
	}

	/**
	 *  GET APPLICATIONS BY STATUS (Admin)
	 */
	@Override
	@Transactional(readOnly = true)
	public List<CreditCardApplicationSummaryResponse> getApplicationsByStatus(String status) {

		ApplicationStatus applicationStatus=parseApplicationStatus(status);
		
		List<CreditCardApplicationSummaryResponse> list = applicationRepository.findAllByApplicationStatus(applicationStatus)
				.stream().map(applicationMapper::toSummaryResponse).collect(Collectors.toList());

		return list;
	}

	
	/**
	 *  MANUAL DECISION (Admin — PENDING_REVIEW only)
	 */
	@Override
	public CreditCardApplicationResponse decide(UUID applicationId, ApplicationDecisionRequest request) {

	    CreditCardApplication application = findApplicationById(applicationId);
	    
	    if (application.getApplicationStatus() != ApplicationStatus.PENDING_REVIEW) {
	        throw new BusinessRuleException("Only PENDING_REVIEW applications can be manually decided");
	    }
	    //manual decision
	    
	    applyManualDecision(application, request);

	    CreditCardApplication saved = applicationRepository.save(application);

	    // Duplicate account check  
        if (saved.getApplicationStatus() == ApplicationStatus.APPROVED) {
            createAccountGuarded(saved);
        }
        
	    return applicationMapper.toResponse(saved);
	}
	
	// Private Helpers Methods
	//---------------------------------------------------------------------------
    // 1. Private helpers — validation
 
    private void validateKycApproved(UUID customerId) {
        // Delegates to KycService — no direct kycRepository access
        if (!kycService.isKycVerified(customerId)) {
            throw new BusinessRuleException("KYC must be verified before applying for a credit card");
        }
    }
 
    private void validateCreditScore(int score) {
        if (score < CREDIT_SCORE_MIN || score > CREDIT_SCORE_MAX) {
            throw new BusinessRuleException(
                    "Credit score must be between " + CREDIT_SCORE_MIN + " and " + CREDIT_SCORE_MAX);
        }
    }
 
    private void validateNoDuplicateActiveApplication(Customer customer, CreditProduct creditProduct) {
        boolean exists = applicationRepository
                .existsByCustomerCustomerIdAndCreditProductCreditProductIdAndApplicationStatusIn(
                        customer.getCustomerId(), creditProduct.getCreditProductId(), ACTIVE_APPLICATION_STATUSES);
        if (exists) {
            throw new ConflictException("You already have an active application for this credit product");
        }
    }
 
    private void validateActiveApplicationLimit(Customer customer) {
        int count = applicationRepository.countByCustomerCustomerIdAndApplicationStatusIn(
                customer.getCustomerId(), ACTIVE_APPLICATION_STATUSES);
        if (count >= MAX_ACTIVE_APPLICATIONS) {
            throw new BusinessRuleException(
                    "Maximum " + MAX_ACTIVE_APPLICATIONS + " active applications allowed at a time. "
                            + "Please wait for your existing applications to be decided.");
        }
    }
 
    private void validateRejectionCooldown(Customer customer, CreditProduct creditProduct) {
        Instant cooldownStart = Instant.now().minus(REJECTION_COOLDOWN_DAYS, ChronoUnit.DAYS);
        boolean recentlyRejected = applicationRepository
                .existsByCustomerCustomerIdAndCreditProductCreditProductIdAndApplicationStatusAndSubmittedAtAfter(
                        customer.getCustomerId(), creditProduct.getCreditProductId(),
                        ApplicationStatus.REJECTED, cooldownStart);
        if (recentlyRejected) {
            throw new BusinessRuleException(
                    "You cannot re-apply for this card product within "
                            + REJECTION_COOLDOWN_DAYS + " days of a rejection");
        }
    }
 
    private void validateNoActiveAccount(Customer customer, CreditProduct creditProduct) {
        if (activeAccountChecker.hasActiveAccount(customer.getCustomerId(), creditProduct.getCreditProductId())) {
            throw new BusinessRuleException(
                    "You already have an active credit account for this product. Cannot apply again.");
        }
    }
 
    private void validateEmploymentDetails(CreditCardApplicationRequest request) {
        if (request.getEmploymentType() == EmploymentType.SALARIED
                && (request.getEmployerName() == null || request.getEmployerName().isBlank())) {
            throw new BusinessRuleException("Employer name is required for salaried applicants");
        }
    }
 
    // Private helpers — entity building + decision application
 
    private CreditCardApplication buildApplication(
            CreditCardApplicationRequest request, Customer customer, CreditProduct creditProduct) {
        CreditCardApplication app = new CreditCardApplication();
        app.setCustomer(customer);
        app.setCreditProduct(creditProduct);
        app.setEmploymentType(request.getEmploymentType());
        app.setEmployerName(request.getEmployerName());
        app.setMonthlyIncome(request.getMonthlyIncome());
        app.setExistingLiabilities(request.getExistingLiabilities());
        app.setCreditScoreAtApplication(request.getCreditScoreAtApplication());
        app.setRequestedCreditLimit(request.getRequestedCreditLimit());
        app.setApplicationStatus(ApplicationStatus.UNDER_REVIEW);
        app.setSubmittedAt(Instant.now());
        return app;
    }
 
    private void applyDecisionToApplication(CreditCardApplication app, UnderwritingDecision decision) {
        app.setRiskScore(decision.getRiskScore());
        app.setDecision(decision.getDecision());
        app.setDecisionReason(decision.getDecisionReason());
        app.setDecisionAt(Instant.now());
        switch (decision.getDecision()) {
            case AUTO_APPROVED -> {
                app.setApplicationStatus(ApplicationStatus.APPROVED);
                app.setApprovedCreditLimit(decision.getApprovedLimit());
                app.setApprovedApr(decision.getApprovedApr());
            }
            case AUTO_REJECTED -> app.setApplicationStatus(ApplicationStatus.REJECTED);
            case PENDING_REVIEW -> app.setApplicationStatus(ApplicationStatus.PENDING_REVIEW);
            default -> app.setApplicationStatus(ApplicationStatus.UNDER_REVIEW);
        }
    }
 
    private void applyManualDecision(CreditCardApplication app, ApplicationDecisionRequest request) {
        if (request.isApproved()) {
            if (request.getApprovedCreditLimit() == null || request.getApprovedApr() == null) {
                throw new BusinessRuleException("Approved credit limit and APR are required for approval");
            }
            app.setApplicationStatus(ApplicationStatus.APPROVED);
            app.setDecision(DecisionType.MANUALLY_APPROVED);
            app.setApprovedCreditLimit(request.getApprovedCreditLimit());
            app.setApprovedApr(request.getApprovedApr());
        } else {
            app.setApplicationStatus(ApplicationStatus.REJECTED);
            app.setDecision(DecisionType.MANUALLY_REJECTED);
        }
        app.setDecisionReason(request.getDecisionReason());
        app.setDecisionAt(Instant.now());
    }
 
    private void createAccountGuarded(CreditCardApplication saved) {
        if (creditAccountService.accountExistsForApplication(saved.getApplicationId())) {
            throw new ConflictException("Credit account already exists for this application");
        }
        try {
            creditAccountService.createAccount(saved);
        } catch (Exception exception) {
            throw new BusinessRuleException("Application approved but account creation failed: " + exception.getMessage());
        }
    }
 
    private CreditCardApplication findApplicationById(UUID applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Application with id " + applicationId + " not found"));
    }
 
    private ApplicationStatus parseApplicationStatus(String status) {
        try {
            return ApplicationStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Invalid application status: " + status);
        }
    }
}