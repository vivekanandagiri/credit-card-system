package com.example.service.ServiceImpl;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.dto.request.CreditCardIssuanceRequest;
import com.example.dto.request.CreditCardStatusUpdateRequest;
import com.example.dto.response.CreditCardIssuanceResponse;
import com.example.dto.response.CreditCardResponse;
import com.example.entity.CreditAccount;
import com.example.entity.CreditCard;
import com.example.entity.CreditCardProduct;
import com.example.entity.Customer;
import com.example.enums.AccountStatus;
import com.example.enums.CardFormat;
import com.example.enums.CardStatus;
import com.example.enums.UserRole;
import com.example.exception.AccessDeniedException;
import com.example.exception.BusinessRuleException;
import com.example.exception.ConflictException;
import com.example.exception.ResourceNotFoundException;
import com.example.mapper.CreditCardMapper;
import com.example.repository.*;
import com.example.security.CustomUserPrincipal;
import com.example.service.CardProductService;
import com.example.service.CreditAccountService;
import com.example.service.CreditCardService;
import com.example.service.CustomerAddressService;
import com.example.service.CustomerService;
import com.example.service.UserService;
import com.example.util.MaskedCardNumberGenerator;

/**
 * Service responsible for managing credit card lifecycle operations.
 * Implementation of {@link CreditCardService}.
 * <p>This service handles card issuance, retrieval, and status transitions
 * while enforcing strict business rules and ownership validations.</p>
 *
 * <p><b>Key Responsibilities:</b></p>
 * <ul>
 *     <li>Issue credit cards (customer & admin flows)</li>
 *     <li>Validate account eligibility and ownership</li>
 *     <li>Manage card lifecycle transitions (ACTIVE, BLOCKED, etc.)</li>
 *     <li>Enforce constraints like max cards per account and virtual card uniqueness</li>
 *     <li>Retrieve card details securely</li>
 * </ul>
 *
 * <p><b>Domain Boundaries:</b></p>
 * <ul>
 *     <li>Owns {@code CreditCardRepository}</li>
 *     <li>Delegates cross-domain lookups to other services</li>
 * </ul>
 * <ul>
 *  *   <li>{@link UserService} — user → customer resolution</li>
 *  *   <li>{@link CreditAccountService} — account entity lookup</li>
 *  *   <li>{@link CardProductService} — card product entity lookup</li>
 *  * </ul>
 *
 * <p>Valid card status transitions:
 *  * <ul>
 *  *   <li>PENDING_ACTIVATION → ACTIVE, CANCELLED</li>
 *  *   <li>ACTIVE → BLOCKED, CANCELLED, EXPIRED</li>
 *  *   <li>BLOCKED → ACTIVE, CANCELLED</li>
 *  *   <li>EXPIRED → CANCELLED</li>
 *  *   <li>CANCELLED → (terminal)</li>
 *  * </ul>
 * <p><b>Important:</b> This is a security-sensitive component.
 * All operations enforce strict ownership and access control checks.</p>
 */

@Service
@Transactional
public class CreditCardServiceImpl implements CreditCardService {

    // Max total active cards per account (PENDING_ACTIVATION + ACTIVE)
    private static final int MAX_ACTIVE_CARDS_PER_ACCOUNT = 5;
    
    private static final Map<CardStatus, Set<CardStatus>> VALID_TRANSITIONS = Map.of(
            CardStatus.PENDING_ACTIVATION, Set.of(CardStatus.ACTIVE, CardStatus.CANCELLED),
            CardStatus.ACTIVE,             Set.of(CardStatus.BLOCKED, CardStatus.CANCELLED, CardStatus.EXPIRED),
            CardStatus.BLOCKED,            Set.of(CardStatus.ACTIVE, CardStatus.CANCELLED),
            CardStatus.EXPIRED,            Set.of(CardStatus.CANCELLED),
            CardStatus.CANCELLED,          Set.of());

    private final CreditCardRepository cardRepository;
    private final CreditAccountService creditAccountService;
    private final CardProductService cardProductService;
    private final CreditCardMapper cardMapper;
    private final MaskedCardNumberGenerator maskedCardNumberGenerator;
    private final CustomerService customerService;
    private final CustomerAddressService customerAddressService;

