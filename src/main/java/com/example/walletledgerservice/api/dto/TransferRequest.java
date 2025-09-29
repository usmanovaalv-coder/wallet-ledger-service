package com.example.walletledgerservice.api.dto;

import jakarta.validation.constraints.*;

public record TransferRequest(@NotNull @Positive Long fromAccountId,
                             @NotNull @Positive Long toAccountId,
                             @NotNull @Positive Long amountMinor,
                             @NotBlank @Pattern(regexp = "^[A-Z]{3}$") String currency, // ISO 4217, UPPER_CASE
                             @Size(max = 255) String description,
                             @Size(max = 120) String externalRef) {}