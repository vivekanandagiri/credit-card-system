package com.example.service.ServiceImpl;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.dto.request.CreditAccountStatusUpdateRequest;
import com.example.dto.response.CreditAccountResponse;
import com.example.entity.CreditAccount;
import com.example.entity.CreditCardApplication;
import com.example.entity.CreditProduct;
import com.example.entity.Customer;
import com.example.enums.AccountStatus;
import com.example.enums.ApplicationStatus;
import com.example.enums.UserRole;
import com.example.exception.BadRequestException;
import com.example.exception.BusinessRuleException;
import com.example.exception.ConflictException;
import com.example.exception.ResourceNotFoundException;
import com.example.mapper.CreditAccountMapper;
import com.example.repository.CreditAccountRepository;
import com.example.service.CreditAccountService;
import com.example.service.CustomerService;
import com.example.util.AccountNumberGenerator;

import lombok.extern.slf4j.Slf4j;


/**
 * Implementation of {@link CreditAccountService}.
 *
 * <p>Valid status transitions:
 * <ul>
 *   <li>ACTIVE → SUSPENDED, BLOCKED, CLOSED</li>
 *   <li>SUSPENDED → ACTIVE, BLOCKED, CLOSED</li>
 *   <li>BLOCKED → ACTIVE, CLOSED</li>
 *   <li>CLOSED → (terminal)</li>
 * </ul>
 */
@Service
@Transactional
@Slf4j
public class CreditAccountServiceImpl implements CreditAccountService {
	
	
	//constructor injection
	
	private static final int DEFAULT_STATEMENT_CYCLE_DAY = 5;
	
	private final CreditAccountRepository accountRepository; 
    private final AccountNumberGenerator accountNumberGenerator;
    private final CreditAccountMapper accountMapper;
    private final CustomerService customerService;                   
 
    public CreditAccountServiceImpl(
            CreditAccountRepository accountRepository,
            AccountNumberGenerator accountNumberGenerator,
            CreditAccountMapper accountMapper,
            CustomerService customerService) {
        this.accountRepository = accountRepository;
        this.accountNumberGenerator = accountNumberGenerator;
        this.accountMapper = accountMapper;
        this.customerService = customerService;
    }

	/**
	 * Auto Credit Account Creation after application approval
	 */
	@Transactional
	@Override
	public CreditAccountResponse createAccount(CreditCardApplication application) {
		
		// Validate application status
		if(application.getApplicationStatus()!=ApplicationStatus.APPROVED) {
			throw new BusinessRuleException( "Account can only be created for approved applications: "
                    + application.getApplicationId());
		}
		
		//duplicate account check
		if(accountRepository.existsByApplicationApplicationId(application.getApplicationId()))
		{
			throw new ConflictException("Account already exist for this application"+application.getApplicationId());
		}
		
		
		//account number generation
		String accountNumber = accountNumberGenerator.generate(
                application.getCreditProduct().getProductCode());
		
		if (application.getApprovedCreditLimit() == null 
		        || application.getApprovedCreditLimit().compareTo(BigDecimal.ZERO) <= 0) {
		    throw new BusinessRuleException("Invalid credit limit");
		}
		
		CreditAccount savedAccount = accountRepository.save(buildCreditAccount(application, accountNumber));
		
		return accountMapper.toResponse(savedAccount);
	}
	
	/**
	 * 
     * CUSTOMER → gets only their accounts
     * ADMIN → gets all accounts (optional filter by status)
     *
	 */
	@Override
	@Transactional(readOnly = true)
	public List<CreditAccountResponse> getAccounts(
	        UUID userId,
	        UserRole role,
	        AccountStatus status) {

	    List<CreditAccount> accounts;

	    if (role == UserRole.CUSTOMER) {

	        Customer customer = customerService.getCustomerByUserId(userId);

	        if (status != null) {
                accounts = accountRepository
                        .findAllByCustomerCustomerIdAndAccountStatus(
                                customer.getCustomerId(), status);
            } else {
                accounts = accountRepository
                        .findAllByCustomerCustomerId(customer.getCustomerId());
            }
	    } else { // ADMIN

	        if (status != null) {
	            accounts = accountRepository.findAllByAccountStatus(status);
	        } else {
	            accounts = accountRepository.findAll();
	        }
	    }

	    return accounts.stream()
	            .map(accountMapper::toResponse)
	            .collect(Collectors.toList());
	}

	/**
	 * Fetch Account By Id
     * CUSTOMER → only own account
     * ADMIN → any account
	 */
	@Override
	public CreditAccountResponse getAccountById(
	        UUID userId,
	        UserRole role,
	        UUID accountId) {

	    CreditAccount creditAccount = findAccountById(accountId);

	    // Authorization check only for CUSTOMER
	    if (role == UserRole.CUSTOMER) {
	        Customer customer = customerService.getCustomerByUserId(userId);

	        if (!creditAccount.getCustomer().getCustomerId()
	                .equals(customer.getCustomerId())) {
	            throw new AccessDeniedException("Access denied to this account");
	        }
	    }

	    // Common response (for both ADMIN + CUSTOMER)
	    CreditAccountResponse response = accountMapper.toResponse(creditAccount);

	    return response;
	}

