package com.example.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import com.example.enums.EntryType;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LedgerEntryResponse {

    private UUID id;
    private UUID accountId;
    private EntryType entryType;
    private BigDecimal amount;
    private String referenceType;
}
