package com.example.service.ServiceImpl;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.dto.request.CreditCardIssuanceRequest;
import com.example.dto.request.CreditCardStatusUpdateRequest;
import com.example.dto.response.ApiResponse;
import com.example.dto.response.CardProductResponse;
import com.example.dto.response.CreditCardIssuanceResponse;
import com.example.dto.response.CreditCardResponse;
import com.example.entity.CreditAccount;
import com.example.entity.CreditCard;
import com.example.entity.CreditCardProduct;
import com.example.entity.Customer;
import com.example.entity.User;
import com.example.enums.AccountStatus;
import com.example.enums.CardFormat;
import com.example.enums.CardStatus;
import com.example.enums.ProductStatus;
import com.example.enums.UserRole;
import com.example.exception.AccessDeniedException;
import com.example.exception.BusinessRuleException;
import com.example.exception.ConflictException;
import com.example.exception.ProfileNotCreatedException;
import com.example.exception.ResourceNotFoundException;
import com.example.exception.UserNotFoundException;
import com.example.mapper.CardProductMapper;
import com.example.mapper.CreditCardMapper;
import com.example.repository.*;
import com.example.security.CustomUserPrincipal;
import com.example.service.CreditCardService;
import com.example.util.MaskedCardNumberGenerator;

@Service
@Transactional
public class CreditCardServiceImpl implements CreditCardService {

    // Max total active cards per account (PENDING_ACTIVATION + ACTIVE)
    private static final int MAX_ACTIVE_CARDS_PER_ACCOUNT = 5;

    private final CreditCardRepository cardRepository;
    private final CreditAccountRepository accountRepository;
    private final CreditCardProductRepository cardProductRepository;
    private final UserRepository userRepository;
    private final CreditCardMapper cardMapper;
    private final CardProductMapper cardProductMapper;
    private final MaskedCardNumberGenerator maskedCardNumberGenerator;

    public CreditCardServiceImpl(CreditCardRepository cardRepository,
                                 CreditAccountRepository accountRepository,
                                 CreditCardProductRepository cardProductRepository,
                                 UserRepository userRepository,
                                 CreditCardMapper cardMapper,
                                 CardProductMapper cardProductMapper,
                                 MaskedCardNumberGenerator maskedCardNumberGenerator) {
        this.cardRepository = cardRepository;
        this.accountRepository = accountRepository;
        this.cardProductRepository = cardProductRepository;
        this.userRepository = userRepository;
        this.cardMapper = cardMapper;
        this.cardProductMapper = cardProductMapper;
        this.maskedCardNumberGenerator = maskedCardNumberGenerator;
    }

    /**
     * GET AVAILABLE CARD PRODUCTS (Customer)
     * Customer can only pick card products that belong to
     * the same CreditProduct as their account
     */
    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<CardProductResponse>> getAvailableCardProducts(UUID userId, UUID accountId) {

        Customer customer = getCustomerFromUser(userId);
        CreditAccount account = findAccountById(accountId);

        validateAccountOwnership(account, customer);
        validateAccountActive(account);

        List<CardProductResponse> products = cardProductRepository
                .findAllByStatus(ProductStatus.ACTIVE)
                .stream()
                .map(cardProductMapper::toResponse)
                .collect(Collectors.toList());

        return new ApiResponse<>(Instant.now(), HttpStatus.OK.value(),
                "Available card products fetched successfully", products);
    }

    /**
     * ISSUE CARD — Customer self-issuance
     */
    @Override
    public ApiResponse<CreditCardResponse> issueCard(UUID userId, CreditCardIssuanceRequest request) {

        Customer customer = getCustomerFromUser(userId);
        CreditCard card = buildAndIssueCard(request, customer, IssuedBy.CUSTOMER);

        return new ApiResponse<>(Instant.now(), HttpStatus.CREATED.value(),
                "Card issued successfully", cardMapper.toResponse(card));
    }

    /**
     * ISSUE CARD — Admin on behalf of customer
     */
    @Override
    public ApiResponse<CreditCardResponse> issueCardByAdmin(CreditCardIssuanceRequest request) {

        CreditAccount account = findAccountById(request.getAccountId());
        CreditCard card = buildAndIssueCard(request, account.getCustomer(), IssuedBy.ADMIN);

        return new ApiResponse<>(Instant.now(), HttpStatus.CREATED.value(),
                "Card issued by admin successfully", cardMapper.toResponse(card));
    }

