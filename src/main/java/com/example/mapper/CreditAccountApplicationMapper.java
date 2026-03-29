package com.example.mapper;

import com.example.dto.response.CreditCardApplicationResponse;
import com.example.dto.response.CreditCardApplicationSummaryResponse;
import com.example.entity.CreditCardApplication;
import com.example.enums.DecisionType;
import org.springframework.stereotype.Component;

@Component
public class CreditAccountApplicationMapper {

    // ENTITY → RESPONSE
    public CreditCardApplicationResponse toResponse(CreditCardApplication app) {

        return new CreditCardApplicationResponse(
                app.getApplicationId(),

                // Customer info
                app.getCustomer().getCustomerId(),
                app.getCustomer().getFirstName() + " " + app.getCustomer().getLastName(),

                // Credit product info
                app.getCreditProduct().getCreditProductId(),
                app.getCreditProduct().getProductName(),
                app.getCreditProduct().getProductCode(), 

                // Application data
                app.getEmploymentType(),
                app.getEmployerName(),
                app.getMonthlyIncome(),
                app.getExistingLiabilities(),
                app.getCreditScoreAtApplication(),
                app.getRequestedCreditLimit(),

                // Decision
                app.getApplicationStatus(),
                app.getDecision() != null ? app.getDecision() : DecisionType.PENDING_REVIEW,
                app.getDecisionReason(),
                app.getApprovedCreditLimit(),
                app.getApprovedApr(),

                // Timestamps
                app.getSubmittedAt(),
                app.getDecisionAt()
        );
    }
    public CreditCardApplicationSummaryResponse toSummaryResponse(CreditCardApplication app) {

        return new CreditCardApplicationSummaryResponse(
                app.getApplicationId(),
                // Customer info
                app.getCustomer().getCustomerId(),
                // Card product info
                app.getCreditProduct().getCreditProductId(),
                // Decision
                app.getApplicationStatus(),
                app.getDecisionReason(),
                // Timestamps
                app.getSubmittedAt()
        );
    }
}