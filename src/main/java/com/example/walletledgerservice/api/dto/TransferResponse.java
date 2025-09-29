package com.example.walletledgerservice.api.dto;

import java.time.OffsetDateTime;

public record TransferResponse(long journalEntryId,
                               long fromAccountId,
                               long toAccountId,
                               long amountMinor,
                               String currency,
                               OffsetDateTime createdAt,
                               String description,
                               String externalRef) {
}