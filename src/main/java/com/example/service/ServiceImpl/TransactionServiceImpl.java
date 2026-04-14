package com.example.service.ServiceImpl;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.example.dto.request.TransactionRequest;
import com.example.dto.response.TransactionDetailResponse;
import com.example.dto.response.TransactionSummaryResponse;
import com.example.entity.CreditAccount;
import com.example.entity.CreditCard;
import com.example.entity.CreditCardProduct;
import com.example.entity.Customer;
import com.example.entity.Payment;
import com.example.entity.Transaction;
import com.example.enums.CardStatus;
import com.example.enums.Currency;
import com.example.enums.PaymentMethod;
import com.example.enums.TransactionChannel;
import com.example.enums.TransactionStatus;
import com.example.enums.TransactionType;
import com.example.exception.AccessDeniedException;
import com.example.exception.BadRequestException;
import com.example.exception.ResourceNotFoundException;
import com.example.mapper.TransactionMapper;
import com.example.repository.TransactionRepository;
import com.example.service.CreditAccountService;
import com.example.service.CreditCardService;
import com.example.service.CustomerService;
import com.example.service.TransactionService;
import com.example.specification.TransactionSpecification;
import com.example.util.ReferenceNumberGenerator;

import org.springframework.transaction.annotation.Transactional;

/**
 * Service responsible for handling credit card transactions.
 *
 * <p>This service processes all card-based financial activities such as purchases,
 * refunds, and system-generated transactions (e.g., payments, fees, interest).</p>
 *
 * <p><b>Key Responsibilities:</b></p>
 * <ul>
 *     <li>Authorize and post transactions against a credit account</li>
 *     <li>Validate card status, limits, and channel permissions</li>
 *     <li>Enforce daily spend limits</li>
 *     <li>Persist both approved and declined transactions for audit</li>
 *     <li>Provide transaction history with filtering and pagination</li>
 * </ul>
 *
 * <p><b>Design Principles:</b></p>
 * <ul>
 *     <li>All transactions are persisted (including declined) for audit compliance</li>
 *     <li>Balance updates occur only for approved debit transactions</li>
 *     <li>Strict ownership validation prevents unauthorized access</li>
 * </ul>
 *
 * <p><b>Note:</b> Currently supports only INR currency.</p>
 */

@Service
@Transactional
public class TransactionServiceImpl  implements TransactionService{
	
	
	//now it is only supporting INR 
	 private static final Currency CURRENCY = Currency.INR;
	

	 private final TransactionRepository transactionRepository;
	    private final CustomerService customerService;
	    private final CreditCardService creditCardService;
	    private final CreditAccountService creditAccountService;
	    private final TransactionMapper transactionMapper;
	    private final ReferenceNumberGenerator referenceNumberGenerator;

	    public TransactionServiceImpl(
	            TransactionRepository transactionRepository,
	            CustomerService customerService,
	            CreditCardService creditCardService,
	            CreditAccountService creditAccountService,
	            TransactionMapper transactionMapper,
	            ReferenceNumberGenerator referenceNumberGenerator) {

	        this.transactionRepository = transactionRepository;
	        this.customerService = customerService;
	        this.creditCardService = creditCardService;
	        this.creditAccountService = creditAccountService;
	        this.transactionMapper = transactionMapper;
	        this.referenceNumberGenerator = referenceNumberGenerator;
	    }
	

