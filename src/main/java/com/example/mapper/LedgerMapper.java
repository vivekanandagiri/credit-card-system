package com.example.mapper;

import org.springframework.stereotype.Component;

import com.example.dto.response.LedgerEntryResponse;
import com.example.entity.LedgerEntry;

@Component
public class LedgerMapper {

    public LedgerEntryResponse toResponse(LedgerEntry entry) {
        return LedgerEntryResponse.builder()
                .id(entry.getId())
                .accountId(entry.getAccountId())
                .entryType(entry.getEntryType())
                .amount(entry.getAmount())
                .referenceType(entry.getReferenceType())
                .build();
    }
}
