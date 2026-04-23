package com.example.mapper;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.example.dto.response.TransactionDetailResponse;
import com.example.dto.response.TransactionSummaryResponse;
import com.example.entity.*;
import com.example.enums.*;

class TransactionMapperTest {

    private final TransactionMapper mapper = new TransactionMapper();

    @Test
    void shouldMapToResponse_success() {

        UUID txnId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();

        CreditCard card = new CreditCard();
        card.setCardId(cardId);
        card.setMaskedCardNumber("4111XXXXXX1234");
        card.setCardFormat(CardFormat.VIRTUAL);

        CreditAccount account = new CreditAccount();
        account.setAccountId(accountId);
        account.setAccountNumber("123456789012");

        Transaction txn = new Transaction();
        txn.setTransactionId(txnId);
        txn.setInternalReference("INT_REF");
        txn.setCard(card);
        txn.setAccount(account);
        txn.setTransactionType(TransactionType.PURCHASE);
        txn.setTransactionStatus(TransactionStatus.APPROVED);
        txn.setAmount(BigDecimal.valueOf(1000));
        txn.setCurrency(Currency.INR);
        txn.setMerchantName("Amazon");
        txn.setMerchantCategoryCode("5411");
        txn.setMerchantCategoryName("GROCERY");
        txn.setDeclineReason(null);
        txn.setTransactionTime(Instant.now());

        TransactionDetailResponse res = mapper.toResponse(txn);

        assertThat(res).isNotNull();
        assertThat(res.getTransactionId()).isEqualTo(txnId);
        assertThat(res.getInternalReference()).isEqualTo("INT_REF");

        assertThat(res.getCardId()).isEqualTo(cardId);
        assertThat(res.getMaskedCardNumber()).isEqualTo("4111XXXXXX1234");
        assertThat(res.getCardFormat()).isEqualTo("VIRTUAL");

        assertThat(res.getAccountId()).isEqualTo(accountId);
        assertThat(res.getAccountNumber()).isEqualTo("123456789012");

        assertThat(res.getTransactionType()).isEqualTo(TransactionType.PURCHASE);
        assertThat(res.getTransactionStatus()).isEqualTo(TransactionStatus.APPROVED);

        assertThat(res.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        assertThat(res.getCurrency()).isEqualTo(Currency.INR);

        assertThat(res.getMerchantName()).isEqualTo("Amazon");
        assertThat(res.getMerchantCategoryCode()).isEqualTo("5411");
        assertThat(res.getMerchantCategoryName()).isEqualTo("GROCERY");

        assertThat(res.getDeclineReason()).isNull();
        assertThat(res.getTransactionTime()).isNotNull();
    }

    @Test
    void shouldMapToResponse_withDeclineReason() {
        Transaction txn = new Transaction();
        txn.setTransactionId(UUID.randomUUID());
        txn.setInternalReference("REF");

        CreditCard card = new CreditCard();
        card.setCardId(UUID.randomUUID());
        card.setMaskedCardNumber("4111XXXX");
        card.setCardFormat(CardFormat.PHYSICAL);

        CreditAccount account = new CreditAccount();
        account.setAccountId(UUID.randomUUID());
        account.setAccountNumber("ACC123");

        txn.setCard(card);
        txn.setAccount(account);

        txn.setTransactionType(TransactionType.PURCHASE);
        txn.setTransactionStatus(TransactionStatus.DECLINED);
        txn.setAmount(BigDecimal.TEN);
        txn.setCurrency(Currency.INR);
        txn.setDeclineReason("INSUFFICIENT_FUNDS");
        txn.setTransactionTime(Instant.now());

        TransactionDetailResponse res = mapper.toResponse(txn);

        assertThat(res.getDeclineReason()).isEqualTo("INSUFFICIENT_FUNDS");
    }

    // ================= SUMMARY =================

    @Test
    void shouldMapToSummaryResponse_success() {

        Transaction txn = new Transaction();
        txn.setTransactionId(UUID.randomUUID());
        txn.setInternalReference("REF123");
        txn.setTransactionStatus(TransactionStatus.APPROVED);
        txn.setTransactionType(TransactionType.PURCHASE);
        txn.setAmount(BigDecimal.valueOf(500));
        txn.setCurrency(Currency.INR);
        txn.setMerchantName("Flipkart");
        txn.setTransactionTime(Instant.now());

        TransactionSummaryResponse res = mapper.toSummaryResponse(txn);

        assertThat(res).isNotNull();
        assertThat(res.getReferenceNumber()).isEqualTo("REF123");
        assertThat(res.getTransactionStatus()).isEqualTo(TransactionStatus.APPROVED);
        assertThat(res.getTransactionType()).isEqualTo(TransactionType.PURCHASE);
        assertThat(res.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(500));
        assertThat(res.getMerchantName()).isEqualTo("Flipkart");
    }
}