	/**
	 * Processes a card transaction request and determines whether it should be approved or declined.
	 *
	 * <p><b>Execution Flow:</b></p>
	 * <ol>
	 *     <li>Validate request (amount, type, channel)</li>
	 *     <li>Verify customer ownership of card</li>
	 *     <li>Check card status (must be ACTIVE)</li>
	 *     <li>Validate channel permissions (ATM, POS, ONLINE)</li>
	 *     <li>Check card expiry</li>
	 *     <li>Check available credit (for debit transactions)</li>
	 *     <li>Validate daily spending limits</li>
	 *     <li>Update account balance (if approved)</li>
	 *     <li>Persist transaction (approved or declined)</li>
	 * </ol>
	 *
	 * <p><b>Approval Rules:</b></p>
	 * <ul>
	 *     <li>Only debit transactions reduce available balance</li>
	 *     <li>Credit transactions (e.g., refund) increase balance</li>
	 * </ul>
	 *
	 * <p><b>Decline Handling:</b></p>
	 * <ul>
	 *     <li>Declined transactions are still stored for audit purposes</li>
	 *     <li>Balance remains unchanged</li>
	 * </ul>
	 *
	 * @param userId  the user initiating the transaction
	 * @param cardId  the credit card used
	 * @param request transaction request payload
	 * @return transaction summary response
	 *
	 * @throws BadRequestException     if request is invalid
	 * @throws AccessDeniedException   if card does not belong to user
	 */
	@Override
	public TransactionSummaryResponse postTransaction(UUID userId, UUID cardId, TransactionRequest request) {

		//1. Basic validations
		if(request.getAmount().compareTo(BigDecimal.ZERO)<=0) {
			throw new BadRequestException("Transaction amount  must be greater than ZERO");
		}
		//2. Transaction channel check
		if (request.getTransactionType() == null) {
            throw new BadRequestException("Transaction type required");
        }
	    if (request.getTransactionChannel() == null) {
	        throw new BadRequestException("Transaction channel is required");
	    }
	    if (request.getTransactionReference() == null ||
	            request.getTransactionReference().isBlank()) {
	            throw new BadRequestException("Transaction reference is required");
	        }
	    // ❌ BLOCK PAYMENT TYPE (important)
        if (request.getTransactionType() == TransactionType.PAYMENT) {
            throw new BadRequestException("Use Payment API for bill payments");
        }
		
		//3. Customer check
		Customer customer = customerService.getCustomerByUserId(userId);
		//5. Delegate Credit card lookup to CreditCardService
		CreditCard creditCard = creditCardService.getCardEntity(cardId);
		//6. card must belong to same user
		if(! creditCard.getCreditAccount().getCustomer().getCustomerId().equals(customer.getCustomerId())) {
			throw new AccessDeniedException("Access denied to this card");
		}
		
		CreditAccount creditAccount = creditCard.getCreditAccount();
		
		TransactionType txnType = request.getTransactionType();
		TransactionChannel txnChannel = request.getTransactionChannel();
		
		//7.Card Status Check (Must be active)
		if(creditCard.getCardStatus() != CardStatus.ACTIVE) {
			return handleDeclined
					(creditCard, creditAccount, txnType, txnChannel, request, "Card is not Active. Current Status:"+creditCard.getCardStatus());			
		}
		
		//8. Channel Validation
		validateChannel(creditCard, txnChannel);
		
		//10. Card Expiry Check
				if (creditCard.getExpiresAt() != null &&
					    creditCard.getExpiresAt().isBefore(Instant.now())) {

					    return handleDeclined(
					            creditCard, creditAccount, txnType, txnChannel,
					            request, "Card is expired"
					    );
					}
		
		//9. Balance check(check Sufficient credit limit available or not)--Only for debit types
		if (isDebit(txnType) && creditAccount.getAvailableBalance().compareTo(request.getAmount()) < 0) {
			return handleDeclined(creditCard, creditAccount, txnType, txnChannel, request,
					"Insufficient balance");

		}
		
		//7. Daily Limit check
		
		BigDecimal dailyLimit = resolveDailyLimit(creditCard.getCardProduct(), txnType,txnChannel);
        
		if (dailyLimit != null) {
            BigDecimal spentToday = getSpentToday(creditCard.getCardId(), txnType,txnChannel);
            if (spentToday.add(request.getAmount()).compareTo(dailyLimit) > 0) {
                BigDecimal remaining = dailyLimit.subtract(spentToday).max(BigDecimal.ZERO);
                return handleDeclined(creditCard, creditAccount, txnType, txnChannel, request,
                        "Daily limit exceeded. Remaining: " + remaining);
            }
        }
		
		//=============All validation check completed================
		//Balance calculation 
		BigDecimal balanceBefore = creditAccount.getAvailableBalance();
		if (isDebit(txnType)) {
		    // PURCHASE, FEE, INTEREST
		    creditAccountService.deductBalance(
		            creditAccount.getAccountId(),
		            request.getAmount()
		    );
		} else {
		    // REFUND, PAYMENT
		    creditAccountService.addBalance(
		            creditAccount.getAccountId(),
		            request.getAmount()
		    );
		}
		// Compute balance after (based on actual operation)
	    BigDecimal balanceAfter = isDebit(txnType)
	            ? balanceBefore.subtract(request.getAmount())// PURCHASE → increase debt
	            : balanceBefore.add(request.getAmount());// PAYMENT → reduce debt

		
	    Transaction txn = buildTransaction(
	            creditCard, creditAccount,
	            txnType, txnChannel,
	            TransactionStatus.APPROVED,
	            request, balanceBefore, balanceAfter, null
	    );
	    
	    //Handle Race Condition
	    try {
	        transactionRepository.save(txn);
	    } catch (DataIntegrityViolationException ex) {

	        Transaction existing = transactionRepository
	                .findByTransactionReference(request.getTransactionReference())
	                .orElseThrow();

	        return transactionMapper.toSummaryResponse(existing);
	    }

        return transactionMapper.toSummaryResponse(txn);
	}

