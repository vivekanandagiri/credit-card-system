package com.example.mapper;

import org.springframework.stereotype.Component;

import com.example.dto.request.CreditProductCreateRequest;
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
	
}

