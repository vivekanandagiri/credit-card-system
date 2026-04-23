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
 * Service implementation for managing credit card applications.
 * Implementation of {@link CreditAccountApplicationService}.
 *
 * <p>This class handles the complete lifecycle of a credit card application:
 * validation, underwriting decisioning, persistence, and account creation.</p>
 *
 * <p>Key responsibilities:</p>
 * <ul>
 *     <li>Validating customer eligibility (KYC, credit score, duplicates, limits)</li>
 *     <li>Processing application submission</li>
 *     <li>Interfacing with underwriting engine</li>
 *     <li>Handling manual and automatic decisions</li>
 *     <li>Creating credit accounts upon approval</li>
 * </ul>
 *
 * <p>Transactional boundaries are managed at the service level.</p>
 */
@Service
@Transactional
public class CreditAccountApplicationServiceImpl implements CreditAccountApplicationService {
	
	// Maximum active applications a customer can have at any time
    private static final int MAX_ACTIVE_APPLICATIONS = 3;
    // Cool down period in days after rejection before re-applying for same product
    private static final int REJECTION_COOLDOWN_DAYS = 30;
    /**
     * Minimum allowed credit score.
     * Maximum allowed credit score.
     */
    private static final int CREDIT_SCORE_MIN = 300;
    private static final int CREDIT_SCORE_MAX = 900;
    
    // List of application statuses considered as "active".

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
 