	/**
	 * Retrieves paginated transaction history for a specific account.
	 *
	 * <p>Supports filtering by transaction status, type, and card ID.</p>
	 *
	 * <p><b>Features:</b></p>
	 * <ul>
	 *     <li>Pagination for memory efficiency</li>
	 *     <li>Sorting by transaction time (latest first)</li>
	 *     <li>Dynamic filtering using specification pattern</li>
	 * </ul>
	 *
	 * <p><b>Security:</b></p>
	 * <ul>
	 *     <li>Ensures account belongs to requesting user</li>
	 * </ul>
	 *
	 * @param userId   user requesting data
	 * @param accountId account identifier
	 * @param status   optional transaction status filter
	 * @param type     optional transaction type filter
	 * @param cardId   optional card filter
	 * @param page     page number (0-based)
	 * @param size     page size
	 * @return paginated transaction summaries
	 */
	@Override
	@Transactional(readOnly = true)
	public Page<TransactionSummaryResponse> getAccountTransactions(
	        UUID userId,
	        UUID accountId,
	        TransactionStatus status,
	        TransactionType type,
	        UUID cardId,
	        int page,
	        int size) {

	    Customer customer = customerService.getCustomerByUserId(userId);
	    CreditAccount account = creditAccountService.getAccountEntity(accountId);

	    validateAccountOwnership(customer, account);

	    if (page < 0) {
	        throw new BadRequestException("Page must be >= 0");
	    }

	    if (size <= 0 || size > 100) {
	        throw new BadRequestException("Size must be between 1 and 100");
	    }

	    Pageable pageable = PageRequest.of(
	            page,
	            size,
	            Sort.by(Sort.Direction.DESC, "transactionTime")
	    );

	    Specification<Transaction> spec =
	            TransactionSpecification.withFilters(
	                    accountId,
	                    status,
	                    type,
	                    cardId
	            );

	    return transactionRepository
	            .findAll(spec, pageable)
	            .map(transactionMapper::toSummaryResponse);
	}
	/**
	 * GET TRANSACTION BY ID (Customer)
	 */

