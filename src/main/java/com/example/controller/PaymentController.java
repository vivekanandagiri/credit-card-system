package com.example.controller;

import com.example.api.PaymentApi;
import com.example.dto.request.PaymentRequest;
import com.example.dto.response.ApiResponse;
import com.example.dto.response.PaymentResponse;
import com.example.idempotency.IdempotencyRecord;
import com.example.idempotency.PaymentIdempotencyService;
import com.example.service.PaymentService;

import lombok.RequiredArgsConstructor;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PaymentController implements PaymentApi {

    private final PaymentService paymentService;
    private final PaymentIdempotencyService paymentIdempotencyService;

    @Override
    public ResponseEntity<ApiResponse<PaymentResponse>> makePayment(
            UUID accountId,
            PaymentRequest request) {

    	IdempotencyRecord<PaymentResponse> record =
    	        paymentIdempotencyService.process(accountId, request);

    	if (record.isDuplicate()) {
    	    return ResponseEntity.status(HttpStatus.CONFLICT)
    	            .body(ApiResponse.success(
    	                    HttpStatus.CONFLICT,
    	                    "Duplicate payment request",
    	                    record.getResponseBody()
    	            ));
    	}

    	return ResponseEntity.status(HttpStatus.CREATED)
    	        .body(ApiResponse.success(
    	                HttpStatus.CREATED,
    	                "Payment successful",
    	                record.getResponseBody()
    	        ));
    }

	@Override
	public ResponseEntity<Page<PaymentResponse>> getPayments(UUID accountId, int page, int size) {
		Page<PaymentResponse> response = paymentService.getPayments(accountId, page, size);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}

	@Override
	public ResponseEntity<PaymentResponse> getPaymentById(UUID accountId, UUID paymentId) {
		PaymentResponse response = paymentService.getPaymentById(accountId, paymentId);
		return ResponseEntity.status(HttpStatus.OK).body(response);
	}


}