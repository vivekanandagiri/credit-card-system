package com.example.mapper;

import com.example.dto.response.BillingStatementResponse;
import com.example.entity.BillingStatement;
import org.springframework.stereotype.Component;

@Component
public class BillingStatementMapper {

	public BillingStatementResponse toResponse(BillingStatement statement) {

	    BillingStatementResponse response = new BillingStatementResponse();

	    // ── Identifiers ──
	    response.setStatementId(statement.getStatementId());
	    response.setAccountId(statement.getAccount().getAccountId());

	    // ✅ OPTIONAL (if needed)
	    response.setAccountNumber(statement.getAccount().getAccountNumber());
	    // response.setCustomerName(statement.getAccount().getCustomer().getName());

	    // ── Billing period ──
	    response.setBillingPeriodStart(statement.getBillingPeriodStart());
	    response.setBillingPeriodEnd(statement.getBillingPeriodEnd());

	    // ── Balances ──
	    response.setOpeningBalance(statement.getOpeningBalance());
	    response.setTotalDebits(statement.getTotalDebits());
	    response.setTotalCredits(statement.getTotalCredits());
	    response.setInterestCharged(statement.getInterestCharged());
	    response.setClosingBalance(statement.getClosingBalance());

	    // ── Due amounts ──
	    response.setTotalAmountDue(statement.getTotalAmountDue());
	    response.setMinimumDueAmount(statement.getMinimumDueAmount());
	    response.setRemainingAmount(statement.getRemainingAmount());

	    // ✅ ADD THESE (CRITICAL FIX)
	    //response.setMinDuePercent(statement.getMinDuePercent());
	    //response.setMinDueFloor(statement.getMinDueFloor());

	    // ── Optional ──
	    response.setLateFee(statement.getLateFee());

	    // ── Dates ──
	    response.setDueDate(statement.getDueDate());
	    response.setGeneratedAt(statement.getGeneratedAt());

	    // ── Payment ──
	    response.setAmountPaid(statement.getAmountPaid());
	    response.setStatementStatus(statement.getStatementStatus());

	    return response;
	}
}