    @Override
    @Transactional(readOnly = true)
    public  TransactionDetailResponse getAccountTransactionById(
            UUID userId,
            UUID accountId,
            UUID transactionId) {

        Customer customer = customerService.getCustomerByUserId(userId);
        CreditAccount account = creditAccountService.getAccountEntity(accountId);

        validateAccountOwnership(customer, account);

        Transaction txn = findTransactionById(transactionId);

        if (!txn.getAccount().getAccountId().equals(accountId)) {
            throw new BadRequestException("Transaction does not belong to this account");
        }

        return transactionMapper.toResponse(txn);
    }
	/**
	 * Records a payment as a transaction in the ledger system.
	 *
	 * <p>This method is invoked internally by the payment service after a successful payment.
	 * It creates a transaction entry representing the payment and updates account balances.</p>
	 *
	 * <p><b>Behavior:</b></p>
	 * <ul>
	 *     <li>Creates a PAYMENT-type transaction</li>
	 *     <li>Maps payment method to transaction channel</li>
	 *     <li>Captures balance before and after payment</li>
	 *     <li>Links transaction to the payment entity</li>
	 * </ul>
	 *
	 * <p><b>Note:</b> This method does not perform balance updates — it assumes the account
	 * has already been updated by the payment service.</p>
	 *
	 * @param account        credit account
	 * @param payment        payment entity
	 * @param balanceBefore  account balance before payment
	 * @param balanceAfter   account balance after payment
	 * @return transaction summary response
	 */
    @Override
    public TransactionSummaryResponse recordPayment(
            CreditAccount account,
            Payment payment,
            BigDecimal balanceBefore,
            BigDecimal balanceAfter) {

        // 🔒 1. Idempotency check
//        if (transactionRepository.existsByPayment(payment)) {
//            throw new IllegalStateException(
//                    "Transaction already exists for payment: " + payment.getPaymentId()
//            );
//        }

        // 🛡️ 2. Basic validation
        if (payment.getAmount() == null ||
            payment.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Invalid payment amount");
        }

        // 🧠 3. Resolve channel
        TransactionChannel channel = resolveChannel(payment.getPaymentMethod());

        // 🏗️ 4. Build transaction
        Transaction txn = new Transaction();

        txn.setCard(null);
        txn.setAccount(account);
        txn.setTransactionType(TransactionType.PAYMENT);
        txn.setTransactionChannel(channel);
        txn.setTransactionStatus(TransactionStatus.APPROVED);
        txn.setAmount(payment.getAmount());
        txn.setCurrency(Currency.INR);

        txn.setBalanceBefore(balanceBefore);
        txn.setBalanceAfter(balanceAfter);

        txn.setPayment(payment);

        txn.setReferenceNumber(payment.getReferenceId());

        txn.setTransactionTime(
                payment.getPaidAt() != null ? payment.getPaidAt() : Instant.now()
        );

        transactionRepository.save(txn);

        return transactionMapper.toSummaryResponse(txn);
    }
    
    private TransactionChannel resolveChannel(PaymentMethod method) {

        if (method == null) {
            return TransactionChannel.ONLINE; 
        }

        return switch (method) {
            case UPI, NET_BANKING, NEFT, RTGS -> TransactionChannel.ONLINE;
            case DEBIT_CARD -> TransactionChannel.POS;
        };
    }

	
	//------------------------------------Helper Method--------------
	/**
	 * Account Ownership Validation
	 * @param customer The Logged in Customer
	 * @param account The Credit account
	 * @throws AccessDeniedException if the Account is not belongs to that user
	 */
	private void validateAccountOwnership(Customer customer, CreditAccount account) {
        if (!account.getCustomer().getCustomerId().equals(customer.getCustomerId())) {
            throw new AccessDeniedException("Access denied to this account");
        }
    }
	/**
	 * Creates and persists a declined transaction.
	 *
	 * <p>This method ensures that even rejected transactions are recorded
	 * for audit and traceability purposes.</p>
	 *
	 * <p><b>Behavior:</b></p>
	 * <ul>
	 *     <li>Balance remains unchanged</li>
	 *     <li>Decline reason is recorded</li>
	 *     <li>Status is set to DECLINED</li>
	 * </ul>
	 *
	 * @param card    credit card
	 * @param account credit account
	 * @param type    transaction type
	 * @param channel transaction channel
	 * @param request original request
	 * @param reason  reason for decline
	 * @return transaction summary response
	 */
	
	private TransactionSummaryResponse handleDeclined(
	        CreditCard card,
	        CreditAccount account,
	        TransactionType type,
	        TransactionChannel channel,
	        TransactionRequest request,
	        String reason) {

	    BigDecimal balance = account.getAvailableBalance();

	    Transaction txn = buildTransaction(
	            card, account, type, channel,
	            TransactionStatus.DECLINED,
	            request,
	            balance,
	            balance,
	            reason
	    );

	    transactionRepository.save(txn);

	    return transactionMapper.toSummaryResponse(txn);
	}