	/**
	 * Update Account Status(Admin Only)
	 * Status: ACTIVE,BLOCKED,SUSPENDED,CLOSED
	 */
	@Override
	public CreditAccountResponse updateAccountStatus(UUID accountId,
			CreditAccountStatusUpdateRequest request) {
		CreditAccount account = findAccountById(accountId);
        AccountStatus newAccountStatus =request.getStatus();
        
        if (newAccountStatus == null) {
            throw new BusinessRuleException("Account status must not be null");
        }
        //validate transition rules 
        validateStatusTransition(account.getAccountStatus(), newAccountStatus);
        
        account.setAccountStatus(newAccountStatus);
        
        //if closing the closing time stamp record
        if(newAccountStatus==AccountStatus.CLOSED) {
        	account.setClosedAt(Instant.now());
        }
        
        //if reactivating from suspended ,clear closed at time stamp
        if(newAccountStatus==AccountStatus.ACTIVE) {
        	account.setClosedAt(null);
        }
        
        CreditAccountResponse response = accountMapper.toResponse(accountRepository.save(account));
		return response;
	}
	
    /**
     * {@inheritDoc}
     *
     * <p>Used by {@link CreditCardServiceImpl} and {@link TransactionServiceImpl}
     * to resolve the account entity without direct repository access.
     */
	
	@Override
    @Transactional(readOnly = true)
    public CreditAccount getAccountEntity(UUID accountId) {
        return findAccountById(accountId);
    }
	
	/**
     * {@inheritDoc}
     *
     * <p>Used by {@link CreditAccountApplicationServiceImpl} to guard against
     * duplicate account creation without cross-domain repository access.
     */
    @Override
    @Transactional(readOnly = true)
    public boolean accountExistsForApplication(UUID applicationId) {
        return accountRepository.existsByApplicationApplicationId(applicationId);
    }
 
    /**
     * {@inheritDoc}
     *
     * <p>Used by {@link IssuedCardActiveCardChecker} to enforce the duplicate-account
     * gate without injecting the account repository directly.
     */
    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveAccountForProduct(UUID customerId, Long creditProductId) {
        return accountRepository
                .existsByCustomerCustomerIdAndCreditProductCreditProductIdAndAccountStatus(
                        customerId, creditProductId, AccountStatus.ACTIVE);
    }
 
    /**
     * {@inheritDoc}
     *
     * <p>Called by {@link TransactionServiceImpl} so the transaction domain
     * never writes directly to the accounts repository.
     */
    @Override
    public void deductBalance(UUID accountId, BigDecimal amount) {
    	//Check the amount > 0
    	if (amount==null || amount.compareTo(BigDecimal.ZERO)<=0) {
    		throw new BusinessRuleException("Amount Must be greater than Zero");
		}
        CreditAccount creditAccount = findAccountById(accountId);
        validateAccountActive(creditAccount);
        
        // Balance validation (defensive)
        if (creditAccount.getAvailableBalance().compareTo(amount) < 0) {
            throw new BusinessRuleException("Insufficient available balance");
        }
        //Apply Changes
        creditAccount.setAvailableBalance(creditAccount.getAvailableBalance().subtract(amount));
        creditAccount.setCurrentBalance(creditAccount.getCurrentBalance().add(amount));
        accountRepository.save(creditAccount);
    }
	/**
	  * {@inheritDoc}
     *
     * <p>Called by {@link TransactionServiceImpl} so the transaction domain
     * never writes directly to the accounts repository.
	 */
    @Override
	 public void addBalance(UUID accountId, BigDecimal amount) {
		if (amount==null || amount.compareTo(BigDecimal.ZERO)<=0) {
			throw new BusinessRuleException("Amount Must be greater than Zero");
		}
		CreditAccount creditAccount = findAccountById(accountId);
		
		validateAccountActive(creditAccount);
		//Idempotency check
		if (creditAccount.getLastPaymentAmount() != null &&
				creditAccount.getLastPaymentDate() != null &&
						creditAccount.getLastPaymentDate().isAfter(Instant.now().minusSeconds(10))) {
		        throw new ConflictException("Duplicate payment detected");
		    }


		// cannot exceed credit limit
		BigDecimal newAvailable = creditAccount.getAvailableBalance().add(amount);
		
		if (newAvailable.compareTo(creditAccount.getCreditLimit())>0) {
			newAvailable=creditAccount.getCreditLimit();	
		}
		
		BigDecimal newCurrentBalance = creditAccount.getCurrentBalance().subtract(amount);
		
		//Prevent negative outstanding (overpayment case)
	    if (newCurrentBalance.compareTo(BigDecimal.ZERO) < 0) {
	        newCurrentBalance = BigDecimal.ZERO;
	    }
	    
	    creditAccount.setAvailableBalance(newAvailable);
	    creditAccount.setCurrentBalance(newCurrentBalance);
	    creditAccount.setLastPaymentAmount(amount);
	    creditAccount.setLastPaymentDate(Instant.now());

	    accountRepository.save(creditAccount);

	 }
    @Override
    @Transactional
    public void applyPayment(UUID accountId, BigDecimal amount,Instant paidAt) {

        CreditAccount account = getAccountEntity(accountId);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Invalid payment amount");
        }

