package com.example.mapper;

import org.springframework.stereotype.Component;

import com.example.dto.request.CreditProductCreateRequest;
import com.example.dto.request.CreditProductUpdateRequest;
import com.example.dto.response.CreditProductCreateResponse;
import com.example.dto.response.CreditProductResponse;
import com.example.entity.CreditProduct;
import com.example.enums.ProductStatus;

@Component
public class CreditProductMapper {
	
	public CreditProduct toEntity(CreditProductCreateRequest request) {
		CreditProduct product = new CreditProduct();
		
		product.setProductName(request.getProductName());
		product.setMinCreditLimit(request.getMinCreditLimit());
        product.setMaxCreditLimit(request.getMaxCreditLimit());
        product.setMinIncomeRequired(request.getMinIncomeRequired());
        product.setMinCreditScore(request.getMinCreditScore());
        product.setAprPurchase(request.getAprPurchase());
        product.setAprCashAdvance(request.getAprCashAdvance());
        product.setGracePeriodDays(request.getGracePeriodDays());
        product.setInterestCalculationMethod(request.getInterestCalculationMethod());
        product.setMinimumDuePercent(request.getMinimumDuePercent());
        product.setMinimumDueAmount(request.getMinimumDueAmount());
        product.setLateFeeAmount(request.getLateFeeAmount());
        product.setOverlimitFee(request.getOverlimitFee());
        product.setJoiningFee(request.getJoiningFee());
        product.setForeignTransactionFeePercent(request.getForeignTransactionFeePercent());
        product.setBalanceTransferFeePercent(request.getBalanceTransferFeePercent());
        product.setCashAdvanceFeePercent(request.getCashAdvanceFeePercent());
        product.setCashAdvanceFeeMin(request.getCashAdvanceFeeMin());
        product.setEffectiveFrom(request.getEffectiveFrom());
        product.setEffectiveTo(request.getEffectiveTo());
        
        product.setStatus(ProductStatus.ACTIVE);
        return product;
	}
	public CreditProductResponse toResponse(CreditProduct product) {

        return new CreditProductResponse(
                product.getCreditProductId(),
                product.getProductCode(),
                product.getProductName(),
                product.getMinCreditLimit(),
                product.getMaxCreditLimit(),
                product.getMinIncomeRequired(),
                product.getMinCreditScore(),
                product.getAprPurchase(),
                product.getAprCashAdvance(),
                product.getGracePeriodDays(),
                product.getInterestCalculationMethod(),
                product.getMinimumDuePercent(),
                product.getMinimumDueAmount(),
                product.getLateFeeAmount(),
                product.getOverlimitFee(),
                product.getJoiningFee(),
                product.getForeignTransactionFeePercent(),
                product.getBalanceTransferFeePercent(),
                product.getCashAdvanceFeePercent(),
                product.getCashAdvanceFeeMin(),
                product.getEffectiveFrom(),
                product.getEffectiveTo(),
                product.getStatus()
        );
    }
	
	public CreditProductCreateResponse toCreateResponse(CreditProduct product) {

        return new CreditProductCreateResponse(
                product.getCreditProductId(),
                product.getProductCode(),
                product.getProductName(),
                product.getStatus()
        );
    }
	public void updateEntity(CreditProductUpdateRequest request, CreditProduct product) {

	    if (request.getProductName() != null)
	        product.setProductName(request.getProductName());

	    if (request.getMinCreditLimit() != null)
	        product.setMinCreditLimit(request.getMinCreditLimit());

	    if (request.getMaxCreditLimit() != null)
	        product.setMaxCreditLimit(request.getMaxCreditLimit());

	    if (request.getMinIncomeRequired() != null)
			product.setMinIncomeRequired(request.getMinIncomeRequired());
	    
		if (request.getMinCreditScore() != null)
			product.setMinCreditScore(request.getMinCreditScore());
		
		if (request.getAprPurchase() != null)
			product.setAprPurchase(request.getAprPurchase());
		
		if (request.getAprCashAdvance() != null)
			product.setAprCashAdvance(request.getAprCashAdvance());
		
		if (request.getGracePeriodDays() != null)
			product.setGracePeriodDays(request.getGracePeriodDays());
		
		if (request.getInterestCalculationMethod() != null)
			product.setInterestCalculationMethod(request.getInterestCalculationMethod());
		
		if (request.getMinimumDuePercent() != null)
			product.setMinimumDuePercent(request.getMinimumDuePercent());
		
		if (request.getMinimumDueAmount() != null)
			product.setMinimumDueAmount(request.getMinimumDueAmount());
		
		if (request.getLateFeeAmount() != null)
			product.setLateFeeAmount(request.getLateFeeAmount());
		
		if (request.getOverlimitFee() != null)
			product.setOverlimitFee(request.getOverlimitFee());
		
		if (request.getJoiningFee() != null)
			product.setJoiningFee(request.getJoiningFee());
		
		if (request.getForeignTransactionFeePercent() != null)
			product.setForeignTransactionFeePercent(request.getForeignTransactionFeePercent());
		
		if (request.getBalanceTransferFeePercent() != null)
			product.setBalanceTransferFeePercent(request.getBalanceTransferFeePercent());
		
		if (request.getCashAdvanceFeePercent() != null)
			product.setCashAdvanceFeePercent(request.getCashAdvanceFeePercent());
		
		if (request.getCashAdvanceFeeMin() != null)
			product.setCashAdvanceFeeMin(request.getCashAdvanceFeeMin());
		
		if (request.getEffectiveFrom() != null)
			product.setEffectiveFrom(request.getEffectiveFrom());
		
		if (request.getEffectiveTo() != null)
			product.setEffectiveTo(request.getEffectiveTo());
		
		if (request.getStatus() != null)
			product.setStatus(request.getStatus());
	}
	
}