    /**
     * GET MY CARDS (Customer — all accounts)
     */
    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<CreditCardIssuanceResponse>> getMyCards(UUID userId) {

        Customer customer = getCustomerFromUser(userId);

        List<CreditCardIssuanceResponse> cards = cardRepository
                .findAllByCustomerCustomerId(customer.getCustomerId())
                .stream()
                .map(cardMapper::toIssueResponse)
                .collect(Collectors.toList());

        return new ApiResponse<>(Instant.now(), HttpStatus.OK.value(),
                "Cards fetched successfully", cards);
    }

    /**
     * GET CARDS BY ACCOUNT (Customer)
     */
    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<CreditCardResponse>> getMyCardsByAccount(UUID userId, UUID accountId) {

        Customer customer = getCustomerFromUser(userId);
        CreditAccount account = findAccountById(accountId);

        validateAccountOwnership(account, customer);

        List<CreditCardResponse> cards = cardRepository
                .findAllByCreditAccountAccountId(account.getAccountId())
                .stream()
                .map(cardMapper::toResponse)
                .collect(Collectors.toList());

        return new ApiResponse<>(Instant.now(), HttpStatus.OK.value(),
                "Cards fetched for account " + account.getAccountNumber(), cards);
    }

    /**
     * GET CARD DETAILS BY ID (Customer)
     */
    @Override
    @Transactional(readOnly = true)
    public ApiResponse<CreditCardResponse> getMyCardById(UUID userId, UUID cardId) {

        Customer customer = getCustomerFromUser(userId);
        CreditCard card = findCardById(cardId);

        if (!card.getCustomer().getCustomerId().equals(customer.getCustomerId())) {
            throw new AccessDeniedException("Access denied to this card");
        }

        return new ApiResponse<>(Instant.now(), HttpStatus.OK.value(),
                "Card fetched successfully", cardMapper.toResponse(card));
    }

    @Override
    public ApiResponse<List<CreditCardIssuanceResponse>> getCardsByStatusForUser(
            CustomUserPrincipal principal,
            CardStatus status) {

        if (principal.getRole() == UserRole.ADMIN) {
            return getCardsByStatus(status);
        }

        Customer customer = getCustomerFromUser(principal.getUserId());

        List<CreditCardIssuanceResponse> cards = cardRepository
                .findAllByCustomerCustomerIdAndCardStatus(customer.getCustomerId(), status)
                .stream()
                .map(cardMapper::toIssueResponse)
                .toList();

        return new ApiResponse<>(Instant.now(), HttpStatus.OK.value(),
                "Cards fetched for status: " + status, cards);
    }

    /**
     * GET ALL CARDS (Admin)
     */
    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<CreditCardIssuanceResponse>> getAllCards() {

        List<CreditCardIssuanceResponse> cards = cardRepository.findAll()
                .stream()
                .map(cardMapper::toIssueResponse)
                .collect(Collectors.toList());

        return new ApiResponse<>(Instant.now(), HttpStatus.OK.value(),
                "All cards fetched successfully", cards);
    }

    /**
     * Fetch cards by account id (Admin)
     */
    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<CreditCardResponse>> getCardsByAccount(UUID accountId) {

        CreditAccount account = findAccountById(accountId);

        List<CreditCardResponse> cards = cardRepository
                .findAllByCreditAccountAccountId(account.getAccountId())
                .stream()
                .map(cardMapper::toResponse)
                .toList();

        return new ApiResponse<>(Instant.now(), HttpStatus.OK.value(),
                "Cards fetched for account " + account.getAccountNumber(), cards);
    }

    /**
     * GET CARDS BY STATUS (Admin)
     */
    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<CreditCardIssuanceResponse>> getCardsByStatus(CardStatus status) {

        List<CreditCardIssuanceResponse> cards = cardRepository
                .findAllByCardStatus(status)
                .stream()
                .map(cardMapper::toIssueResponse)
                .collect(Collectors.toList());

        return new ApiResponse<>(Instant.now(), HttpStatus.OK.value(),
                "Cards fetched for status: " + status, cards);
    }

    /**
     * GET CARD BY ID (Admin)
     */
    @Override
    @Transactional(readOnly = true)
    public ApiResponse<CreditCardResponse> getCardById(UUID cardId) {
        return new ApiResponse<>(Instant.now(), HttpStatus.OK.value(),
                "Card fetched successfully",
                cardMapper.toResponse(findCardById(cardId)));
    }