        BigDecimal currentBalance = account.getCurrentBalance() != null
                ? account.getCurrentBalance()
                : BigDecimal.ZERO;

        // Reduce outstanding balance
        BigDecimal newBalance = currentBalance.subtract(amount);

        account.setCurrentBalance(newBalance);

        //  Handle credit balance (over payment)
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            log.info("Account {} now has credit balance: {}", accountId, newBalance);
        }

        // Recalculate available credit
        BigDecimal creditLimit = account.getCreditLimit();
        BigDecimal availableCredit = creditLimit.subtract(newBalance);

        account.setAvailableBalance(availableCredit);
        account.setLastPaymentAmount(amount);
        account.setLastPaymentDate(paidAt);
        
        accountRepository.save(account);

        log.info("Account payment applied | accountId={} | amount={} | newBalance={} | availableCredit={}",
                accountId, amount, newBalance,availableCredit);
    }
    
    //Used in Payment Service 
    @Override
    @Transactional(readOnly = true)
    public CreditAccount getAccount(UUID accountId) {

        return accountRepository.findById(accountId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Credit account not found"));
    }
    
	//-----------------Helper method------------------------------ 
	/**
	 * Find Credit Account by id 
	 * @param accountId
	 * @return
	 */
	private CreditAccount findAccountById(UUID accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Account with id " + accountId + " not found"));
    }
	
	/**
	 * Build Credit account 
	 * @param application
	 * @param accountNumber
	 * @return
	 */
    private CreditAccount buildCreditAccount(CreditCardApplication application, String accountNumber) {
    	//Build Account entity
    	CreditProduct product = application.getCreditProduct();
        CreditAccount account = new CreditAccount();
        account.setAccountNumber(accountNumber);
        account.setCustomer(application.getCustomer());
        account.setApplication(application);
        account.setCreditProduct(application.getCreditProduct());
        account.setAccountStatus(AccountStatus.ACTIVE);
        //Credit terms -from application
        account.setCreditLimit(application.getApprovedCreditLimit());
        account.setApr(application.getApprovedApr());
        account.setGracePeriodDays(product.getGracePeriodDays());
        account.setMinimumDuePercent(product.getMinimumDuePercent());
        account.setLateFeeAmount(product.getLateFeeAmount());
        //Financial state
        account.setCurrentBalance(BigDecimal.ZERO);
        account.setAvailableBalance(application.getApprovedCreditLimit());
        //
        //account.setMinimumDueAmount(BigDecimal.ZERO);
        account.setStatementCycleDay(DEFAULT_STATEMENT_CYCLE_DAY);
        account.setLastStatementDate(null);
        account.setLastStatementBalance(null);
        account.setNextDueDate(null);
        account.setLastPaymentDate(null);
        account.setLastPaymentAmount(null);
        account.setActivatedAt(Instant.now());
        return account;
    }
    

	/**
	 * Validate account is active or not and Prevent operation on closed account
	 * @param account
	 */
	private void validateAccountActive(CreditAccount account) {
	    if (account.getAccountStatus() == AccountStatus.CLOSED) {
	        throw new BusinessRuleException("Operation not allowed on CLOSED account");
	    }
	    if (account.getAccountStatus() != AccountStatus.ACTIVE) {
	        throw new BusinessRuleException("Account is not active");
	    }
	}
	
	/**
	 * * Valid status transitions:
     *
     * ACTIVE    → SUSPENDED, BLOCKED, CLOSED
     * SUSPENDED → ACTIVE, BLOCKED, CLOSED
     * BLOCKED   → ACTIVE, CLOSED          (admin can unblock or close)
     * CLOSED    → (no transitions allowed — terminal state)
	 * @param current
	 * @param next
	 */
	private void validateStatusTransition(AccountStatus current, AccountStatus next) {

        if (current == next) {
            throw new BusinessRuleException(
                    "Account is already in " + current + " status");
        }

        if (current == AccountStatus.CLOSED) {
            throw new BusinessRuleException(
                    "Cannot change status of a CLOSED account. "
                            + "It is a terminal state.");
        }
    }

	@Override
	@Transactional
	public void updateAccountAfterBilling(
	        UUID accountId,
	        Instant lastStatementDate,
	        BigDecimal lastStatementBalance,
	        Instant nextDueDate,
	        BigDecimal minimumDueAmount) {

	    CreditAccount account = getAccountEntity(accountId);

	    account.setLastStatementDate(lastStatementDate);
	    account.setLastStatementBalance(lastStatementBalance);
	    account.setNextDueDate(nextDueDate);
	    accountRepository.save(account);
	}

	@Override
	@Transactional(readOnly = true)
	public CreditAccount getAccountForUpdate(UUID accountId) {
	    return accountRepository.findByIdForUpdate(accountId)
	            .orElseThrow(() ->
	                    new ResourceNotFoundException("Credit Account not found"));
	}
}