    /**
     * Constructs the service with required dependencies.
     *
     * @param applicationRepository repository for application persistence
     * @param customerService       service to fetch customer details
     * @param kycService            service to verify KYC status
     * @param creditProductService  service to fetch credit product details
     * @param creditAccountService  service to create credit accounts
     * @param activeAccountChecker  service to check existing active accounts
     * @param underwritingService   service to evaluate credit risk
     * @param applicationMapper     mapper for entity ↔ DTO conversion
     */
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
     * Submits a new credit card application.
     *
     * <p>Performs multiple validations including KYC, credit score, duplicate applications,
     * and business rules before sending the application for underwriting.</p>
     *
     * <p>If auto-approved, a credit account is created immediately.</p>
     *
     * @param userId  the user ID of the applicant
     * @param request the application request payload
     * @return summary response of the created application
     */
	@Override
	public CreditCardApplicationSummaryResponse apply(UUID userId, CreditCardApplicationRequest request) {
		// 1. User and Profile check
		Customer customer = customerService.getCustomerByUserId(userId);
		// 2. KYC check
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
     * Retrieves all applications for a given user.
     *
     * @param userId user identifier
     * @return list of application summaries
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
	
	/**
     * Retrieves applications filtered by status for a customer.
     *
     * @param userId user identifier
     * @param status application status (string)
     * @return filtered list of applications
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
     * Retrieves a specific application belonging to a customer.
     *
     * @param customerId    customer ID
     * @param applicationId application ID
     * @return application details
     * @throws AccessDeniedException if application does not belong to the customer
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
     * Retrieves an application by ID (admin/internal use).
     *
     * @param applicationId application ID
     * @return application details
     */
	@Override
	@Transactional(readOnly = true)
	public CreditCardApplicationResponse getApplicationById(UUID applicationId) {

	    CreditCardApplication application = findApplicationById(applicationId);
	    return applicationMapper.toResponse(application);
	}

	/**
     * Retrieves all applications (admin).
     *
     * @return list of all application summaries
     */
	@Override
	@Transactional(readOnly = true)
	public List<CreditCardApplicationSummaryResponse> getAllApplications() {

		List<CreditCardApplicationSummaryResponse> list = applicationRepository.findAll().stream()
				.map(applicationMapper::toSummaryResponse).collect(Collectors.toList());

		return  list;
	}

	/**
     * Retrieves applications filtered by status (admin).
     *
     * @param status application status
     * @return filtered list
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
	 * Performs manual decision on an application (admin only).
     *
     * <p>Only applications in {@code PENDING_REVIEW} state can be manually decided.</p>
     *
     * @param applicationId application ID
     * @param request       decision request
     * @return updated application response
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
 
	 /**
     * Validates whether KYC is approved for the customer.
     */
    private void validateKycApproved(UUID customerId) {
        // Delegates to KycService — no direct kycRepository access
        if (!kycService.isKycVerified(customerId)) {
            throw new BusinessRuleException("KYC must be verified before applying for a credit card");
        }
    }
 
    /**
     * Validates the credit score range.
     */
    private void validateCreditScore(int score) {
        if (score < CREDIT_SCORE_MIN || score > CREDIT_SCORE_MAX) {
            throw new BusinessRuleException(
                    "Credit score must be between " + CREDIT_SCORE_MIN + " and " + CREDIT_SCORE_MAX);
        }
    }
 
    /**
     * Validates that the customer does not already have an active application
     * for the same credit product.
     *
     * @param customer      the customer applying
     * @param creditProduct the credit product being applied for
     * @throws ConflictException if an active application already exists
     */
    private void validateNoDuplicateActiveApplication(Customer customer, CreditProduct creditProduct) {
        boolean exists = applicationRepository
                .existsByCustomerCustomerIdAndCreditProductCreditProductIdAndApplicationStatusIn(
                        customer.getCustomerId(), creditProduct.getCreditProductId(), ACTIVE_APPLICATION_STATUSES);
        if (exists) {
            throw new ConflictException("You already have an active application for this credit product");
        }
    }
    
    /**
     * Ensures the customer has not exceeded the maximum allowed active applications.
     *
     * @param customer the customer
     * @throws BusinessRuleException if the limit is exceeded
     */
    private void validateActiveApplicationLimit(Customer customer) {
        int count = applicationRepository.countByCustomerCustomerIdAndApplicationStatusIn(
                customer.getCustomerId(), ACTIVE_APPLICATION_STATUSES);
        if (count >= MAX_ACTIVE_APPLICATIONS) {
            throw new BusinessRuleException(
                    "Maximum " + MAX_ACTIVE_APPLICATIONS + " active applications allowed at a time. "
                            + "Please wait for your existing applications to be decided.");
        }
    }
 
    /**
     * Prevents re-application for the same credit product within the cool down period
     * after a rejection.
     *
     * @param customer      the customer
     * @param creditProduct the credit product
     * @throws BusinessRuleException if within cool down period
     */
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
 
    /**
     * Validates that the customer does not already hold an active account
     * for the given credit product.
     *
     * @param customer      the customer
     * @param creditProduct the credit product
     * @throws BusinessRuleException if an active account already exists
     */
    private void validateNoActiveAccount(Customer customer, CreditProduct creditProduct) {
        if (activeAccountChecker.hasActiveAccount(customer.getCustomerId(), creditProduct.getCreditProductId())) {
            throw new BusinessRuleException(
                    "You already have an active credit account for this product. Cannot apply again.");
        }
    }
 
    /**
     * Validates employment-related details in the application request.
     *
     * <p>For salaried applicants, employer name is mandatory.</p>
     *
     * @param request the application request
     * @throws BusinessRuleException if required fields are missing
     */
    private void validateEmploymentDetails(CreditCardApplicationRequest request) {
        if (request.getEmploymentType() == EmploymentType.SALARIED
                && (request.getEmployerName() == null || request.getEmployerName().isBlank())) {
            throw new BusinessRuleException("Employer name is required for salaried applicants");
        }
    }
 
    /**
     * Builds a {@link CreditCardApplication} entity from request data.
     *
     * @param request       the application request
     * @param customer      the customer entity
     * @param creditProduct the selected credit product
     * @return populated application entity (not yet persisted)
     */
 
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
 
    /**
     * Applies System underwriting decision results to the application entity.
     *
     * @param app      the application entity
     * @param decision the underwriting decision result
     */
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
 
    /**
     * Applies a manual decision (approval/rejection) to an application.
     *
     * @param app     the application entity
     * @param request the decision request
     * @throws BusinessRuleException if required approval fields are missing
     */
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
 
    /**
     * Safely creates a credit account for an approved application.
     *
     * <p>Prevents duplicate account creation and wraps failures into business exceptions.</p>
     *
     * @param saved the approved application
     * @throws ConflictException if account already exists
     * @throws BusinessRuleException if account creation fails
     */
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
 
    /**
     * Retrieves an application by ID.
     *
     * @param applicationId the application ID
     * @return the application entity
     * @throws ResourceNotFoundException if not found
     */
    private CreditCardApplication findApplicationById(UUID applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Application with id " + applicationId + " not found"));
    }
    
    /**
     * Converts a string representation of status into {@link ApplicationStatus}.
     *
     * @param status the status string
     * @return parsed enum value
     * @throws BadRequestException if invalid status is provided
     */
    private ApplicationStatus parseApplicationStatus(String status) {
        try {
            return ApplicationStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Invalid application status: " + status);
        }
    }
}