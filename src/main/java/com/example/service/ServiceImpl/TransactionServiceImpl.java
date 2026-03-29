package com.example.service.ServiceImpl;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

import com.example.dto.request.TransactionRequest;
import com.example.dto.response.TransactionDetailResponse;
import com.example.dto.response.TransactionSummaryResponse;
import com.example.entity.CreditAccount;
import com.example.entity.CreditCard;
import com.example.entity.CreditCardProduct;
import com.example.entity.Customer;
import com.example.entity.Transaction;
import com.example.enums.CardStatus;
import com.example.enums.Currency;
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
import com.example.util.ReferenceNumberGenerator;

import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link TransactionService}.
 *
 * <p>Currently supports INR only. All declined transactions are persisted for audit purposes.
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
     * Posts a transaction against the customer's card.
     * Runs all eligibility checks and either approves or declines.
     * Both outcomes are persisted for audit purposes.
     */
	@Override
	public TransactionSummaryResponse postTransaction(UUID userId, UUID cardId, TransactionRequest request) {

		//1. Basic validations
		if(request.getAmount().compareTo(BigDecimal.ZERO)<=0) {
			throw new BadRequestException("Transaction amount  must be greater than ZERO");
		}
		//2. Transaction channel check
	    if (request.getTransactionChannel() == null) {
	        throw new BadRequestException("Transaction channel is required");
	    }
		
		//3. Customer check
		Customer customer = customerService.getCustomerByUserId(userId);
		
	    
		//5. Delegate card lookup to CreditCardService
		CreditCard creditCard = creditCardService.getCardEntity(cardId);
		//6. card must belongs to same account 
		
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
		
		//9. Balance check(check Sufficient credit limit available or not)--Only for debit types
		if (isDebit(txnType) && creditAccount.getAvailableBalance().compareTo(request.getAmount()) < 0) {
			return handleDeclined(creditCard, creditAccount, txnType, txnChannel, request,
					"Insufficent balance");

		}
		
		//10. Card Expiry Check
		if (creditCard.getExpiresAt() != null &&
			    creditCard.getExpiresAt().isBefore(Instant.now())) {

			    return handleDeclined(
			            creditCard, creditAccount, txnType, txnChannel,
			            request, "Card is expired"
			    );
			}
		
		
		
		//7. Daily Limit check
		
		BigDecimal dailyLimit = resolveDailyLimit(creditCard.getCardProduct(), txnType,txnChannel);
        
		if (dailyLimit != null) {
            BigDecimal spentToday = getSpentToday(creditCard.getCardId(), txnType,txnChannel);
            if (spentToday.add(request.getAmount()).compareTo(dailyLimit) > 0) {
                BigDecimal remaining = dailyLimit.subtract(spentToday).max(BigDecimal.ZERO);
                return handleDeclined(creditCard, creditAccount, txnType, txnChannel, request,
                        "Daily limit exceeded. Remaining: ₹" + remaining);
            }
        }
		
		//All validation check completed
		
		//Balance calculation 
		BigDecimal balanceBefore = creditAccount.getAvailableBalance();
	    BigDecimal balanceAfter = isDebit(txnType)
	            ? balanceBefore.subtract(request.getAmount())
	            : balanceBefore.add(request.getAmount());
		
		//Real time balance Deduction
	    if (isDebit(txnType)) {
	        creditAccountService.deductBalance(creditAccount.getAccountId(), request.getAmount());
	    } else {
	        creditAccountService.addBalance(creditAccount.getAccountId(), request.getAmount());
	    }
		
	    Transaction txn = buildTransaction(
	            creditCard, creditAccount,
	            txnType, txnChannel,
	            TransactionStatus.APPROVED,
	            request, balanceBefore, balanceAfter, null
	    );

        transactionRepository.save(txn);

        return transactionMapper.toSummaryResponse(txn);
	}

	/**
	 * Get All Transaction by account (customer)
	 */
	@Override
	@Transactional(readOnly = true)
	public List<TransactionSummaryResponse> getAccountTransactions(
	        UUID userId,
	        UUID accountId,
	        TransactionStatus status,
	        TransactionType type,
	        UUID cardId) {

	    Customer customer = customerService.getCustomerByUserId(userId);
	    CreditAccount account = creditAccountService.getAccountEntity(accountId);

	    validateAccountOwnership(customer, account);

	    List<Transaction> transactions =
	            transactionRepository.findByFilters(accountId, status, type, cardId);

	    return transactions.stream()
	                    .map(transactionMapper::toSummaryResponse)
	                    .toList();
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

	// GET ALL TRANSACTIONS (Admin)
//    @Override
//    public List<TransactionSummaryResponse> getAllTransactions(
//            TransactionStatus status,
//            TransactionType type,
//            UUID accountId,
//            UUID userId) {
//
//        return transactionRepository.findAllWithFilters(status, type, accountId, userId)
//                .stream()
//                .map(transactionMapper::toSummaryResponse)
//                .toList();
//    }
//
//    @Override
//    public TransactionDetailResponse getTransactionById(UUID transactionId) {
//        return transactionMapper.toResponse(findTransactionById(transactionId));
//    }
	
	//------------------------------------Helper Method--------------
	/**
	 * Account Ownership Validation
	 * @param customer
	 * @param account
	 */
	private void validateAccountOwnership(Customer customer, CreditAccount account) {
        if (!account.getCustomer().getCustomerId().equals(customer.getCustomerId())) {
            throw new AccessDeniedException("Access denied to this account");
        }
    }
	/**
     * Builds a DECLINED transaction response.
     * Balance is unchanged — balanceBefore == balanceAfter.
     * Declined transactions are always persisted for audit trail.
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
     * Builds a Transaction entity for both APPROVED and DECLINED cases.
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
        txn.setReferenceNumber(referenceNumberGenerator.generate());
        txn.setTransactionTime(Instant.now());

        return txn;
    }
 
    
    
    /**
     * Resolves daily spend limit based on transaction type.
     * PURCHASE → posDailyLimit
     * ONLINE   → ecommerceDailyLimit
     */
    private BigDecimal resolveDailyLimit(CreditCardProduct cardProduct,
                                          TransactionType txnType,TransactionChannel txnChannel) {
    	
    	if (txnType != TransactionType.PURCHASE) return null;
    	
    	return switch (txnChannel) {
            case POS -> cardProduct.getPosDailyLimit();
            case ONLINE ->cardProduct.getEcommerceDailyLimit();
            case ATM->cardProduct.getAtmDailyLimit();
            default -> null;
        };
    }

    /**
     * Daily Spent check
     * @param cardId
     * @param txnType
     * @return
     */
    private BigDecimal getSpentToday(UUID cardId, TransactionType txnType,TransactionChannel txnChannel) {
        Instant startOfDay = Instant.now().truncatedTo(ChronoUnit.DAYS);
        BigDecimal spent = transactionRepository.sumApprovedAmountByCardAndTypeAndChannelAfter(cardId, txnType,txnChannel, startOfDay);
        return spent != null ? spent : BigDecimal.ZERO;
    }
    
    /**
     * Fetch Tranction By id
     * @param transactionId
     * @return
     */
    private Transaction findTransactionById(UUID transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + transactionId));
    }
    /*
     * is the transaction is debit or credit
     */
    private boolean isDebit(TransactionType type) {
        return switch (type) {
            case PURCHASE, FEE, INTEREST -> true;
            case REFUND, PAYMENT -> false;
        };
    }
    
    private void validateChannel(CreditCard creditCard,TransactionChannel txnChannel) {
    	CreditCardProduct cardProduct = creditCard.getCardProduct();
    	
    	switch(txnChannel) {
    	case ONLINE ->{
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
    	}
    }

	
	
}
