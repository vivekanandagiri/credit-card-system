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
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Core service responsible for processing credit card payments.
 *
 * <p>This service acts as the financial transaction engine for incoming payments,
 * ensuring correct allocation across billing statements, updating account balances,
 * and maintaining a consistent financial ledger.</p>
 *
 * <h3>Key Responsibilities</h3>
 * <ul>
 *     <li>Validate and process incoming payment requests</li>
 *     <li>Allocate payments to unpaid statements (oldest first - FIFO)</li>
 *     <li>Handle partial payments and over payments</li>
 *     <li>Update account balances and credit availability</li>
 *     <li>Record transactions for audit and traceability</li>
 * </ul>
 *
 * <h3>Financial Guarantees</h3>
 * <ul>
 *     <li>ACID-compliant transactions via {@code @Transactional}</li>
 *     <li>Deterministic allocation order (oldest statements first)</li>
 *     <li>Precision-safe calculations using {@link BigDecimal}</li>
 * </ul>
 *
 * <p><b>Important:</b> This is a critical financial component. Any modification
 * must preserve consistency, idempotency, and audit correctness.</p>
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
     * Processes a payment for a credit account.
     *
     * <h3>Execution Flow</h3>
     * <ol>
     *     <li>Validate request</li>
     *     <li>Acquire account lock</li>
     *     <li>Check idempotency via referenceId</li>
     *     <li>Create and persist payment</li>
     *     <li>Record ledger transaction</li>
     *     <li>Allocate payment across statements (FIFO)</li>
     *     <li>Update account balance</li>
     *     <li>Handle overpayment (if any)</li>
     * </ol>
     *
     * <h3>Allocation Strategy</h3>
     * <ul>
     *     <li>Oldest unpaid statements are cleared first</li>
     *     <li>Supports partial and multi-statement payments</li>
     *     <li>Stops when payment amount is exhausted</li>
     * </ul>
     *
     * <h3>Over payment Handling</h3>
     * <ul>
     *     <li>Remaining amount (if any) increases available credit</li>
     * </ul>
     *
     * @param accountId credit account ID
     * @param request   payment request
     * @return processed payment response
     *
     * @throws BadRequestException if validation fails
     */
    @Override
    public PaymentResponse makePayment(UUID accountId, PaymentRequest request) {

        validateRequest(request);

        CreditAccount account =
                creditAccountService.getAccountForUpdate(accountId);

        BigDecimal paymentAmount = request.getAmount();

        // 1.Idempotency
        Optional<Payment> existing =
                paymentRepository.findByReferenceId(request.getReferenceId());

        if (existing.isPresent()) {
            return paymentMapper.toResponse(existing.get());
        }
        // 2. Create Payment
        Payment payment = Payment.builder()
                .account(account)
                .amount(paymentAmount)
                .status(PaymentStatus.SUCCESS)
                .paymentMethod(request.getPaymentMethod())
                .referenceId(request.getReferenceId()) // use request reference
                .paidAt(Instant.now())
                .build();

        
        paymentRepository.save(payment);
        // 3. RECORD TRANSACTION (→ LEDGER FIRST)
        transactionService.recordPayment(account, payment);

        // 5. ALLOCATE TO STATEMENTS (FIFO)
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

            //update paid amount
            statement.setAmountPaid(
                    defaultZero(statement.getAmountPaid()).add(allocation)
            );

            //update remaining amount
            statement.setRemainingAmount(
                    stmtRemaining.subtract(allocation)
            );

            // immediate full-paid-Status Change
            if (statement.getRemainingAmount().compareTo(BigDecimal.ZERO) == 0 &&
            	    statement.getStatementStatus() != StatementStatus.OVERDUE) {

            	    statement.setStatementStatus(StatementStatus.PAID);
            	}

            billingService.save(statement);

            //allocation record
            allocationRepository.save(
                    PaymentAllocation.builder()
                            .payment(payment)
                            .statement(statement)
                            .allocatedAmount(allocation)
                            .build()
            );

            remainingPayment = remainingPayment.subtract(allocation);
        }
        // 6. UPDATE ACCOUNT (AFTER LEDGER + ALLOCATION)
        creditAccountService.applyPayment(
                accountId,
                paymentAmount,
                payment.getPaidAt()
        );

        // 6. HANDLE OVERPAYMENT
        if (remainingPayment.compareTo(BigDecimal.ZERO) > 0) {
            log.info("Overpayment detected | accountId={} | amount={}",
                    accountId, remainingPayment);
        }

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
     * Retrieves paginated payments for an account.
     *
     * @param accountId account ID
     * @param page      page index (0-based)
     * @param size      page size
     * @return paginated payment responses
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
     * Retrieves a payment by ID with strict ownership validation.
     *
     * @param accountId account ID
     * @param paymentId payment ID
     * @return payment response
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
     * Retrieves payment by reference ID (idempotency lookup).
     *
     * @param referenceId unique reference ID
     * @return payment response
     */
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

    /**
     * Validates payment request input.
     *
     * <p>This method performs fail-fast validation before any database interaction,
     * ensuring invalid requests are rejected early to conserve system resources.</p>
     *
     * <p><b>Validations:</b></p>
     * <ul>
     *     <li>The Amount must be non-null and greater than zero</li>
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
}