    public CreditCardServiceImpl(
            CreditCardRepository cardRepository,
            CustomerService customerService, 
            CreditAccountService creditAccountService,
            CardProductService cardProductService,
            CreditCardMapper cardMapper,
            MaskedCardNumberGenerator maskedCardNumberGenerator, CustomerAddressService customerAddressService) {

        this.cardRepository = cardRepository;
        this.customerService = customerService; 
        this.creditAccountService = creditAccountService;
        this.cardProductService = cardProductService;
        this.cardMapper = cardMapper;
        this.maskedCardNumberGenerator = maskedCardNumberGenerator;
		this.customerAddressService = customerAddressService;
    }

    /**
     * Issues a new credit card for a customer.
     *
     * <p>This method allows a customer to request a card for their own account,
     * subject to eligibility checks.</p>
     *
     * <p><b>Validations:</b></p>
     * <ul>
     *     <li>Account must belong to the user</li>
     *     <li>Account must be ACTIVE</li>
     *     <li>Card product must be valid and active</li>
     *     <li>Maximum card limit per account must not be exceeded</li>
     *     <li>Virtual card uniqueness enforced</li>
     *     <li>Physical card requires address</li>
     * </ul>
     *
     * @param userId   the user requesting the card
     * @param accountId the target credit account
     * @param request  issuance request payload
     * @return issued card details
     */
    @Override
    public CreditCardResponse issueCard(
            UUID userId,
            UUID accountId,
            CreditCardIssuanceRequest request) {

        Customer customer = customerService.getCustomerByUserId(userId);
        CreditAccount account = creditAccountService.getAccountEntity(accountId);

        validateAccountOwnership(account, customer);
        validateAccountActive(account);

        CreditCard card = buildAndIssueCard(account, request, IssuedBy.CUSTOMER);

        return cardMapper.toResponse(card);
    }

    /**
     * Issues a credit card on behalf of a customer (admin operation).
     *
     * <p>This method bypasses customer ownership validation but still enforces
     * all business rules related to card issuance.</p>
     *
     * @param accountId the account for which the card is issued
     * @param request   issuance request payload
     * @return issued card details
     */
    @Override
    public CreditCardResponse issueCardByAdmin(UUID accountId, CreditCardIssuanceRequest request) {

        CreditAccount account = creditAccountService.getAccountEntity(accountId);
        CreditCard card = buildAndIssueCard(account, request, IssuedBy.ADMIN);

        return cardMapper.toResponse(card);
    }

