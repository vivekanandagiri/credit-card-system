package com.example.service.ServiceImpl;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.dto.request.CreditCardIssuanceRequest;
import com.example.dto.request.CreditCardStatusUpdateRequest;
import com.example.dto.response.CardProductResponse;
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
import com.example.service.CustomerService;
import com.example.service.UserService;
import com.example.util.MaskedCardNumberGenerator;

/**
 * Implementation of {@link CreditCardService}.
 *
 * <p><strong>Domain ownership:</strong> owns only {@code CreditCardRepository}.
 * Cross-domain lookups are delegated:
 * <ul>
 *   <li>{@link UserService} — user → customer resolution</li>
 *   <li>{@link CreditAccountService} — account entity lookup</li>
 *   <li>{@link CardProductService} — card product entity lookup</li>
 * </ul>
 *
 * <p>Valid card status transitions:
 * <ul>
 *   <li>PENDING_ACTIVATION → ACTIVE, CANCELLED</li>
 *   <li>ACTIVE → BLOCKED, CANCELLED, EXPIRED</li>
 *   <li>BLOCKED → ACTIVE, CANCELLED</li>
 *   <li>EXPIRED → CANCELLED</li>
 *   <li>CANCELLED → (terminal)</li>
 * </ul>
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

    public CreditCardServiceImpl(
            CreditCardRepository cardRepository,
            CustomerService customerService, 
            CreditAccountService creditAccountService,
            CardProductService cardProductService,
            CreditCardMapper cardMapper,
            MaskedCardNumberGenerator maskedCardNumberGenerator) {

        this.cardRepository = cardRepository;
        this.customerService = customerService; 
        this.creditAccountService = creditAccountService;
        this.cardProductService = cardProductService;
        this.cardMapper = cardMapper;
        this.maskedCardNumberGenerator = maskedCardNumberGenerator;
    }
    
    /**
     * GET AVAILABLE CARD PRODUCTS (Customer)
     * Customer can only pick card products that belong to
     * the same CreditProduct as their account
     */
    @Override
    @Transactional(readOnly = true)
    public List<CardProductResponse> getAvailableCardProducts(UUID userId, UUID accountId) {

        Customer customer = customerService.getCustomerByUserId(userId);
        CreditAccount account = creditAccountService.getAccountEntity(accountId);

        validateAccountOwnership(account, customer);
        validateAccountActive(account);

        List<CardProductResponse> products = cardProductService.getAllActive();

        return  products;
    }

    /**
     * ISSUE CARD — Customer self-issuance
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
     * ISSUE CARD — Admin on behalf of customer
     */
    @Override
    public CreditCardResponse issueCardByAdmin(UUID accountId, CreditCardIssuanceRequest request) {

        CreditAccount account = creditAccountService.getAccountEntity(accountId);
        CreditCard card = buildAndIssueCard(account, request, IssuedBy.ADMIN);

        return cardMapper.toResponse(card);
    }

//    /**
//     * GET customers CARDS (Customer — all accounts)
//     */
//    @Override
//    @Transactional(readOnly = true)
//    public ApiResponse<List<CreditCardIssuanceResponse>> getMyCards(UUID userId) {
//
//        Customer customer = customerService.getCustomerByUserId(userId);
//
//        List<CreditCardIssuanceResponse> cards = cardRepository
//                .findAllByCreditAccountCustomerCustomerId(customer.getCustomerId())
//                .stream()
//                .map(cardMapper::toIssueResponse)
//                .collect(Collectors.toList());
//
//        return new ApiResponse<>(Instant.now(), HttpStatus.OK.value(),
//                "Cards fetched successfully", cards);
//    }

    /**
     * GET CARDS BY ACCOUNT (Customer)
     */
    @Override
    @Transactional(readOnly = true)
    public List<CreditCardResponse> getCardsByAccount(UUID userId, UUID accountId) {

        Customer customer = customerService.getCustomerByUserId(userId);
        CreditAccount account = creditAccountService.getAccountEntity(accountId);

        validateAccountOwnership(account, customer);

        List<CreditCardResponse> cards = cardRepository
                .findAllByCreditAccountAccountId(account.getAccountId())
                .stream()
                .map(cardMapper::toResponse)
                .collect(Collectors.toList());

        return cards;
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

        List<CreditCardIssuanceResponse> cards = cardRepository
                .findAllByCreditAccountCustomerCustomerIdAndCardStatus(customer.getCustomerId(), status)
                .stream()
                .map(cardMapper::toIssueResponse)
                .toList();

        return  cards;
    }

