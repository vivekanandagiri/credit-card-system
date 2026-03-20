package com.example.service.ServiceImpl;

import com.example.dto.request.ApplicationDecisionRequest;
import com.example.dto.request.CreditCardApplicationRequest;
import com.example.dto.response.ApiResponse;
import com.example.dto.response.CreditCardApplicationCreateResponse;
import com.example.dto.response.CreditCardApplicationResponse;
import com.example.entity.CreditCardApplication;
import com.example.entity.CreditProduct;
import com.example.entity.Customer;
import com.example.entity.User;
import com.example.enums.*;
import com.example.exception.BadRequestException;
import com.example.exception.BusinessRuleException;
import com.example.exception.ConflictException;
import com.example.exception.ProfileNotCreatedException;
import com.example.exception.ResourceNotFoundException;
import com.example.exception.UserNotFoundException;
import com.example.mapper.CreditAccountApplicationMapper;
import com.example.repository.*;
import com.example.service.ActiveAccountChecker;
import com.example.service.CreditAccountService;
import com.example.service.CreditAccountApplicationService;
import com.example.underwriting.UnderwritingService;
import com.example.underwriting.model.UnderwritingDecision;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class CreditAccountApplicationServiceImpl implements CreditAccountApplicationService {

	
	// Maximum active applications a customer can have at any time
    private static final int MAX_ACTIVE_APPLICATIONS = 3;

    // Cool down period in days after rejection before re-applying for same product
    private static final int REJECTION_COOLDOWN_DAYS = 30;
    
	private final CreditCardApplicationRepository applicationRepository;
	private final CreditProductRepository creditProductRepository;
	private final KycRepository kycRepository;
	private final UserRepository userRepository;
	private final CreditAccountApplicationMapper applicationMapper;
	private final UnderwritingService underwritingService;
	private final ActiveAccountChecker activeAccountChecker;
	private final CreditAccountService creditAccountService;
	private final CreditAccountRepository creditAccountRepository;

	public CreditAccountApplicationServiceImpl(CreditCardApplicationRepository applicationRepository,
			CreditProductRepository creditProductRepository, KycRepository kycRepository,
			UserRepository userRepository, CreditAccountApplicationMapper applicationMapper,
			 UnderwritingService underwritingService, ActiveAccountChecker activeAccountChecker, CreditAccountService creditAccountService, CreditAccountRepository creditAccountRepository) {

		this.applicationRepository = applicationRepository;
		this.creditProductRepository = creditProductRepository;
		this.kycRepository = kycRepository;
		this.userRepository = userRepository;
		this.applicationMapper = applicationMapper;
		this.underwritingService = underwritingService;
		this.activeAccountChecker = activeAccountChecker;
		this.creditAccountService = creditAccountService;
		this.creditAccountRepository = creditAccountRepository;
	}


	// GET AVAILABLE CARD PRODUCTS
//	@Override
//    @Transactional(readOnly = true)
//	public ApiResponse<List<CreditProductResponse>> getAvailableCreditProducts() {
//		 
//        List<CreditProduct> products = creditProductRepository
//                .findAllByStatus(ProductStatus.ACTIVE);
// 
//        return ApiResponse( HttpStatus.OK.value(),
//                "Available credit products fetched successfully", products);
//    }


	// CREATE APPLICATION
	@Override
	public ApiResponse<CreditCardApplicationCreateResponse> apply(UUID userId, CreditCardApplicationRequest request) {
		
		// 1. Load user
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new UserNotFoundException("User with id " + userId + " not found"));

		// 2. Profile check
		Customer customer = user.getCustomer();
		if (customer == null) {
			throw new ProfileNotCreatedException("Complete your profile before applying for a credit card");
		}

		// 3. KYC check
		boolean kycApproved = kycRepository.findByCustomerCustomerId(customer.getCustomerId()).stream()
				.anyMatch(kycRepository -> kycRepository.getStatus() == KycStatus.VERIFIED);

		if (!kycApproved) {
			throw new BusinessRuleException("Kyc must be verified before applying the credoy");
		}
		// 4.Credit score validation check
		
		if (request.getCreditScoreAtApplication() < 300 
		        || request.getCreditScoreAtApplication() > 900) {
		    throw new BusinessRuleException("Credit score must be between 300 and 900");
		}
		
		// 5.Credit product validation check
		CreditProduct creditProduct = creditProductRepository
	                .findById(request.getCreditProductId())
	                .orElseThrow(() ->
	                        new ResourceNotFoundException("Credit product with id "
	                                + request.getCreditProductId() + " not found"));

		if (creditProduct.getStatus() == ProductStatus.INACTIVE) {
			throw new ResourceNotFoundException("Selected credit product is no longer available");
		}
		
		// 6. Duplicate application check(there should be No active application for same credit product)
		boolean activeApplicationExists = applicationRepository
				.existsByCustomerCustomerIdAndCreditProductCreditProductIdAndApplicationStatusIn(customer.getCustomerId(),
						creditProduct.getCreditProductId(),
						List.of(ApplicationStatus.SUBMITTED,
								ApplicationStatus.UNDER_REVIEW,
								ApplicationStatus.PENDING_REVIEW));

		if (activeApplicationExists) {
			throw new ConflictException("You already have an active application for this credit product");
		}
		
		// 7.Max active application check
		int activeApplicationCount = applicationRepository
		.countByCustomerCustomerIdAndApplicationStatusIn(customer.getCustomerId(),
				List.of(ApplicationStatus.SUBMITTED,
						ApplicationStatus.UNDER_REVIEW,
						ApplicationStatus.PENDING_REVIEW)
				);
		if(activeApplicationCount>=MAX_ACTIVE_APPLICATIONS) {
			throw new BusinessRuleException("Maximum " + MAX_ACTIVE_APPLICATIONS + " active applications allowed at a time. "
                    + "Please wait for your existing applications to be decided.");
		}
		
		// 8. Rejection Cool down Check
		Instant cooldownStart= Instant.now().minus(REJECTION_COOLDOWN_DAYS,ChronoUnit.DAYS);
		
		boolean recentlyRejected= applicationRepository
		.existsByCustomerCustomerIdAndCreditProductCreditProductIdAndApplicationStatusAndSubmittedAtAfter(
				customer.getCustomerId(),
				creditProduct.getCreditProductId(), 
				ApplicationStatus.REJECTED, 
				cooldownStart);
		
		if(recentlyRejected) {
			throw new BusinessRuleException("You cannot re-apply for this card product within "
                    + REJECTION_COOLDOWN_DAYS + " days of a rejection");
		}
		
		
		// 9.No active account for same credit product
		
		// NoOpActiveCardChecker returns false until card issuance module is built.
        // Once built, swap to IssuedCardActiveCardChecker — no code change needed here.
		if (activeAccountChecker.hasActiveAccount(
                customer.getCustomerId(),
                creditProduct.getCreditProductId())) {
            throw new BusinessRuleException(
                    "You already have an active credit account for this product. "
                            + "Cannot apply again.");
        }
		
		//Employment validation
		if (request.getEmploymentType() == EmploymentType.SALARIED
		        && (request.getEmployerName() == null || request.getEmployerName().isBlank())) {
		    throw new BusinessRuleException("Employer name is required for salaried applicants");
		}
		

		// . Build application entity
		CreditCardApplication application = new CreditCardApplication();
		application.setCustomer(customer);
		application.setCreditProduct(creditProduct);
		application.setEmploymentType(request.getEmploymentType());//// need to check
		application.setEmployerName(request.getEmployerName());
		application.setMonthlyIncome(request.getMonthlyIncome());
		application.setExistingLiabilities(request.getExistingLiabilities());
		application.setCreditScoreAtApplication(request.getCreditScoreAtApplication());
		application.setRequestedCreditLimit(request.getRequestedCreditLimit());
		application.setApplicationStatus(ApplicationStatus.UNDER_REVIEW);
		application.setSubmittedAt(Instant.now());

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

        return new ApiResponse<>(Instant.now(), HttpStatus.CREATED.value(),
                "Application submitted successfully",
                applicationMapper.toCreateResponse(saved));
	}

	// GET Customer  Application

	@Override
	@Transactional(readOnly = true)
	public ApiResponse<List<CreditCardApplicationResponse>> getMyApplications(UUID userId) {

		Customer customer = getCustomerFromUser(userId);

		List<CreditCardApplicationResponse> list = applicationRepository
				.findAllByCustomerCustomerId(customer.getCustomerId()).stream().map(applicationMapper::toResponse)
				.collect(Collectors.toList());

		return new ApiResponse<>(Instant.now(), HttpStatus.OK.value(), "Applications fetched successfully", list);
	}

	// GET CUSTOMER APPLICATION BY ID
	@Override
	@Transactional(readOnly = true)
	public ApiResponse<CreditCardApplicationResponse> getMyApplicationById(UUID userId, UUID applicationId) {
		Customer customer = getCustomerFromUser(userId);

		CreditCardApplication application = applicationRepository.findById(applicationId)
				.orElseThrow(() -> new ResourceNotFoundException("Application with id " + applicationId + " not found"));

		if (!application.getCustomer().getCustomerId().equals(customer.getCustomerId())) {
			throw new AccessDeniedException("Access denied to this application");
		}

		return new ApiResponse<>(Instant.now(), HttpStatus.OK.value(), "Application fetched successfully",
				applicationMapper.toResponse(application));
	}
	
	//GET Application By Id 
	@Override
	@Transactional(readOnly = true)
	public ApiResponse<CreditCardApplicationResponse> getApplicationById(UUID applicationId) {

	    CreditCardApplication application = applicationRepository.findById(applicationId)
	            .orElseThrow(() -> new ResourceNotFoundException(
	                    "Application with id " + applicationId + " not found"));

	    return new ApiResponse<>(
	            Instant.now(),
	            HttpStatus.OK.value(),
	            "Application fetched successfully",
	            applicationMapper.toResponse(application)
	    );
	}

	// GET ALL APPLICATIONS (Admin)
	@Override
	@Transactional(readOnly = true)
	public ApiResponse<List<CreditCardApplicationResponse>> getAllApplications() {

		List<CreditCardApplicationResponse> list = applicationRepository.findAll().stream()
				.map(applicationMapper::toResponse).collect(Collectors.toList());

		return new ApiResponse<>(Instant.now(), HttpStatus.OK.value(), "All applications fetched successfully", list);
	}

	// GET APPLICATIONS BY STATUS (Admin)
	@Override
	@Transactional(readOnly = true)
	public ApiResponse<List<CreditCardApplicationResponse>> getApplicationsByStatus(String status) {

		ApplicationStatus applicationStatus;
		try {
			applicationStatus = ApplicationStatus.valueOf(status.toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new BadRequestException("Invalid application status: " + status);
		}

		List<CreditCardApplicationResponse> list = applicationRepository.findAllByApplicationStatus(applicationStatus)
				.stream().map(applicationMapper::toResponse).collect(Collectors.toList());

		return new ApiResponse<>(Instant.now(), HttpStatus.OK.value(), "Applications fetched for status: " + status,
				list);
	}

	
	// MANUAL DECISION (Admin — PENDING_REVIEW only)
	@Override
	public ApiResponse<CreditCardApplicationResponse> decide(UUID applicationId, ApplicationDecisionRequest request) {

	    CreditCardApplication application = applicationRepository.findById(applicationId)
	            .orElseThrow(() -> new ResourceNotFoundException("Application with id " + applicationId + " not found"));

	    if (application.getApplicationStatus() != ApplicationStatus.PENDING_REVIEW) {
	        throw new BusinessRuleException("Only PENDING_REVIEW applications can be manually decided");
	    }

	    if (request.isApproved()) {
	        if (request.getApprovedCreditLimit() == null || request.getApprovedApr() == null) {
	            throw new BusinessRuleException("Approved credit limit and APR are required for approval");
	        }

	        application.setApplicationStatus(ApplicationStatus.APPROVED);
	        application.setDecision(DecisionType.MANUALLY_APPROVED);
	        application.setApprovedCreditLimit(request.getApprovedCreditLimit());
	        application.setApprovedApr(request.getApprovedApr());

	    } else {
	        application.setApplicationStatus(ApplicationStatus.REJECTED);
	        application.setDecision(DecisionType.MANUALLY_REJECTED);
	    }

	    application.setDecisionReason(request.getDecisionReason());
	    application.setDecisionAt(Instant.now());

	    CreditCardApplication saved = applicationRepository.save(application);

	    // Duplicate account check
	    if (saved.getApplicationStatus() == ApplicationStatus.APPROVED) {

	        boolean accountExists = creditAccountRepository
	                .existsByApplicationApplicationId(saved.getApplicationId());

	        if (accountExists) {
	            throw new ConflictException("Credit account already exists for this application");
	        }

	        try {
	            creditAccountService.createAccount(saved);
	        } catch (Exception e) {
	            throw new BusinessRuleException("Application approved but account creation failed: " + e.getMessage());
	        }
	    }

	    return new ApiResponse<>(
	            Instant.now(),
	            HttpStatus.OK.value(),
	            "Application approved and credit account created successfully",
	            applicationMapper.toResponse(saved)
	    );
	}
	
	// Private Helpers Methods
	private void applyDecisionToApplication(CreditCardApplication application, UnderwritingDecision decision) {
		application.setRiskScore(decision.getRiskScore());
		application.setDecision(decision.getDecision());
		application.setDecisionReason(decision.getDecisionReason());
		application.setDecisionAt(Instant.now());

		
		switch (decision.getDecision()) {
		case AUTO_APPROVED -> {
			application.setApplicationStatus(ApplicationStatus.APPROVED);
			application.setApprovedCreditLimit(decision.getApprovedLimit());
			application.setApprovedApr(decision.getApprovedApr());
		}
		case AUTO_REJECTED -> application.setApplicationStatus(ApplicationStatus.REJECTED);
		case PENDING_REVIEW -> application.setApplicationStatus(ApplicationStatus.PENDING_REVIEW);
		default -> application.setApplicationStatus(ApplicationStatus.UNDER_REVIEW);
		}
	}
	
	private Customer getCustomerFromUser(UUID userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new UserNotFoundException("User with id " + userId + " not found"));

		Customer customer = user.getCustomer();
		if (customer == null) {
			throw new ProfileNotCreatedException("Customer profile not found for user " + userId);
		}
		return customer;
	}
}