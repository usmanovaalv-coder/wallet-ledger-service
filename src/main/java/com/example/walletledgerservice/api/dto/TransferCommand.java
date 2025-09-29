package com.example.walletledgerservice.api.dto;

import jakarta.validation.constraints.*;

import java.util.Locale;

public record TransferCommand(@NotBlank @Size(max = 80) String idempotencyKey,
                              @NotNull Long fromAccountId,
                              @NotNull Long toAccountId,
                              @NotNull @Positive Long amountMinor,
                              @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
                              @Size(max = 255) String description,
                              @Size(max = 120) String externalRef) {

    public static TransferCommand of(String idempotencyKey, TransferRequest r) {

        return new TransferCommand(
                idempotencyKey,
                r.fromAccountId(), r.toAccountId(),
                r.amountMinor(), r.currency().toUpperCase(Locale.ROOT),
                r.description(), r.externalRef()
        );
    }
}
