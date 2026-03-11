package com.example.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

import com.example.enums.EmploymentType;

@Data
public class CreditCardApplicationRequest {

    private UUID cardProductId;

    private EmploymentType employmentType;        
    private String employerName;          //optional

    private BigDecimal monthlyIncome;
    private BigDecimal existingLiabilities;
    private Integer creditScoreAtApplication;
    private BigDecimal requestedCreditLimit;
}