//    /**
//     * GET ALL CARDS (Admin)
//     */
//    @Override
//    @Transactional(readOnly = true)
//    public List<CreditCardIssuanceResponse> getAllCards() {
//
//        List<CreditCardIssuanceResponse> cards = cardRepository.findAll()
//                .stream()
//                .map(cardMapper::toIssueResponse)
//                .collect(Collectors.toList());
//
//        return cards;
//    }

    /**
     * Fetch cards by account id (Admin)
     */
    @Override
    @Transactional(readOnly = true)
    public List<CreditCardResponse> getCardsByAccount(UUID accountId) {

        CreditAccount account = creditAccountService.getAccountEntity(accountId);

        List<CreditCardResponse> cards = cardRepository
                .findAllByCreditAccountAccountId(account.getAccountId())
                .stream()
                .map(cardMapper::toResponse)
                .toList();

        return  cards;
    }

    /**
     * GET CARDS BY STATUS (Admin)
     */
    @Override
    @Transactional(readOnly = true)
    public List<CreditCardIssuanceResponse> getCardsByStatus(CardStatus status) {

        List<CreditCardIssuanceResponse> cards = cardRepository
                .findAllByCardStatus(status)
                .stream()
                .map(cardMapper::toIssueResponse)
                .collect(Collectors.toList());

        return cards;
    }


    @Override
    @Transactional(readOnly = true)
    public CreditCardResponse getCardById(UUID cardId) {
        return cardMapper.toResponse(findCardById(cardId));
    }

    /**
     * UPDATE CARD STATUS (Shared)
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
     * CORE ISSUANCE LOGIC (shared by customer + admin)
     */
    private CreditCard buildAndIssueCard(CreditAccount account,
    									 CreditCardIssuanceRequest request,
                                         IssuedBy issuedBy) {
        //Gate 1: Credit Card Product exist and Active
        
        CreditCardProduct cardProduct = cardProductService.getActiveCardProductEntity(request.getCardProductId());
        //Gate 2: Parse Card Format
        CardFormat cardFormat = request.getCardFormat();
        //Gate 3: Max Active card per Account
        validateActiveCardLimit(account);
        //Gate 4: One VIRTUAL card per account at a time
        validateVirtualCardUniqueness(account, cardFormat);
        // Build card
        CreditCard creditCard = buildCardEntity(request, account, cardProduct, cardFormat, issuedBy);

        return cardRepository.save(creditCard);
    }
    
