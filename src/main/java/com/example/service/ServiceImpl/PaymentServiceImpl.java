package com.example.service.ServiceImpl;

import com.example.dto.request.PaymentRequest;
import com.example.dto.response.PaymentResponse;
import com.example.entity.BillingStatement;
import com.example.entity.CreditAccount;
import com.example.entity.Payment;
import com.example.entity.PaymentAllocation;
import com.example.enums.*;
import com.example.exception.BadRequestException;
import com.example.exception.ResourceNotFoundException;
import com.example.mapper.PaymentMapper;
import com.example.repository.PaymentAllocationRepository;
import com.example.repository.PaymentRepository;
import com.example.service.BillingStatementService;
import com.example.service.CreditAccountService;
import com.example.service.PaymentService;
import com.example.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Core service responsible for processing credit card payments.
 *
 * <p>This service acts as the financial transaction engine for incoming payments,
 * ensuring correct allocation across billing statements, updating account balances,
 * and maintaining a consistent financial ledger.</p>
 *
 * <p><b>Key Responsibilities:</b></p>
 * <ul>
 *     <li>Validate and process incoming payment requests</li>
 *     <li>Allocate payments to outstanding billing statements (oldest first)</li>
 *     <li>Handle partial payments and overpayments</li>
 *     <li>Update account balances and available credit</li>
 *     <li>Record transactions for audit and traceability</li>
 * </ul>
 *
 * <p><b>Financial Guarantees:</b></p>
 * <ul>
 *     <li>ACID-compliant transactions using {@code @Transactional}</li>
 *     <li>Deterministic payment allocation order (oldest statements first)</li>
 *     <li>Accurate monetary calculations using {@link java.math.BigDecimal}</li>
 * </ul>
 *
 * <p><b>Important:</b> This service is part of the financial ledger system.
 * Any modifications must ensure consistency, idempotency, and audit correctness.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PaymentServiceImpl implements PaymentService {

	
    private final BillingStatementService billingService;
    private final CreditAccountService creditAccountService;
    private final PaymentRepository paymentRepository;
    private final PaymentAllocationRepository allocationRepository;
    private final TransactionService transactionService;
    private final PaymentMapper paymentMapper;

    /**
     * Processes a payment against a credit account and updates all related financial records.
     *
     * <p><b>Execution Flow:</b></p>
     * <ol>
     *     <li>Validate request payload</li>
     *     <li>Acquire account lock (for update)</li>
     *     <li>Create and persist payment record</li>
     *     <li>Allocate payment across unpaid statements (oldest first)</li>
     *     <li>Update statement balances and statuses</li>
     *     <li>Apply payment to account balance</li>
     *     <li>Record transaction for audit trail</li>
     * </ol>
     *
     * <p><b>Allocation Strategy:</b></p>
     * <ul>
     *     <li>Payments are applied to the oldest unpaid statements first</li>
     *     <li>Supports partial payments and multiple statement settlement</li>
     *     <li>Stops allocation once payment amount is exhausted</li>
     * </ul>
     *
     * <p><b>Overpayment Handling:</b></p>
     * <ul>
     *     <li>Any remaining amount after clearing all statements is treated as overpayment</li>
     *     <li>Overpayment is applied to increase available credit on the account</li>
     * </ul>
     *
     * <p><b>Consistency Guarantees:</b></p>
     * <ul>
     *     <li>Fully transactional — all updates succeed or fail together</li>
     *     <li>Prevents partial updates to statements or account balances</li>
     * </ul>
     *
     * @param accountId the credit account receiving the payment
     * @param request   the validated payment request payload
     * @return payment response containing transaction details
     *
     * @throws BadRequestException if the request is invalid or violates business rules
     */
    @Override
    public PaymentResponse makePayment(UUID accountId, PaymentRequest request) {

        validateRequest(request);

        
        CreditAccount account =
                creditAccountService.getAccountForUpdate(accountId);

        BigDecimal paymentAmount = request.getAmount();

        // 4. Create Payment
        Payment payment = Payment.builder()
                .account(account)
                .amount(paymentAmount)
                .status(PaymentStatus.SUCCESS)
                .paymentMethod(request.getPaymentMethod())
                .referenceId(request.getReferenceId()) // use request reference
                .paidAt(Instant.now())
                .build();

        
        paymentRepository.save(payment);
        

        // 5. Balance BEFORE
        BigDecimal remainingPayment = paymentAmount;
        
        List<BillingStatement> unpaidStatements =
                billingService.getUnpaidStatementsOldestFirst(accountId);

        for (BillingStatement statement : unpaidStatements) {

            if (remainingPayment.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal stmtRemaining = defaultZero(statement.getRemainingAmount());

            if (stmtRemaining.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal allocation = remainingPayment.min(stmtRemaining);

            statement.setAmountPaid(
                    defaultZero(statement.getAmountPaid()).add(allocation)
            );

            statement.setRemainingAmount(
                    stmtRemaining.subtract(allocation)
            );

            // immediate full-paid -Status Change
            if (statement.getRemainingAmount().compareTo(BigDecimal.ZERO) == 0 &&
            	    statement.getStatementStatus() != StatementStatus.OVERDUE) {

            	    statement.setStatementStatus(StatementStatus.PAID);
            	}

            billingService.save(statement);

            allocationRepository.save(
                    PaymentAllocation.builder()
                            .payment(payment)
                            .statement(statement)
                            .allocatedAmount(allocation)
                            .build()
            );

            remainingPayment = remainingPayment.subtract(allocation);
        }

        BigDecimal before = defaultZero(account.getAvailableBalance());
        

        // 6. Update account (FULL payment including overpayment)
        creditAccountService.applyPayment(
                accountId,
                paymentAmount,
                payment.getPaidAt()
        );

        BigDecimal after = creditAccountService
                .getAccount(accountId)
                .getAvailableBalance();

        // 7. Record transaction 
        transactionService.recordPayment(
                account,
                payment,
                before,
                after
        );

        log.info("""
                Payment processed
                accountId={}
                paymentId={}
                totalAmount={}
                unallocatedOverpayment={}
                """,
                accountId,
                payment.getPaymentId(),
                paymentAmount,
                remainingPayment
        );

        // 9. Response
        return paymentMapper.toResponse(payment);
    }


    /**
     * Retrieves a paginated list of payments for a specific account.
     *
     * <p>This method ensures efficient data retrieval by enforcing pagination,
     * preventing excessive memory usage when handling large payment histories.</p>
     *
     * <p><b>Details:</b></p>
     * <ul>
     *     <li>Fetches payments using page-based queries</li>
     *     <li>Loads associated allocation details for each payment</li>
     *     <li>Maps entities to response DTOs</li>
     * </ul>
     *
     * @param accountId the account identifier
     * @param page      zero-based page index
     * @param size      number of records per page
     * @return paginated list of payment responses
     */
    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponse> getPayments(
            UUID accountId,
            int page,
            int size
    ) {

        Page<Payment> paymentPage =
                paymentRepository.findByAccount_AccountId(
                        accountId,
                        PageRequest.of(page, size)
                );

        List<UUID> paymentIds = paymentPage.getContent()
                .stream()
                .map(Payment::getPaymentId)
                .toList();

        if (paymentIds.isEmpty()) {
            return Page.empty(PageRequest.of(page, size));
        }

        List<Payment> detailedPayments =
                paymentRepository.findAllWithAllocationsByIds(paymentIds);

        Map<UUID, Payment> detailedMap =
                detailedPayments.stream()
                        .collect(Collectors.toMap(
                                Payment::getPaymentId,
                                p -> p
                        ));

        return paymentPage.map(
                payment -> paymentMapper.toResponse(
                        detailedMap.get(payment.getPaymentId())
                )
        );
    }

    /**
     * Retrieves a specific payment for an account with strict ownership validation.
     *
     * <p>This method ensures that the requested payment belongs to the given account,
     * preventing unauthorized access to financial data.</p>
     *
     * <p><b>Security:</b></p>
     * <ul>
     *     <li>Validates both paymentId and accountId</li>
     *     <li>Prevents cross-account data access</li>
     * </ul>
     *
     * @param accountId the account identifier
     * @param paymentId the payment identifier
     * @return payment details
     *
     * @throws ResourceNotFoundException if payment is not found or does not belong to account
     */
    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(
            UUID accountId,
            UUID paymentId
    ) {

        Payment payment =
                paymentRepository.findDetailedByPaymentIdAndAccountId(
                        paymentId,
                        accountId
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Payment not found"
                        )
                );

        return paymentMapper.toResponse(payment);
    }

    /**
     * Validates the structure and basic integrity of a payment request.
     *
     * <p>This method performs fail-fast validation before any database interaction,
     * ensuring invalid requests are rejected early to conserve system resources.</p>
     *
     * <p><b>Validations:</b></p>
     * <ul>
     *     <li>Amount must be non-null and greater than zero</li>
     *     <li>Payment method must be provided</li>
     *     <li>Reference ID must be provided</li>
     * </ul>
     *
     * @param request payment request payload
     * @throws BadRequestException if validation fails
     */
    private void validateRequest(PaymentRequest request) {

        if (request.getAmount() == null ||
                request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Invalid payment amount");
        }

        if (request.getPaymentMethod() == null) {
            throw new BadRequestException("Payment method is required");
        }

        if (request.getReferenceId() == null) {
            throw new BadRequestException("ReferenceId is required");
        }
    }
    /**
     * Returns {@link BigDecimal#ZERO} if the given value is null.
     *
     * <p>This method protects financial calculations from {@link NullPointerException}
     * caused by database aggregate functions (e.g., SUM) returning null when no rows match.</p>
     *
     * @param val the input value (possibly null)
     * @return non-null BigDecimal value
     */
    private BigDecimal defaultZero(BigDecimal val) {
        return val == null ? BigDecimal.ZERO : val;
    }


    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getByReferenceId(String referenceId) {

        Payment payment = paymentRepository
                .findByReferenceId(referenceId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Payment not found for referenceId: " + referenceId
                        )
                );

        return paymentMapper.toResponse(payment);
    }
}