	/**
	 * Constructs a {@link Transaction} entity for both approved and declined scenarios.
	 *
	 * <p>This method centralizes transaction creation logic to ensure consistency
	 * across all transaction types.</p>
	 *
	 * <p><b>Fields populated:</b></p>
	 * <ul>
	 *     <li>Card and account references</li>
	 *     <li>Transaction type, channel, and status</li>
	 *     <li>Amount and currency</li>
	 *     <li>Merchant details</li>
	 *     <li>Balance before and after transaction</li>
	 *     <li>Reference identifiers</li>
	 * </ul>
	 *
	 * @param creditCard      credit card used
	 * @param creditAccount   associated account
	 * @param txnType         transaction type
	 * @param txnChannel      transaction channel
	 * @param status          transaction status
	 * @param request         transaction request
	 * @param balanceBefore   balance before transaction
	 * @param balanceAfter    balance after transaction
	 * @param declineReason   reason for decline (if any)
	 * @return constructed transaction entity
	 */
    private Transaction buildTransaction(CreditCard creditCard,
                                          CreditAccount creditAccount,
                                          TransactionType txnType,
                                          TransactionChannel txnChannel,
                                          TransactionStatus status,
                                          TransactionRequest request,
                                          BigDecimal balanceBefore,
                                          BigDecimal balanceAfter,
                                          String declineReason) {
        Transaction txn = new Transaction();
        txn.setCard(creditCard);
        txn.setAccount(creditAccount);
        txn.setTransactionType(txnType);
        txn.setTransactionChannel(txnChannel);
        txn.setTransactionStatus(status);
        txn.setAmount(request.getAmount());
        txn.setCurrency(CURRENCY);
        txn.setMerchantName(request.getMerchantName());
        txn.setMerchantCategoryCode(request.getMerchantCategoryCode());
        txn.setMerchantCategoryName(request.getMerchantCategoryName());
        txn.setBalanceBefore(balanceBefore);
        txn.setBalanceAfter(balanceAfter);
        txn.setDeclineReason(declineReason);
        txn.setTransactionReference(request.getTransactionReference());
        txn.setReferenceNumber(
        	    request.getTransactionReference() != null
        	        ? request.getTransactionReference()
        	        : referenceNumberGenerator.generate()
        	);
        txn.setTransactionTime(Instant.now());

        return txn;
    }
	/**
	 * Determines the daily spending limit based on transaction type and channel.
	 *
	 * <p>Applicable only for PURCHASE transactions.</p>
	 *
	 * <p><b>Channel Mapping:</b></p>
	 * <ul>
	 *     <li>POS → POS daily limit</li>
	 *     <li>ONLINE → E-commerce daily limit</li>
	 *     <li>ATM → ATM withdrawal limit</li>
	 * </ul>
	 *
	 * @param cardProduct card product configuration
	 * @param txnType     transaction type
	 * @param txnChannel  transaction channel
	 * @return daily limit or null if not applicable
	 */
     private BigDecimal resolveDailyLimit(CreditCardProduct cardProduct,
                                          TransactionType txnType,TransactionChannel txnChannel) {
    	
    	if (txnType != TransactionType.PURCHASE) return null;
    	
    	return switch (txnChannel) {
            case POS -> cardProduct.getPosDailyLimit();
            case ONLINE ->cardProduct.getEcommerceDailyLimit();
            case ATM->cardProduct.getAtmDailyLimit();
		default -> throw new IllegalArgumentException("Unexpected value: " + txnChannel);
        };
     }

    /**
     * Daily Spent check
     * @param cardId Credit Card Used
     * @param txnType Transaction Type
     * @return Daily spent amount
     */
    private BigDecimal getSpentToday(UUID cardId, TransactionType txnType,TransactionChannel txnChannel) {
        Instant startOfDay = Instant.now().truncatedTo(ChronoUnit.DAYS);
        BigDecimal spent = transactionRepository.sumApprovedAmountByCardAndTypeAndChannelAfter(cardId, txnType,txnChannel, startOfDay);
        return spent != null ? spent : BigDecimal.ZERO;
    }
    