    /**
     * UPDATE CARD STATUS (Shared)
     */
    @Override
    public ApiResponse<CreditCardIssuanceResponse> updateCardStatusForUser(
            CustomUserPrincipal principal,
            UUID cardId,
            CreditCardStatusUpdateRequest request) {

        CreditCard card = findCardById(cardId);
        CardStatus currentStatus = card.getCardStatus();
        CardStatus newStatus = request.getStatus();

        if (principal.getRole() == UserRole.CUSTOMER) {

            Customer customer = getCustomerFromUser(principal.getUserId());

            if (!card.getCustomer().getCustomerId().equals(customer.getCustomerId())) {
                throw new AccessDeniedException("Access denied to this card");
            }

            if (!List.of(CardStatus.ACTIVE, CardStatus.BLOCKED).contains(newStatus)) {
                throw new BusinessRuleException(
                        "Customers can only change status to ACTIVE or BLOCKED");
            }
        }

        validateCardStatusTransition(currentStatus, newStatus);

        Instant now = Instant.now();

        switch (newStatus) {
            case ACTIVE -> {
                if (card.getActivatedAt() == null) {
                    card.setActivatedAt(now);
                }
            }
            case BLOCKED -> card.setBlockedAt(now);
            case CANCELLED -> card.setCancelledAt(now);
            case EXPIRED -> card.setExpiresAt(now);
            default -> {}
        }

        card.setCardStatus(newStatus);

        return new ApiResponse<>(Instant.now(), HttpStatus.OK.value(),
                "Card status updated from " + currentStatus + " to " + newStatus,
                cardMapper.toIssueResponse(cardRepository.save(card)));
    }

    /**
     * CORE ISSUANCE LOGIC (shared by customer + admin)
     */
    private CreditCard buildAndIssueCard(CreditCardIssuanceRequest request,
                                         Customer customer,
                                         IssuedBy issuedBy) {

        CreditAccount account = findAccountById(request.getAccountId());

        if (issuedBy == IssuedBy.CUSTOMER) {
            validateAccountOwnership(account, customer);
        }

        validateAccountActive(account);

        CreditCardProduct cardProduct = cardProductRepository
                .findById(request.getCardProductId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Card product not found"));

        if (cardProduct.getStatus() != ProductStatus.ACTIVE) {
            throw new BusinessRuleException("Card product is not active");
        }

        // IMPORTANT VALIDATION
        if (!cardProduct.getCreditProduct().getCreditProductId()
                .equals(account.getCreditProduct().getCreditProductId())) {
            throw new BusinessRuleException(
                    "Card product does not belong to account's credit product");
        }

        CardFormat cardFormat = request.getCardFormat();

        int activeCardCount = cardRepository
                .countByCreditAccountAccountIdAndCardStatusIn(
                        account.getAccountId(),
                        List.of(CardStatus.PENDING_ACTIVATION, CardStatus.ACTIVE));

        if (activeCardCount >= MAX_ACTIVE_CARDS_PER_ACCOUNT) {
            throw new BusinessRuleException("Maximum card limit reached");
        }

        if (cardFormat == CardFormat.VIRTUAL) {
            boolean exists = cardRepository
                    .existsByCreditAccountAccountIdAndCardFormatAndCardStatusIn(
                            account.getAccountId(),
                            CardFormat.VIRTUAL,
                            List.of(CardStatus.PENDING_ACTIVATION, CardStatus.ACTIVE));

            if (exists) {
                throw new BusinessRuleException("Virtual card already exists");
            }
        }

        LocalDateTime now = LocalDateTime.now();

        int expiryYear = now.getYear() + cardProduct.getCardValidityYears();
        int expiryMonth = now.getMonthValue();

        Instant expiresAt = java.time.YearMonth.of(expiryYear, expiryMonth)
                .atEndOfMonth()
                .atTime(23, 59, 59)
                .toInstant(ZoneOffset.UTC);

        CardStatus initialStatus = (cardFormat == CardFormat.VIRTUAL)
                ? CardStatus.ACTIVE
                : CardStatus.PENDING_ACTIVATION;

        CreditCard card = new CreditCard();

        card.setCreditAccount(account);
        card.setCustomer(customer);
        card.setCardProduct(cardProduct);
        card.setCardFormat(cardFormat);
        card.setCardStatus(initialStatus);
        card.setMaskedCardNumber(
                maskedCardNumberGenerator.generate(cardProduct.getNetworkType().name()));
        card.setIssuedAt(Instant.now());
        card.setExpiresAt(expiresAt);
        card.setIssuedBy(issuedBy.name());

        if (cardFormat == CardFormat.VIRTUAL) {
            card.setActivatedAt(Instant.now());
        }

        return cardRepository.save(card);
    }
    
    @Override
    public ApiResponse<CreditCardIssuanceResponse> activateCard(
            CustomUserPrincipal principal,
            UUID cardId) {

        CreditCard card = findCardById(cardId);

        validateOwnershipIfCustomer(principal, card);

        validateCardStatusTransition(card.getCardStatus(), CardStatus.ACTIVE);

        if (card.getActivatedAt() == null) {
            card.setActivatedAt(Instant.now());
        }

        card.setCardStatus(CardStatus.ACTIVE);

        return buildSuccessResponse(card, "Card activated successfully");
    }
    
    @Override
    public ApiResponse<CreditCardIssuanceResponse> blockCard(
            CustomUserPrincipal principal,
            UUID cardId,
            String reason) {

        CreditCard card = findCardById(cardId);

        validateOwnershipIfCustomer(principal, card);

        validateCardStatusTransition(card.getCardStatus(), CardStatus.BLOCKED);

        card.setBlockedAt(Instant.now());
        card.setCardStatus(CardStatus.BLOCKED);

        return buildSuccessResponse(card, "Card blocked successfully");
    }
    
    @Override
    public ApiResponse<CreditCardIssuanceResponse> unblockCard(
            CustomUserPrincipal principal,
            UUID cardId) {

        CreditCard card = findCardById(cardId);

        validateOwnershipIfCustomer(principal, card);

        validateCardStatusTransition(card.getCardStatus(), CardStatus.ACTIVE);

        card.setCardStatus(CardStatus.ACTIVE);

        return buildSuccessResponse(card, "Card unblocked successfully");
    }
    
    @Override
    public ApiResponse<CreditCardIssuanceResponse> cancelCard(
            CustomUserPrincipal principal,
            UUID cardId,
            String reason) {

        CreditCard card = findCardById(cardId);

        validateOwnershipIfCustomer(principal, card);

        validateCardStatusTransition(card.getCardStatus(), CardStatus.CANCELLED);

        card.setCancelledAt(Instant.now());
        card.setCardStatus(CardStatus.CANCELLED);

        return buildSuccessResponse(card, "Card cancelled successfully");
    }

    // PRIVATE HELPERS

    private Customer getCustomerFromUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (user.getCustomer() == null) {
            throw new ProfileNotCreatedException("Customer profile not found");
        }

        return user.getCustomer();
    }