//    @Override
//    public ApiResponse<CreditCardIssuanceResponse> activateCard(
//            CustomUserPrincipal principal,
//            UUID cardId) {
//
//        CreditCard card = findCardById(cardId);
//
//        validateOwnershipIfCustomer(principal, card);
//
//        validateCardStatusTransition(card.getCardStatus(), CardStatus.ACTIVE);
//
//        if (card.getActivatedAt() == null) {
//            card.setActivatedAt(Instant.now());
//        }
//
//        card.setCardStatus(CardStatus.ACTIVE);
//
//        return buildSuccessResponse(card, "Card activated successfully");
//    }
//    
//    @Override
//    public ApiResponse<CreditCardIssuanceResponse> blockCard(
//            CustomUserPrincipal principal,
//            UUID cardId,
//            String reason) {
//
//        CreditCard card = findCardById(cardId);
//
//        validateOwnershipIfCustomer(principal, card);
//
//        validateCardStatusTransition(card.getCardStatus(), CardStatus.BLOCKED);
//
//        card.setBlockedAt(Instant.now());
//        card.setCardStatus(CardStatus.BLOCKED);
//
//        return buildSuccessResponse(card, "Card blocked successfully");
//    }
//    
//    @Override
//    public ApiResponse<CreditCardIssuanceResponse> unblockCard(
//            CustomUserPrincipal principal,
//            UUID cardId) {
//
//        CreditCard card = findCardById(cardId);
//
//        validateOwnershipIfCustomer(principal, card);
//
//        validateCardStatusTransition(card.getCardStatus(), CardStatus.ACTIVE);
//
//        card.setCardStatus(CardStatus.ACTIVE);
//
//        return buildSuccessResponse(card, "Card unblocked successfully");
//    }
//    
//    @Override
//    public ApiResponse<CreditCardIssuanceResponse> cancelCard(
//            CustomUserPrincipal principal,
//            UUID cardId,
//            String reason) {
//
//        CreditCard card = findCardById(cardId);
//
//        validateOwnershipIfCustomer(principal, card);
//
//        validateCardStatusTransition(card.getCardStatus(), CardStatus.CANCELLED);
//
//        card.setCancelledAt(Instant.now());
//        card.setCardStatus(CardStatus.CANCELLED);
//
//        return buildSuccessResponse(card, "Card cancelled successfully");
//    }
    
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
     * @param account
     * @param customer
     */
    private void validateAccountOwnership(CreditAccount account, Customer customer) {
        if (!account.getCustomer().getCustomerId().equals(customer.getCustomerId())) {
            throw new AccessDeniedException("Access denied to this account");
        }
    }
    /**
     * Validate Account Status 
     * @param account
     */
    private void validateAccountActive(CreditAccount account) {
        if (account.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new BusinessRuleException("Account must be ACTIVE to perform this operation");
        }
    }
    /**
     * Validate the User is Customer
     * @param principal
     * @param card
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
     * @param cardId
     * @return
     */
    private CreditCard findCardById(UUID cardId) {
        return cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found: " + cardId));
    }
    
    
    /**
     * Validate Maximum Card Limit Per Account 
     * @param account
     */
    private void validateActiveCardLimit(CreditAccount account) {
        int count = cardRepository.countByCreditAccountAccountIdAndCardStatusIn(
                account.getAccountId(), List.of(CardStatus.PENDING_ACTIVATION, CardStatus.ACTIVE));
        if (count >= MAX_ACTIVE_CARDS_PER_ACCOUNT) {
            throw new BusinessRuleException("Maximum card limit reached for this account");
        }
    }
    /**
     * Validate that the card is unique
     * @param account
     * @param cardFormat
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
     * Validate Card Status Transition 
     * @param current
     * @param next
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
     * Apply Card Status change timestamp
     * @param card
     * @param newStatus
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
 
    private CreditCard buildCardEntity(CreditCardIssuanceRequest request,
            CreditAccount account, CreditCardProduct cardProduct, CardFormat cardFormat, IssuedBy issuedBy) {
        LocalDateTime now = LocalDateTime.now();
        int expiryYear = now.getYear() + cardProduct.getCardValidityYears();
        int expiryMonth = now.getMonthValue();
        Instant expiresAt = YearMonth.of(expiryYear, expiryMonth)
                .atEndOfMonth().atTime(23, 59, 59).toInstant(ZoneOffset.UTC);
 
        CardStatus initialStatus = cardFormat == CardFormat.VIRTUAL
                ? CardStatus.ACTIVE : CardStatus.PENDING_ACTIVATION;
 
        CreditCard card = new CreditCard();
        card.setCreditAccount(account);
        card.setCardProduct(cardProduct);
        card.setCardFormat(cardFormat);
        card.setCardStatus(initialStatus);
        card.setIssuanceReason(request.getIssuanceReason());
        card.setMaskedCardNumber(maskedCardNumberGenerator.generate(cardProduct.getNetworkType().name()));
        card.setIssuedAt(Instant.now());
        card.setExpiresAt(expiresAt);
        card.setExpiryMonth(expiryMonth);
        card.setExpiryYear(expiryYear);
        card.setOnlineEnabled(cardProduct.getOnlineTransactionsAllowed());
        card.setAtmEnabled(cardProduct.getAtmWithdrawalAllowed());
        card.setInternationalEnabled(cardProduct.getInternationalUsageAllowed());
        card.setIssuedBy(issuedBy.name());
        if (cardFormat == CardFormat.VIRTUAL) card.setActivatedAt(Instant.now());
        return card;
    }
 
    /**
     * 
     * @param principal
     * @param card
     */
    private void validateCustomerCardOwnership(CustomUserPrincipal principal, CreditCard card) {
        Customer customer = customerService.getCustomerByUserId(principal.getUserId());
        if (!card.getCreditAccount().getCustomer().getCustomerId().equals(customer.getCustomerId())) {
            throw new AccessDeniedException("Access denied to this card");
        }
    }
 
    private enum IssuedBy { CUSTOMER, ADMIN }

    
}