    /**
     * Fetch Transaction By id
     * @param transactionId Transaction Identifier
     * @return return Transaction details
	 * @throws ResourceNotFoundException if no such record available with this id
     */
    private Transaction findTransactionById(UUID transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + transactionId));
    }
    /**
     * Is the transaction is debit or credit
     * @param type TransactionType
     * @return Is it Debit or Credit
     */
    private boolean isDebit(TransactionType type) {
        return switch (type) {
            case PURCHASE, FEE, INTEREST -> true;
            case REFUND, PAYMENT -> false;
        };
    }
	/**
	 * Validates whether a transaction channel is allowed for the given card and product.
	 *
	 * <p><b>Checks:</b></p>
	 * <ul>
	 *     <li>Product-level channel permissions</li>
	 *     <li>Card-level channel enablement</li>
	 * </ul>
	 *
	 * @param creditCard credit card
	 * @param txnChannel transaction channel
	 * @throws BadRequestException if channel is not allowed
	 */
	private void validateChannel(CreditCard creditCard, TransactionChannel txnChannel) {
		CreditCardProduct cardProduct = creditCard.getCardProduct();

		switch (txnChannel) {
		case ONLINE -> {
			if (!cardProduct.getOnlineTransactionsAllowed()) {
				throw new BadRequestException("Online Transaction is  not allowed for this product");
			}
			if (!creditCard.getOnlineEnabled()) {
				throw new BadRequestException("Online Transaction is disabled on this card");
			}
		}
		case ATM -> {
			if (!cardProduct.getAtmWithdrawalAllowed()) {
				throw new BadRequestException("ATM Transaction is not allowed for this product");
			}
			if (!creditCard.getAtmEnabled()) {
				throw new BadRequestException("ATM Transaction is disabled on this card");
			}
		}
		case POS -> {
			// POS usually allowed, can extend later
		}
        case SYSTEM -> {
            // System transactions are internal (interest, fee)
        }
		}
	}



	@Override
	@Transactional(readOnly = true)
	public TransactionSummaryResponse getByTransactionReference(
	        String transactionReference) {

	    Transaction txn = transactionRepository
	            .findByTransactionReference(transactionReference)
	            .orElseThrow(() ->
	                    new ResourceNotFoundException(
	                            "Transaction not found"));

	    return transactionMapper.toSummaryResponse(txn);
	}
	
	@Override
	public TransactionSummaryResponse postSystemTransaction(
	        CreditAccount account,
	        TransactionType type,
	        BigDecimal amount,
	        String description,
	        String reference) {

	    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
	        throw new BadRequestException("Invalid system transaction amount");
	    }

	    // 🔒 IDEMPOTENCY
	    Optional<Transaction> existing =
	            transactionRepository.findByReferenceNumber(reference);

	    if (existing.isPresent()) {
	        return transactionMapper.toSummaryResponse(existing.get());
	    }

	    BigDecimal balanceBefore = account.getAvailableBalance();

	    if (isDebit(type)) {
	        creditAccountService.deductBalance(account.getAccountId(), amount);
	    }

	    BigDecimal balanceAfter = isDebit(type)
	            ? balanceBefore.subtract(amount)
	            : balanceBefore.add(amount);

	    Transaction txn = new Transaction();
	    txn.setCard(null);
	    txn.setAccount(account);
	    txn.setTransactionType(type);
	    txn.setTransactionChannel(TransactionChannel.SYSTEM);
	    txn.setTransactionStatus(TransactionStatus.APPROVED);
	    txn.setAmount(amount);
	    txn.setCurrency(Currency.INR);

	    
	    txn.setMerchantName(description != null ? description : type.name());
	    txn.setBalanceBefore(balanceBefore);
	    txn.setBalanceAfter(balanceAfter);

	    txn.setReferenceNumber(reference); // 🔥 deterministic
	    txn.setTransactionTime(Instant.now());

	    transactionRepository.save(txn);

	    return transactionMapper.toSummaryResponse(txn);
	}

}