    /**
     * GET CARDS BY ACCOUNT (Customer)
     */
    @Override
    @Transactional(readOnly = true)
    public List<CreditCardResponse> getCardsByAccount(UUID userId, UUID accountId) {

        Customer customer = customerService.getCustomerByUserId(userId);
        CreditAccount account = creditAccountService.getAccountEntity(accountId);

        validateAccountOwnership(account, customer);

        return cardRepository
                .findAllByCreditAccountAccountId(account.getAccountId())
                .stream()
                .map(cardMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * GET CARD DETAILS BY ID (Customer)
     */
    @Override
    @Transactional(readOnly = true)
    public CreditCardResponse getCardById(UUID userId,UUID accountId ,UUID cardId) {

        Customer customer = customerService.getCustomerByUserId(userId);
        CreditAccount account = creditAccountService.getAccountEntity(accountId);
        
        validateAccountOwnership(account, customer);

        CreditCard card = findCardById(cardId);
        
        if (!card.getCreditAccount().getAccountId().equals(accountId)) {
            throw new AccessDeniedException("Card does not belong to this account");
        }
        
        if (!card.getCreditAccount().getCustomer().getCustomerId().equals(customer.getCustomerId())) {
            throw new AccessDeniedException("Access denied to this card");
        }

        return cardMapper.toResponse(card);
    }

    @Override
    public List<CreditCardIssuanceResponse> getCardsByStatusForUser(
            CustomUserPrincipal principal,
            CardStatus status) {

        if (principal.getRole() == UserRole.ADMIN) {
            return getCardsByStatus(status);
        }

        Customer customer = customerService.getCustomerByUserId(principal.getUserId());

        return cardRepository
                .findAllByCreditAccountCustomerCustomerIdAndCardStatus(customer.getCustomerId(), status)
                .stream()
                .map(cardMapper::toIssueResponse)
                .toList();
    }

    /**
     * Fetch cards by account id (Admin)
     */
    @Override
    @Transactional(readOnly = true)
    public List<CreditCardResponse> getCardsByAccount(UUID accountId) {

        CreditAccount account = creditAccountService.getAccountEntity(accountId);

        return cardRepository
                .findAllByCreditAccountAccountId(account.getAccountId())
                .stream()
                .map(cardMapper::toResponse)
                .toList();
    }

    /**
     * GET CARDS BY STATUS (Admin)
     */
    @Override
    @Transactional(readOnly = true)
    public List<CreditCardIssuanceResponse> getCardsByStatus(CardStatus status) {

        return cardRepository
                .findAllByCardStatus(status)
                .stream()
                .map(cardMapper::toIssueResponse)
                .collect(Collectors.toList());
    }


    @Override
    @Transactional(readOnly = true)
    public CreditCardResponse getCardById(UUID cardId) {
    	CreditCard creditCard = cardRepository.findById(cardId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Card not found with id: " + cardId
                        ));

        return cardMapper.toResponse(creditCard);
    }

    /**
     * Updates the status of a credit card with role-based access control.
     *
     * <p><b>Behavior:</b></p>
     * <ul>
     *     <li>Validates ownership for customer users</li>
     *     <li>Restricts customers to limited status transitions (ACTIVE, BLOCKED)</li>
     *     <li>Admins can perform all valid transitions</li>
     *     <li>Ensures transition follows predefined state machine</li>
     *     <li>Applies relevant timestamps (activatedAt, blockedAt, etc.)</li>
     * </ul>
     *
     * @param principal authenticated user
     * @param accountId account ID
     * @param cardId    card ID
     * @param request   status update request
     * @return updated card response
     *
     * @throws BusinessRuleException if transition is invalid
     * @throws AccessDeniedException if user is unauthorized
     */
    @Override
    public CreditCardIssuanceResponse updateCardStatusForUser(
            CustomUserPrincipal principal,
            UUID accountId,
            UUID cardId,
            CreditCardStatusUpdateRequest request) {

        CreditCard card = findCardById(cardId);
        
        if (!card.getCreditAccount().getAccountId().equals(accountId)) {
            throw new AccessDeniedException("Card does not belong to this account");
        }
        
        validateOwnershipIfCustomer(principal, card);
        
        CardStatus currentStatus = card.getCardStatus();
        CardStatus newStatus = request.getStatus();

        if (principal.getRole() == UserRole.CUSTOMER) {

            validateCustomerCardOwnership(principal, card);

            if (!List.of(CardStatus.ACTIVE, CardStatus.BLOCKED).contains(newStatus)) {
                throw new BusinessRuleException(
                        "Customers can only change status to ACTIVE or BLOCKED");
            }
        }

        validateCardStatusTransition(currentStatus, newStatus);

        applyStatusTimestamps(card, newStatus);

        card.setCardStatus(newStatus);

        return cardMapper.toIssueResponse(cardRepository.save(card));
    }

    /**
     * Core method responsible for constructing and issuing a credit card.
     *
     * <p>This method centralizes all issuance validations and entity creation logic.</p>
     *
     * <p><b>Validation Pipeline:</b></p>
     * <ul>
     *     <li>Card product must exist and be active</li>
     *     <li>Physical cards require a valid customer address</li>
     *     <li>Maximum active card limit per account enforced</li>
     *     <li>Only one virtual card allowed per account</li>
     * </ul>
     *
     * @param account   credit account
     * @param request   issuance request
     * @param issuedBy  source of issuance (CUSTOMER / ADMIN)
     * @return persisted credit card entity
     */
    private CreditCard buildAndIssueCard(CreditAccount account,
    									 CreditCardIssuanceRequest request,
                                         IssuedBy issuedBy) {
        //Gate 1: Credit Card Product exist and Active
        
        CreditCardProduct cardProduct = cardProductService.getActiveCardProductEntity(request.getCardProductId());
        //Gate 2: Parse Card Format
        CardFormat cardFormat = request.getCardFormat();
        //Gate 4 :Address Check for Physical card 
        validatePhysicalCardAddress(account.getCustomer(), cardFormat);
        //Gate 3: Max Active card per Account
        validateActiveCardLimit(account);
        //Gate 4: One VIRTUAL card per account at a time
        validateVirtualCardUniqueness(account, cardFormat);
        // Build card
        CreditCard creditCard = buildCardEntity(request, account, cardProduct, cardFormat, issuedBy);

        return cardRepository.save(creditCard);
    }
    
    /**
     * {@inheritDoc}
     *
     * <p>Used by {@link TransactionServiceImpl} to resolve the card entity
     * without injecting the card repository.
     */
    @Override
    @Transactional(readOnly = true)
    public CreditCard getCardEntity(UUID cardId) {
        return findCardById(cardId);
    }
    // =================================================PRIVATE HELPERS================================================================

    /**
     * Account Ownership Validation
     * @param account Credit Account
     * @param customer Authenticated Customer
     */
    private void validateAccountOwnership(CreditAccount account, Customer customer) {
        if (!account.getCustomer().getCustomerId().equals(customer.getCustomerId())) {
            throw new AccessDeniedException("Access denied to this account");
        }
    }
    /**
     * Validate Account Status 
     * @param account Credit Account
     */
    private void validateAccountActive(CreditAccount account) {
        if (account.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new BusinessRuleException("Account must be ACTIVE to perform this operation");
        }
    }
    /**
     * Validate the User is Customer
     * @param principal Custom User Principal
     * @param card Credit card
     */
    private void validateOwnershipIfCustomer(CustomUserPrincipal principal, CreditCard card) {
        if (principal.getRole() == UserRole.CUSTOMER) {
            Customer customer = customerService.getCustomerByUserId(principal.getUserId());
            if (!card.getCreditAccount().getCustomer().getCustomerId()
                    .equals(customer.getCustomerId())) {
                throw new AccessDeniedException("Access denied to this card");
            }
        }
    }
    /**
     * Parse Credit Card By id
     * @param cardId Credit Card id
     * @return Card Details
     */
    private CreditCard findCardById(UUID cardId) {
        return cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found: " + cardId));
    }

    /**
     * Ensures that the account does not exceed the maximum allowed active cards.
     *
     * <p>Counts cards in ACTIVE and PENDING_ACTIVATION states.</p>
     *
     * @param account credit account
     * @throws BusinessRuleException if limit is exceeded
     */
    private void validateActiveCardLimit(CreditAccount account) {
        int count = cardRepository.countByCreditAccountAccountIdAndCardStatusIn(
                account.getAccountId(), List.of(CardStatus.PENDING_ACTIVATION, CardStatus.ACTIVE));
        if (count >= MAX_ACTIVE_CARDS_PER_ACCOUNT) {
            throw new BusinessRuleException("Maximum card limit reached for this account");
        }
    }
    /**
     * Ensures only one virtual card exists per account.
     *
     * <p>Prevents issuing multiple active or pending virtual cards.</p>
     *
     * @param account    credit account
     * @param cardFormat requested card format
     * @throws BusinessRuleException if virtual card already exists
     */
    private void validateVirtualCardUniqueness(CreditAccount account, CardFormat cardFormat) {
        if (cardFormat == CardFormat.VIRTUAL) {
            boolean exists = cardRepository.existsByCreditAccountAccountIdAndCardFormatAndCardStatusIn(
                    account.getAccountId(), CardFormat.VIRTUAL,
                    List.of(CardStatus.PENDING_ACTIVATION, CardStatus.ACTIVE));
            if (exists) {
                throw new BusinessRuleException("A virtual card already exists for this account");
            }
        }
    }
    /**
     * Validates whether a card status transition is allowed.
     *
     * <p>Uses a predefined state transition map to ensure only valid transitions occur.</p>
     *
     * <p><b>Example:</b></p>
     * <ul>
     *     <li>ACTIVE → BLOCKED (allowed)</li>
     *     <li>ACTIVE → PENDING_ACTIVATION (not allowed)</li>
     * </ul>
     *
     * @param current current card status
     * @param next    requested new status
     * @throws BusinessRuleException if transition is invalid
     * @throws ConflictException if status is unchanged
     */
    private void validateCardStatusTransition(CardStatus current, CardStatus next) {
        if (current == next) throw new ConflictException("Card is already in " + current + " status");
        Set<CardStatus> allowed = VALID_TRANSITIONS.getOrDefault(current, Set.of());
        if (!allowed.contains(next)) {
            throw new BusinessRuleException(
                    "Invalid transition from " + current + " to " + next + ". Allowed: " + allowed);
        }
    }
    /**
     * Applies timestamps based on card status transitions.
     *
     * <p>Automatically updates lifecycle timestamps such as:</p>
     * <ul>
     *     <li>activatedAt</li>
     *     <li>blockedAt</li>
     *     <li>cancelledAt</li>
     *     <li>expiresAt</li>
     * </ul>
     *
     * @param card      credit card entity
     * @param newStatus new card status
     */
    private void applyStatusTimestamps(CreditCard card, CardStatus newStatus) {
        Instant now = Instant.now();
        switch (newStatus) {
            case ACTIVE -> { if (card.getActivatedAt() == null) card.setActivatedAt(now); }
            case BLOCKED -> card.setBlockedAt(now);
            case CANCELLED -> card.setCancelledAt(now);
            case EXPIRED -> card.setExpiresAt(now);
            default -> { }
        }
    }
    /**
     * Constructs a new {@link CreditCard} entity with all required fields.
     *
     * <p><b>Responsibilities:</b></p>
     * <ul>
     *     <li>Assign card metadata (product, format, status)</li>
     *     <li>Generate masked card number</li>
     *     <li>Set expiry date based on product validity</li>
     *     <li>Initialize channel permissions</li>
     *     <li>Apply activation logic for virtual cards</li>
     * </ul>
     *
     * @param request     issuance request
     * @param account     credit account
     * @param cardProduct card product
     * @param cardFormat  card format
     * @param issuedBy    issuer type
     * @return constructed card entity
     */
    private CreditCard buildCardEntity(CreditCardIssuanceRequest request,
            CreditAccount account, CreditCardProduct cardProduct, CardFormat cardFormat, IssuedBy issuedBy) {
    	// Using LocalDateTime.now() uses the server's local time. If the server is in India (IST) 
        // and it is 2:00 AM on the 1st of the month, but the customer is in the US, the customer 
        // gets a card that expires a full month later than intended.
        LocalDateTime now = LocalDateTime.now(ZoneId.of("UTC"));
        int expiryYear = now.getYear() + cardProduct.getCardValidityYears();
        int expiryMonth = now.getMonthValue();
     // A credit card technically expires at 23:59:59 UTC on the last day of the month.
        Instant expiresAt = YearMonth.of(expiryYear, expiryMonth)
                .atEndOfMonth().atTime(23, 59, 59).atZone(ZoneId.of("UTC")).toInstant();
        CardStatus initialStatus = cardFormat == CardFormat.VIRTUAL
                ? CardStatus.ACTIVE : CardStatus.PENDING_ACTIVATION;
 
        CreditCard card = new CreditCard();
        card.setCreditAccount(account);
        card.setCardProduct(cardProduct);
        card.setCardFormat(cardFormat);
        card.setCardStatus(initialStatus);
        card.setIssuanceReason(request.getIssuanceReason());
        card.setMaskedCardNumber(maskedCardNumberGenerator.generate(cardProduct.getNetworkType()));
        card.setIssuedAt(Instant.now());
        card.setExpiresAt(expiresAt);
        card.setExpiryMonth(expiryMonth);
        card.setExpiryYear(expiryYear);
        // Inheriting rules from the product catalog
        card.setOnlineEnabled(cardProduct.getOnlineTransactionsAllowed());
        card.setAtmEnabled(cardProduct.getAtmWithdrawalAllowed());
        card.setInternationalEnabled(cardProduct.getInternationalUsageAllowed());
        card.setIssuedBy(issuedBy.name());
        if (cardFormat == CardFormat.VIRTUAL) card.setActivatedAt(Instant.now());
        return card;
    }
 
    /**
     * 
     * @param principal CustomUserPrincipal
     * @param card Credit Card
     */
    private void validateCustomerCardOwnership(CustomUserPrincipal principal, CreditCard card) {
        Customer customer = customerService.getCustomerByUserId(principal.getUserId());
        if (!card.getCreditAccount().getCustomer().getCustomerId().equals(customer.getCustomerId())) {
            throw new AccessDeniedException("Access denied to this card");
        }
    }
    /**
     * Validates that a customer has a delivery address for physical card issuance.
     *
     * <p>Required only for PHYSICAL card format.</p>
     *
     * @param customer   customer entity
     * @param cardFormat card format
     * @throws BusinessRuleException if address is missing
     */
    private void validatePhysicalCardAddress(Customer customer, CardFormat cardFormat) {

        if (cardFormat == CardFormat.PHYSICAL) {

            boolean hasAddress = customerAddressService
                    .hasAddress(customer.getCustomerId());

            if (!hasAddress) {
                throw new BusinessRuleException(
                    "Physical card requires a delivery address. Please add an address first."
                );
            }
        }
    }
 
    private enum IssuedBy { CUSTOMER, ADMIN }

    
}