    private CreditAccount findAccountById(UUID accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
    }

    private CreditCard findCardById(UUID cardId) {
        return cardRepository.findById(cardId)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found"));
    }

    private void validateAccountOwnership(CreditAccount account, Customer customer) {
        if (!account.getCustomer().getCustomerId().equals(customer.getCustomerId())) {
            throw new AccessDeniedException("Access denied to this account");
        }
    }

    private void validateAccountActive(CreditAccount account) {
        if (account.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new BusinessRuleException("Account must be ACTIVE");
        }
    }

    private void validateCardStatusTransition(CardStatus current, CardStatus next) {

        if (current == next) {
            throw new ConflictException("Card already in same status");
        }

        Set<CardStatus> allowed = VALID_TRANSITIONS.getOrDefault(current, Set.of());

        if (!allowed.contains(next)) {
        	throw new BusinessRuleException(
        		    "Invalid transition from " + current + " to " + next +
        		    ". Allowed: " + allowed
        		);
        }
    }
    
    private void validateOwnershipIfCustomer(
            CustomUserPrincipal principal,
            CreditCard card) {

        if (principal.getRole() == UserRole.CUSTOMER) {

            Customer customer = getCustomerFromUser(principal.getUserId());

            if (!card.getCustomer().getCustomerId()
                    .equals(customer.getCustomerId())) {
                throw new AccessDeniedException("Access denied to this card");
            }
        }
    }
    private ApiResponse<CreditCardIssuanceResponse> buildSuccessResponse(
            CreditCard card,
            String message) {

        return new ApiResponse<>(
                Instant.now(),
                HttpStatus.OK.value(),
                message,
                cardMapper.toIssueResponse(cardRepository.save(card))
        );
    }

    private enum IssuedBy {
        CUSTOMER, ADMIN
    }

    private static final Map<CardStatus, Set<CardStatus>> VALID_TRANSITIONS = Map.of(
            CardStatus.PENDING_ACTIVATION, Set.of(CardStatus.ACTIVE, CardStatus.CANCELLED),
            CardStatus.ACTIVE, Set.of(CardStatus.BLOCKED, CardStatus.CANCELLED, CardStatus.EXPIRED),
            CardStatus.BLOCKED, Set.of(CardStatus.ACTIVE, CardStatus.CANCELLED),
            CardStatus.EXPIRED, Set.of(CardStatus.CANCELLED),
            CardStatus.CANCELLED, Set.of()
    );
}