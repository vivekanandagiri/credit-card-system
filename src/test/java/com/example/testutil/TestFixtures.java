package com.example.testutil;

import com.example.dto.request.AddressCreateRequest;
import com.example.dto.request.ApplicationDecisionRequest;
import com.example.dto.request.CreditCardApplicationRequest;
import com.example.dto.request.CustomerProfileUpdateRequest;
import com.example.dto.request.LoginRequest;
import com.example.dto.request.RegisterRequest;
import com.example.dto.response.CustomerProfileResponse;
import com.example.entity.BillingStatement;
import com.example.entity.CreditAccount;
import com.example.entity.CreditCard;
import com.example.entity.CreditCardApplication;
import com.example.entity.CreditCardProduct;
import com.example.entity.CreditProduct;
import com.example.entity.Customer;
import com.example.entity.User;
import com.example.enums.AccountStatus;
import com.example.enums.ApplicationStatus;
import com.example.enums.CardFormat;
import com.example.enums.CardIssuanceReason;
import com.example.enums.CardStatus;
import com.example.enums.CardType;
import com.example.enums.DecisionType;
import com.example.enums.EmploymentType;
import com.example.enums.Gender;
import com.example.enums.KycStatus;
import com.example.enums.NetworkType;
import com.example.enums.ProductStatus;
import com.example.enums.StatementStatus;
import com.example.enums.UserRole;
import com.example.underwriting.model.UnderwritingDecision;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public class TestFixtures {

    public static RegisterRequest validRegisterRequest() {
        return new RegisterRequest(
                "vivek@gmail.com",
                "9765432101",
                "Password@123",
                "Vivek",
                "Giri",
                LocalDate.of(2000, 8, 15),
                Gender.MALE,
                "ABCDE1234F",
                "RESIDENT",
                "India",
                "Asia/Kolkata"
        );
    }

    public static LoginRequest validLoginRequest() {
        LoginRequest req = new LoginRequest();
        req.setEmail("vivek@gmail.com");
        req.setPassword("Password@123");
        return req;
    }

    public static User validUser() {
        User user = new User();
        user.setUserId(UUID.randomUUID());
        user.setEmail("user_" + UUID.randomUUID() + "@test.com");
        user.setPasswordHash("encoded");
        user.setRole(UserRole.CUSTOMER);
        user.setActive(true);
        user.setLocked(false);
        return user;
    }

    public static Customer validCustomer() {
        Customer customer = new Customer();
        customer.setCustomerId(UUID.randomUUID());
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setDateOfBirth(LocalDate.of(1995, 8, 15));
        customer.setEmail("john_" + UUID.randomUUID() + "@example.com");  // 🔥 FIX
        customer.setPhone("98" + System.nanoTime());
        customer.setPanNumber("PAN" + System.nanoTime());
        customer.setResidencyStatus("RESIDENT");
        customer.setCitizenshipCountry("India");
        customer.setGender(Gender.MALE);
        customer.setTimezone("Asia/Kolkata"); 
        customer.setKycStatus(KycStatus.PENDING);
        return customer;
    }

    public static CustomerProfileResponse validCustomerProfileResponse(UUID userId) {
        return new CustomerProfileResponse(
                userId,
                "Vishal",
                "Das",
                LocalDate.of(1995, 8, 15),
                "vishal@gmail.com",
                "9876543210",
                "ABCDE****F",
                "RESIDENT",
                "India"
        );
    }

    public static CustomerProfileUpdateRequest validCustomerUpdateRequest() {
        CustomerProfileUpdateRequest request = new CustomerProfileUpdateRequest();
        request.setFirstName("Amit");
        request.setLastName("Sharma");
        request.setDateOfBirth(LocalDate.of(1995, 8, 15));
        request.setResidencyStatus("RESIDENT");
        request.setCitizenshipCountry("India");
        return request;
    }

    public static Customer validCustomerWithUser() {
        User user = new User();
        user.setEmail("user_" + UUID.randomUUID() + "@test.com");
        user.setMobileNumber("98" + System.nanoTime());     
        user.setPasswordHash("encoded");        
        user.setRole(UserRole.CUSTOMER);        
        user.setActive(true);
        user.setLocked(false);

        Customer customer = new Customer();

        customer.setUser(user);
        user.setCustomer(customer);

        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setDateOfBirth(LocalDate.of(1995, 8, 15));
        customer.setEmail("john_" + UUID.randomUUID() + "@example.com");  // 🔥 FIX
        customer.setPhone("98" + System.nanoTime());
        customer.setPanNumber("PAN" + System.nanoTime());
        customer.setResidencyStatus("RESIDENT");
        customer.setCitizenshipCountry("India");

        customer.setGender(Gender.MALE);
        customer.setTimezone("Asia/Kolkata");
        customer.setKycStatus(KycStatus.PENDING);

        return customer;
    }
    public static AddressCreateRequest validAddressRequest() {
        return new AddressCreateRequest(
                "HOME",
                "123 MG Road",
                "Near Metro",
                "Bangalore",
                "Karnataka",
                "560001",
                "India",
                true
        );
    }

    public static AddressCreateRequest invalidAddressRequest() {
        return new AddressCreateRequest(
                "", "", "Near Metro",
                "Bangalore", "Karnataka",
                "123", "India", false
        );
    }
    
    public static CreditProduct validCreditProductEntity() {
        CreditProduct product = new CreditProduct();

        product.setProductCode("PLAT_" + System.nanoTime());
        product.setProductName("Platinum Card");
        product.setStatus(ProductStatus.ACTIVE);

        // CREDIT LIMITS
        product.setMinCreditLimit(BigDecimal.valueOf(10000));
        product.setMaxCreditLimit(BigDecimal.valueOf(500000));

        // ELIGIBILITY
        product.setMinCreditScore(650);
        product.setMinIncomeRequired(BigDecimal.valueOf(20000));

        // APR (REQUIRED)
        product.setAprPurchase(BigDecimal.valueOf(12.5));
        product.setAprCashAdvance(BigDecimal.valueOf(18.0));

        // 🔥 FIX CURRENT ERROR
        product.setInterestCalculationMethod("DAILY_REDUCING"); // or enum if used

        // FEES
        product.setJoiningFee(BigDecimal.valueOf(500));
        product.setLateFeeAmount(BigDecimal.valueOf(500));
        product.setOverlimitFee(BigDecimal.valueOf(600));

        product.setCashAdvanceFeePercent(BigDecimal.valueOf(2.5));
        product.setCashAdvanceFeeMin(BigDecimal.valueOf(300));
        product.setForeignTransactionFeePercent(BigDecimal.valueOf(3.5));
        product.setBalanceTransferFeePercent(BigDecimal.valueOf(2.0));

        // BILLING
        product.setGracePeriodDays(45);
        product.setMinimumDuePercent(BigDecimal.valueOf(5));
        product.setMinimumDueAmount(BigDecimal.valueOf(500));

        // DATES (safe defaults)
        product.setEffectiveFrom(LocalDate.now());
        product.setEffectiveTo(LocalDate.now().plusYears(1)); // +1 year

        // METADATA
        product.setCreatedAt(Instant.now());
        product.setCreatedBy("TEST");

        return product;
    }
    public static CreditCardApplicationRequest validApplicationRequest(Long productId) {
        CreditCardApplicationRequest request = new CreditCardApplicationRequest();
        request.setCreditProductId(productId);
        request.setEmploymentType(EmploymentType.SALARIED);
        request.setEmployerName("Infosys");
        request.setMonthlyIncome(BigDecimal.valueOf(50000.00));
        request.setExistingLiabilities(BigDecimal.valueOf(10000));
        request.setCreditScoreAtApplication(750);
        request.setRequestedCreditLimit(BigDecimal.valueOf(100000));
        return request;
    }

    public static CreditCardApplication validApplication(Customer customer, CreditProduct product) {
        CreditCardApplication app = new CreditCardApplication();

        app.setApplicationId(UUID.randomUUID());

        // RELATIONSHIPS
        app.setCustomer(customer);
        app.setCreditProduct(product);

        // REQUIRED EMPLOYMENT
        app.setEmploymentType(EmploymentType.SALARIED);
        app.setEmployerName("Test Company");

        // REQUIRED FINANCIALS
        app.setMonthlyIncome(BigDecimal.valueOf(50000));
        app.setExistingLiabilities(BigDecimal.valueOf(5000));
        app.setCreditScoreAtApplication(750);
        app.setRequestedCreditLimit(BigDecimal.valueOf(100000));

        // REQUIRED STATUS
        app.setApplicationStatus(ApplicationStatus.UNDER_REVIEW);

        // REQUIRED TIMESTAMP
        app.setSubmittedAt(Instant.now());

        // OPTIONAL BUT GOOD
        app.setRiskScore(BigDecimal.valueOf(2.5));

        return app;
    }

    public static UnderwritingDecision approvedDecision() {
        UnderwritingDecision decision = new UnderwritingDecision();
        decision.setDecision(DecisionType.AUTO_APPROVED);
        decision.setApprovedLimit(BigDecimal.valueOf(80000.00));
        decision.setApprovedApr(BigDecimal.valueOf(12.5));
        decision.setRiskScore(BigDecimal.valueOf(90));
        return decision;
    }

    public static UnderwritingDecision rejectedDecision() {
        UnderwritingDecision decision = new UnderwritingDecision();
        decision.setDecision(DecisionType.AUTO_REJECTED);
        decision.setRiskScore(BigDecimal.valueOf(70));
        return decision;
    }

    public static UnderwritingDecision pendingDecision() {
        UnderwritingDecision decision = new UnderwritingDecision();
        decision.setDecision(DecisionType.PENDING_REVIEW);
        decision.setRiskScore(BigDecimal.valueOf(60));
        return decision;
    }

    public static ApplicationDecisionRequest approveDecisionRequest() {
        ApplicationDecisionRequest req = new ApplicationDecisionRequest();
        req.setApproved(true);
        req.setApprovedCreditLimit(BigDecimal.valueOf(50000));
        req.setApprovedApr(BigDecimal.valueOf(10.0));
        req.setDecisionReason("Manual approval");
        return req;
    }

    public static ApplicationDecisionRequest rejectDecisionRequest() {
        ApplicationDecisionRequest req = new ApplicationDecisionRequest();
        req.setApproved(false);
        req.setDecisionReason("Low income");
        return req;
    }
    public static CreditCardProduct validCardProduct() {
        CreditCardProduct cp = new CreditCardProduct();
        cp.setProductName("Test Product");
        cp.setOnlineTransactionsAllowed(true);
        cp.setAtmWithdrawalAllowed(true);
        return cp;
    }

    public static CreditCard validCard(CreditAccount account, CreditCardProduct product) {
        CreditCard card = new CreditCard();
        card.setCreditAccount(account);
        card.setCardProduct(product);
        card.setCardStatus(CardStatus.ACTIVE);
        card.setCardFormat(CardFormat.VIRTUAL);
        card.setIssuanceReason(CardIssuanceReason.NEW_CARD);
        card.setMaskedCardNumber("411111XXXXXX" + (System.currentTimeMillis() % 9999));

        // ✅ EXPIRY (FIX)
        card.setExpiryMonth(12);
        card.setExpiryYear(2030);
        card.setExpiresAt(Instant.now().plus(5 * 365, ChronoUnit.DAYS));

        card.setIssuedAt(Instant.now());
        card.setIssuedBy("FIXTURE");

        return card;
    }
    public static CreditAccount validCreditAccount(Customer customer, CreditCardApplication app, CreditProduct product) {
        CreditAccount account = new CreditAccount();
        account.setAccountNumber("1001" + (System.currentTimeMillis() % 100000000L));
        account.setCustomer(customer);
        account.setApplication(app);
        account.setCreditProduct(product);
        account.setAccountStatus(AccountStatus.ACTIVE);
        
        // Financials
        account.setCreditLimit(BigDecimal.valueOf(50000));
        account.setApr(BigDecimal.valueOf(15.99));
        account.setCurrentBalance(BigDecimal.ZERO);
        account.setAvailableBalance(BigDecimal.valueOf(50000));
        
        // Terms
        account.setGracePeriodDays(45);
        account.setMinimumDuePercent(BigDecimal.valueOf(5.00));
        account.setLateFeeAmount(BigDecimal.valueOf(500));
        account.setStatementCycleDay(15);
        
        account.setActivatedAt(Instant.now());
        return account;
    }

    public static CreditCardProduct validCreditCardProduct() {
        CreditCardProduct cp = new CreditCardProduct();
        cp.setProductName("Visa Platinum");
        cp.setNetworkType(NetworkType.VISA);
        cp.setCardType(CardType.VIRTUAL);
        cp.setStatus(ProductStatus.ACTIVE);
        cp.setAnnualFee(BigDecimal.ZERO);
        cp.setCardValidityYears(5);
        cp.setForexMarkupPercent(BigDecimal.valueOf(3.5));
        cp.setStatementCycleDay(1);
        cp.setContactlessEnabled(true);
        cp.setInternationalUsageAllowed(false);
        cp.setOnlineTransactionsAllowed(true);
        cp.setAtmWithdrawalAllowed(true);
        cp.setEcommerceDailyLimit(BigDecimal.valueOf(50000));
        cp.setPosDailyLimit(BigDecimal.valueOf(50000));
        cp.setAtmDailyLimit(BigDecimal.valueOf(20000));
        cp.setCreatedAt(Instant.now());
        cp.setCreatedBy("SYSTEM");
        cp.setUpdatedAt(Instant.now());
        cp.setUpdatedBy("SYSTEM");
        return cp;
    }

    public static CreditCard validCreditCard(CreditAccount account, CreditCardProduct product) {
        CreditCard card = new CreditCard();
        card.setCreditAccount(account);
        card.setCardProduct(product);
        card.setCardFormat(CardFormat.VIRTUAL);
        card.setCardStatus(CardStatus.ACTIVE);
        card.setIssuanceReason(CardIssuanceReason.NEW_CARD);
        card.setMaskedCardNumber("411111XXXXXX" + (System.currentTimeMillis() % 9999));

        // ✅ EXPIRY (FIX)
        card.setExpiryMonth(12);
        card.setExpiryYear(2030);
        card.setExpiresAt(Instant.now().plus(5 * 365, ChronoUnit.DAYS)); // 🔥 REQUIRED

        // FEATURES
        card.setOnlineEnabled(true);
        card.setAtmEnabled(true);
        card.setInternationalEnabled(false);

        // TIMESTAMPS
        card.setIssuedAt(Instant.now());
        card.setIssuedBy("SYSTEM_TEST");

        return card;
    }
    
    public static BillingStatement validBillingStatement(CreditAccount account) {
        BillingStatement stmt = new BillingStatement();

        stmt.setAccount(account);
        stmt.setBillingPeriodStart(LocalDate.now().minusMonths(1));
        stmt.setBillingPeriodEnd(LocalDate.now());

        stmt.setOpeningBalance(BigDecimal.valueOf(1000));
        stmt.setTotalDebits(BigDecimal.valueOf(500));
        stmt.setTotalCredits(BigDecimal.valueOf(200));
        stmt.setInterestCharged(BigDecimal.valueOf(50));

        // ✅ CRITICAL FIX
        stmt.setRemainingAmount(BigDecimal.valueOf(1350)); 

        stmt.setClosingBalance(BigDecimal.valueOf(1350));
        stmt.setTotalAmountDue(BigDecimal.valueOf(1350));

        stmt.setMinimumDueAmount(BigDecimal.valueOf(200));
        stmt.setMinDuePercent(BigDecimal.valueOf(5));
        stmt.setMinDueFloor(BigDecimal.valueOf(200));

        stmt.setDueDate(LocalDate.now().minusDays(1)); // force due
        stmt.setAmountPaid(BigDecimal.ZERO);

        stmt.setLateFeeApplied(false);
        stmt.setStatementStatus(StatementStatus.GENERATED);
        stmt.setGeneratedAt(Instant.now());

        return stmt;
    }
}
