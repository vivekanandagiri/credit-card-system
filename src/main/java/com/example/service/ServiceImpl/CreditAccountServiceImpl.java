package com.example.service.ServiceImpl;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.example.dto.request.CreditAccountStatusUpdateRequest;
import com.example.dto.response.ApiResponse;
import com.example.dto.response.CreditAccountResponse;
import com.example.entity.CreditAccount;
import com.example.entity.CreditCardApplication;
import com.example.entity.Customer;
import com.example.entity.User;
import com.example.enums.AccountStatus;
import com.example.enums.ApplicationStatus;
import com.example.exception.BusinessRuleException;
import com.example.exception.ConflictException;
import com.example.exception.ProfileNotCreatedException;
import com.example.exception.ResourceNotFoundException;
import com.example.exception.UserNotFoundException;
import com.example.mapper.CreditAccountMapper;
import com.example.repository.CreditAccountRepository;
import com.example.repository.UserRepository;
import com.example.service.CreditAccountService;
import com.example.util.AccountNumberGenerator;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class CreditAccountServiceImpl implements CreditAccountService {
	
	
	//constructor injection
	
	private static final int DEFAULT_STATEMENT_CYCLE_DAY = 5;
	
	private final CreditAccountRepository accountRepository;
	private final AccountNumberGenerator accountNumberGenerator;
	private final CreditAccountMapper accountMapper;
	private final UserRepository userRepository;
	
	

	public CreditAccountServiceImpl(CreditAccountRepository accountRepository,
			AccountNumberGenerator accountNumberGenerator, CreditAccountMapper accountMapper,
			UserRepository userRepository) {
		this.accountRepository = accountRepository;
		this.accountNumberGenerator = accountNumberGenerator;
		this.accountMapper = accountMapper;
		this.userRepository = userRepository;
	}

	//Auto Credit Account Creation after application approval
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
		
		//Build Account entity
		CreditAccount account = new CreditAccount();
		account.setAccountNumber(accountNumber);
		account.setCustomer(application.getCustomer());
		account.setApplication(application);
		account.setCreditProduct(application.getCreditProduct());
		account.setAccountStatus(AccountStatus.ACTIVE);
		
		//Credit terms -from application
		account.setCreditLimit(application.getApprovedCreditLimit());
		account.setApr(application.getApprovedApr());
		
		account.setCurrentBalance(BigDecimal.ZERO);
		account.setAvailableBalance(application.getApprovedCreditLimit());
		account.setMinimumDueAmount(BigDecimal.ZERO);
		
		//Default date  5 No card product exists yet at this point.
		account.setStatementCycleDay(DEFAULT_STATEMENT_CYCLE_DAY);
		
		account.setLastStatementDate(null);
		account.setLastStatementBalance(null);
		account.setNextDueDate(null);
		account.setLastPaymentDate(null);
		account.setLastPaymentAmount(null);
		account.setActivatedAt(Instant.now());
		
		CreditAccount savedAccount = accountRepository.save(account);
		
		return accountMapper.toResponse(savedAccount);
	}
	//get all my accounts(Customer)
	@Override
	public ApiResponse<List<CreditAccountResponse>> getMyAccounts(UUID userId) {
		Customer customer = getCustomerFromUser(userId);
		
		List<CreditAccountResponse> accounts = 
				accountRepository.findAllByCustomerCustomerId(customer.getCustomerId())
				.stream()
				.map(accountMapper::toResponse)
				.collect(Collectors.toList());

		return new ApiResponse<>
				(Instant.now(),
				HttpStatus.OK.value(),
				"Accounts fetched Successfully",accounts);
	}

	// GET MY ACCOUNT BY ID (Customer)
	@Override
	public ApiResponse<CreditAccountResponse> getMyAccountById(UUID userId, UUID accountId) {
		
		Customer customer = getCustomerFromUser(userId);
		CreditAccount account = findAccountById(accountId);
		
		if(!account.getCustomer().getCustomerId().equals(customer.getCustomerId()))
		{
			throw new AccessDeniedException("Access Denied to this Account ");
		}
		return new ApiResponse<>
			(Instant.now(),
			HttpStatus.OK.value(),
			"Account Fetched Successfully",
			accountMapper.toResponse(account));
	}

	//Get All accounts (Admin)
	@Override
	public ApiResponse<List<CreditAccountResponse>> getAllAccounts() {
		
		List<CreditAccountResponse> accounts = accountRepository.findAll()
                .stream()
                .map(accountMapper::toResponse)
                .collect(Collectors.toList());
		
		return new ApiResponse<>
		(Instant.now(),
		HttpStatus.OK.value(),
		"All Acccount Fetched Successfully",
		accounts);
	}

	// GET ACCOUNTS BY STATUS (Admin)
	@Override
	public ApiResponse<List<CreditAccountResponse>> getAccountsByStatus(String status) {
		AccountStatus accountStatus = parseAccountStatus(status);
		
		List<CreditAccountResponse> accounts =
                accountRepository.findAllByAccountStatus(accountStatus)
                        .stream()
                        .map(accountMapper::toResponse)
                        .collect(Collectors.toList());

        return new ApiResponse<>(Instant.now(), HttpStatus.OK.value(),
                "Accounts fetched for status: " + status, accounts);
	}

	 // GET ACCOUNT BY ID (Admin)
	@Override
	public ApiResponse<CreditAccountResponse> getAccountById(UUID accountId) {
		CreditAccountResponse response = accountMapper.toResponse(findAccountById(accountId));
		
		return new ApiResponse<>
			(Instant.now(),
			HttpStatus.OK.value(),
	        "Account fetched successfully",
	        response);
	}

	// UPDATE ACCOUNT STATUS (Admin)
	@Override
	public ApiResponse<CreditAccountResponse> updateAccountStatus(UUID accountId,
			CreditAccountStatusUpdateRequest request) {
		CreditAccount account = findAccountById(accountId);
        AccountStatus newAccountStatus = parseAccountStatus(request.getStatus());
        
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

        

		return new ApiResponse<>
				(Instant.now(),
				HttpStatus.OK.value(),
				"Account Status updated to"+newAccountStatus,
				response);
	}
	
	//Helper method 
	
	private CreditAccount findAccountById(UUID accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Account with id " + accountId + " not found"));
    }
	
	private Customer getCustomerFromUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new UserNotFoundException("User with id " + userId + " not found"));

        Customer customer = user.getCustomer();
        if (customer == null) {
            throw new ProfileNotCreatedException(
                    "Customer profile not found for user " + userId);
        }
        return customer;
    }
	
	private AccountStatus parseAccountStatus(String status) {
        try {
            return AccountStatus.valueOf(status.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException(
                    "Invalid account status: '" + status
                            + "'. Valid values: "
                            + Arrays.toString(AccountStatus.values()));
        }
    }
	
	
	/**
     * Valid status transitions:
     *
     * ACTIVE    → SUSPENDED, BLOCKED, CLOSED
     * SUSPENDED → ACTIVE, BLOCKED, CLOSED
     * BLOCKED   → ACTIVE, CLOSED          (admin can unblock or close)
     * CLOSED    → (no transitions allowed — terminal state)
     *//**
     * Valid status transitions:
    *
    * ACTIVE    → SUSPENDED, BLOCKED, CLOSED
    * SUSPENDED → ACTIVE, BLOCKED, CLOSED
    * BLOCKED   → ACTIVE, CLOSED          (admin can unblock or close)
    * CLOSED    → (no transitions allowed — terminal state)
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

}
