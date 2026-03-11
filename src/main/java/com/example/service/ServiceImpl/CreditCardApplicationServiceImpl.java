package com.example.service.ServiceImpl;

import com.example.dto.request.ApplicationDecisionRequest;
import com.example.dto.request.CreditCardApplicationRequest;
import com.example.dto.response.ApiResponse;
import com.example.dto.response.CardProductResponse;
import com.example.dto.response.CreditCardApplicationResponse;
import com.example.entity.CreditCardApplication;
import com.example.entity.CreditCardProduct;
import com.example.entity.Customer;
import com.example.entity.User;
import com.example.enums.*;
import com.example.exception.ProfileNotCreatedException;
import com.example.exception.ResourceNotFoundException;
import com.example.exception.UserNotFoundException;
import com.example.mapper.CardProductMapper;
import com.example.mapper.CreditCardApplicationMapper;
import com.example.repository.*;
import com.example.service.ActiveCardChecker;
import com.example.service.CreditCardApplicationService;
import com.example.underwriting.UnderwritingService;
import com.example.underwriting.model.UnderwritingDecision;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class CreditCardApplicationServiceImpl implements CreditCardApplicationService {

	
	// Maximum active applications a customer can have at any time
    private static final int MAX_ACTIVE_APPLICATIONS = 3;

    // Cool down period in days after rejection before re-applying for same product
    private static final int REJECTION_COOLDOWN_DAYS = 30;
    
	private final CreditCardApplicationRepository applicationRepository;
	private final CreditCardProductRepository cardProductRepository;
	private final KycRepository kycRepository;
	private final UserRepository userRepository;
	private final CreditCardApplicationMapper applicationMapper;
	private final CardProductMapper cardProductMapper;
	private final UnderwritingService underwritingService;
	private final ActiveCardChecker activeCardChecker;

	public CreditCardApplicationServiceImpl(CreditCardApplicationRepository applicationRepository,
			CreditCardProductRepository cardProductRepository, KycRepository kycRepository,
			UserRepository userRepository, CreditCardApplicationMapper applicationMapper,
			CardProductMapper cardProductMapper, UnderwritingService underwritingService, ActiveCardChecker activeCardChecker) {

		this.applicationRepository = applicationRepository;
		this.cardProductRepository = cardProductRepository;
		this.kycRepository = kycRepository;
		this.userRepository = userRepository;
		this.applicationMapper = applicationMapper;
		this.cardProductMapper = cardProductMapper;
		this.underwritingService = underwritingService;
		this.activeCardChecker = activeCardChecker;
	}

	// =====================================================
	// GET AVAILABLE CARD PRODUCTS
	// =====================================================
	@Override
	@Transactional(readOnly = true)
	public ApiResponse<List<CardProductResponse>> getAvailableCardProducts() {

		List<CardProductResponse> products = cardProductRepository.findAllByStatus(ProductStatus.ACTIVE).stream()
				.map(cardProductMapper::toResponse).collect(Collectors.toList());

		return new ApiResponse<>(Instant.now(), HttpStatus.OK.value(), "Available card products fetched successfully",
				products);
	}

	// =====================================================
	// SUBMIT APPLICATION
	// =====================================================
	@Override
	public ApiResponse<CreditCardApplicationResponse> apply(UUID userId, CreditCardApplicationRequest request) {
		
		// 1. Load user
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new UserNotFoundException("User with id " + userId + " not found"));

		// 2. Profile check
		Customer customer = user.getCustomer();
		if (customer == null) {
			throw new ProfileNotCreatedException("Complete your profile before applying for a credit card");
		}

		// 3. KYC gate
		boolean kycApproved = kycRepository.findByCustomerCustomerId(customer.getCustomerId()).stream()
				.anyMatch(kycRepository -> kycRepository.getStatus() == KycStatus.VERIFIED);

		if (!kycApproved) {
			throw new RuntimeException("Kyc must be verified before applying the credoy");
		}

		// 4. Card product validation
		CreditCardProduct cardProduct = cardProductRepository.findById(request.getCardProductId()).orElseThrow(
				() -> new ResourceNotFoundException("Card product with id " + request.getCardProductId() + " not found"));

		if (cardProduct.getStatus() == ProductStatus.INACTIVE) {
			throw new ResourceNotFoundException("Selected card product is no longer available");
		}
		
		
		// 5.Active Card check 
//		 boolean activeCardExists = cardAccountRepository
//	                .existsByCustomerCustomerIdAndCardProductCardProductIdAndStatus(
//	                        customer.getCustomerId(),
//	                        cardProduct.getCardProductId(),
//	                        AccountStatus.ACTIVE
//	                );
//
//	        if (activeCardExists) {
//	            throw new RuntimeException(
//	                    "You already have an active card for this product");
//	        }

		// 6. Duplicate application check
		boolean activeApplicationExists = applicationRepository
				.existsByCustomerCustomerIdAndCardProductCardProductIdAndApplicationStatusIn(customer.getCustomerId(),
						cardProduct.getCardProductId(),
						List.of(ApplicationStatus.SUBMITTED, ApplicationStatus.UNDER_REVIEW,
								ApplicationStatus.PENDING_REVIEW));

		if (activeApplicationExists) {
			throw new RuntimeException("You already have an active application for this card product");
		}
		
		// 7.Max active application check
		int activeApplicationCount = applicationRepository
		.countByCustomerCustomerIdAndApplicationStatusIn(customer.getCustomerId(),
				List.of(ApplicationStatus.SUBMITTED,
						ApplicationStatus.UNDER_REVIEW,
						ApplicationStatus.PENDING_REVIEW)
				);
		if(activeApplicationCount>=MAX_ACTIVE_APPLICATIONS) {
			throw new RuntimeException("Maximum " + MAX_ACTIVE_APPLICATIONS + " active applications allowed at a time. "
                    + "Please wait for your existing applications to be decided.");
		}
		
		// 8. Rejection Cool down Check
		Instant cooldownStart= Instant.now().minus(REJECTION_COOLDOWN_DAYS,ChronoUnit.DAYS);
		
		boolean recentlyRejected= applicationRepository
		.findTopByCustomerCustomerIdAndCardProductCardProductIdAndApplicationStatusAndSubmittedAtAfter(
				customer.getCustomerId(),
				cardProduct.getCardProductId(), 
				ApplicationStatus.REJECTED, 
				cooldownStart);
		
		if(recentlyRejected) {
			throw new RuntimeException("You cannot re-apply for this card product within "
                    + REJECTION_COOLDOWN_DAYS + " days of a rejection");
		}
		
		// NoOpActiveCardChecker returns false until card issuance module is built.
        // Once built, swap to IssuedCardActiveCardChecker — no code change needed here.
        if (activeCardChecker.hasActiveCard(
                customer.getCustomerId(),
                cardProduct.getCardProductId())) {
            throw new RuntimeException(
                    "You already hold an active card for this product. "
                            + "Cannot apply for the same product again.");
        }
		

		// . Build application entity
		CreditCardApplication application = new CreditCardApplication();
		application.setCustomer(customer);
		application.setCardProduct(cardProduct);
		application.setEmploymentType(request.getEmploymentType());//// need to check
		application.setEmployerName(request.getEmployerName());
		application.setMonthlyIncome(request.getMonthlyIncome());
		application.setExistingLiabilities(request.getExistingLiabilities());
		application.setCreditScoreAtApplication(request.getCreditScoreAtApplication());
		application.setRequestedCreditLimit(request.getRequestedCreditLimit());
		application.setApplicationStatus(ApplicationStatus.UNDER_REVIEW);
		application.setSubmittedAt(Instant.now());

		// ── Run underwriting on unsaved entity ──
        // ApplicationContext only reads field values — no DB ID needed
        UnderwritingDecision decision = underwritingService.evaluate(application);

        // ── Apply decision to entity ──
        applyDecisionToApplication(application, decision);

        // ── BUG 4 FIX: Single save with final decided state ──
        CreditCardApplication saved = applicationRepository.save(application);

        return new ApiResponse<>(Instant.now(), HttpStatus.CREATED.value(),
                "Application submitted successfully",
                applicationMapper.toResponse(saved));
	}

	// =====================================================
	// GET MY APPLICATIONS
	// =====================================================
	@Override
	@Transactional(readOnly = true)
	public ApiResponse<List<CreditCardApplicationResponse>> getMyApplications(UUID userId) {

		Customer customer = getCustomerFromUser(userId);

		List<CreditCardApplicationResponse> list = applicationRepository
				.findAllByCustomerCustomerId(customer.getCustomerId()).stream().map(applicationMapper::toResponse)
				.collect(Collectors.toList());

		return new ApiResponse<>(Instant.now(), HttpStatus.OK.value(), "Applications fetched successfully", list);
	}

	// =====================================================
	// GET MY APPLICATION BY ID
	// =====================================================
	@Override
	@Transactional(readOnly = true)
	public ApiResponse<CreditCardApplicationResponse> getMyApplicationById(UUID userId, UUID applicationId) {
		Customer customer = getCustomerFromUser(userId);

		CreditCardApplication application = applicationRepository.findById(applicationId)
				.orElseThrow(() -> new RuntimeException("Application with id " + applicationId + " not found"));

		if (!application.getCustomer().getCustomerId().equals(customer.getCustomerId())) {
			throw new RuntimeException("Access denied to this application");
		}

		return new ApiResponse<>(Instant.now(), HttpStatus.OK.value(), "Application fetched successfully",
				applicationMapper.toResponse(application));
	}

	// =====================================================
	// GET ALL APPLICATIONS (Admin)
	// =====================================================
	@Override
	@Transactional(readOnly = true)
	public ApiResponse<List<CreditCardApplicationResponse>> getAllApplications() {

		List<CreditCardApplicationResponse> list = applicationRepository.findAll().stream()
				.map(applicationMapper::toResponse).collect(Collectors.toList());

		return new ApiResponse<>(Instant.now(), HttpStatus.OK.value(), "All applications fetched successfully", list);
	}

	// =====================================================
	// GET APPLICATIONS BY STATUS (Admin)
	// =====================================================
	@Override
	@Transactional(readOnly = true)
	public ApiResponse<List<CreditCardApplicationResponse>> getApplicationsByStatus(String status) {

		ApplicationStatus applicationStatus;
		try {
			applicationStatus = ApplicationStatus.valueOf(status.toUpperCase());
		} catch (IllegalArgumentException e) {
			throw new RuntimeException("Invalid application status: " + status);
		}

		List<CreditCardApplicationResponse> list = applicationRepository.findAllByApplicationStatus(applicationStatus)
				.stream().map(applicationMapper::toResponse).collect(Collectors.toList());

		return new ApiResponse<>(Instant.now(), HttpStatus.OK.value(), "Applications fetched for status: " + status,
				list);
	}

	// =====================================================
	// MANUAL DECISION (Admin — PENDING_REVIEW only)
	// =====================================================
	@Override
	public ApiResponse<CreditCardApplicationResponse> decide(UUID applicationId, ApplicationDecisionRequest request) {

		CreditCardApplication application = applicationRepository.findById(applicationId)
				.orElseThrow(() -> new RuntimeException("Application with id " + applicationId + " not found"));

		if (application.getApplicationStatus() != ApplicationStatus.PENDING_REVIEW) {
			throw new RuntimeException("Only PENDING_REVIEW applications can be manually decided");
		}

		if (request.isApproved()) {
			if (request.getApprovedCreditLimit() == null || request.getApprovedApr() == null) {
				throw new RuntimeException("Approved credit limit and APR are required for approval");
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

		return new ApiResponse<>(Instant.now(), HttpStatus.OK.value(), "Decision recorded successfully",
				applicationMapper.toResponse(applicationRepository.save(application)));
	}

	// =====================================================
	// PRIVATE HELPERS
	// =====================================================
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
//helpers
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