package com.example.service.ServiceImpl;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.example.dto.request.CreditProductCreateRequest;
import com.example.dto.request.CreditProductUpdateRequest;
import com.example.dto.response.ApiResponse;
import com.example.dto.response.CreditProductCreateResponse;
import com.example.dto.response.CreditProductResponse;
import com.example.entity.CreditProduct;
import com.example.enums.ProductStatus;
import com.example.exception.BusinessRuleException;
import com.example.exception.ResourceNotFoundException;
import com.example.mapper.CreditProductMapper;
import com.example.repository.CreditProductRepository;
import com.example.service.CreditProductService;
import com.example.util.ProductCodeGenerator;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class CreditProductServiceImpl implements CreditProductService {

	
	private final CreditProductRepository creditProductRepository;
	private final CreditProductMapper mapper;
	private final ProductCodeGenerator codeGenerator;

	public CreditProductServiceImpl(CreditProductRepository creditProductRepository, CreditProductMapper mapper, ProductCodeGenerator codeGenerator) {
		this.creditProductRepository = creditProductRepository;
		this.mapper = mapper;
		this.codeGenerator = codeGenerator;
	}

	// Create Product
	@Override
	public ApiResponse<CreditProductCreateResponse> create(CreditProductCreateRequest request) {
		
		//Credit Limit Validation check
		if(request.getMinCreditLimit().compareTo(request.getMaxCreditLimit())>0) {
			throw new BusinessRuleException("Minimum Credit Limit can not exceed maximum credit limit");
		}
		

	    // Effective from cannot be past
	    if (request.getEffectiveFrom().isBefore(LocalDate.now())) {
	        throw new BusinessRuleException("Effective start date cannot be in the past");
	    }
	    
	    
		//Date Validation check
		 if (request.getEffectiveTo() != null &&
			        request.getEffectiveTo().isBefore(request.getEffectiveFrom())) {

			        throw new BusinessRuleException(
			                "Effective end date cannot be before start date");
			    }
		 
		 
		CreditProduct product = mapper.toEntity(request);
		// credit product code generation
		String baseCode = codeGenerator.generateBaseCode(request.getProductName());

		String finalCode = generateUniqueCode(baseCode);

		product.setProductCode(finalCode);
		
		CreditProductCreateResponse response = mapper.toCreateResponse(creditProductRepository.save(product));
		
		return new ApiResponse<>(Instant.now(), HttpStatus.CREATED.value(), "Credit Product Created Successfully",
				response);
	}

	// Get Specific Product
	@Override
	public ApiResponse<CreditProductResponse> getById(Long id) {
		CreditProduct product = findById(id);
		CreditProductResponse response = mapper.toResponse(product);

		return new ApiResponse<>(Instant.now(), HttpStatus.OK.value(), "Credit Product fetched Successfully", response);

	}

	@Override
	public ApiResponse<List<CreditProductResponse>> getAll() {
		List<CreditProductResponse> list = creditProductRepository.findAll().stream().map(mapper::toResponse)
				.collect(Collectors.toList());
		return new ApiResponse<>(Instant.now(), HttpStatus.OK.value(), "Credit product fetched Successfully", list);
	}

	@Override
	public ApiResponse<List<CreditProductResponse>> getAllActive() {
		List<CreditProductResponse> list = creditProductRepository.findAllByStatus(ProductStatus.ACTIVE).stream()
				.map(mapper::toResponse).collect(Collectors.toList());

		return new ApiResponse<>(Instant.now(), HttpStatus.OK.value(), "Active Credit product futched Successfully",
				list);
	}

	@Override
	public ApiResponse<CreditProductResponse> update(Long id, CreditProductUpdateRequest request) {

		CreditProduct product = findById(id);

		if (product.getStatus() == ProductStatus.INACTIVE) {
			throw new BusinessRuleException("Cannot update an inactive credit product");
		}

		applyUpdates(request, product);
		// product.setUpdatedBy("ADMIN");

		CreditProductResponse response = mapper.toResponse(creditProductRepository.save(product));

		return new ApiResponse<>(Instant.now(), HttpStatus.OK.value(), "Credit product updated successfully", response);
	}


	// Helpers
	private void applyUpdates(CreditProductUpdateRequest request, CreditProduct product) {
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
	}

	private CreditProduct findById(Long id) {
		return creditProductRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Credit product with id " + id + " not found"));
	}
	
	// helper for code generation
		private String generateUniqueCode(String baseCode) {

			int counter = 1;
			String newCode = baseCode + "-001";

			while (creditProductRepository.existsByProductCode(newCode)) {

				counter++;
				newCode = String.format("%s-%03d", baseCode, counter);
			}

			return newCode;
		}

		@Override
		public ApiResponse<String> updateStatus(Long id, ProductStatus status) {
			CreditProduct creditProduct = findById(id);
			
			// Prevent updating to the same status
	        if (creditProduct.getStatus() == status) {
	            throw new BusinessRuleException(
	                    "Credit product is already " + status.name().toLowerCase());
	        }
	        
	        creditProduct.setStatus(status);
	        
	        creditProductRepository.save(creditProduct);
	        
	        
	        String message = status == ProductStatus.ACTIVE
	                ? "Credit product activated successfully"
	                : "Credit product deactivated successfully";

	        return new ApiResponse<>(
	                Instant.now(),
	                HttpStatus.OK.value(),
	                message,
	                status.name()
	        );
		}

}
