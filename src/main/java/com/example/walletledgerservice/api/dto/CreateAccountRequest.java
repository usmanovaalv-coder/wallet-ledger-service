package com.example.walletledgerservice.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateAccountRequest(@NotNull Long ownerId,
                                   @NotBlank @Pattern(regexp = "(?i)^[a-z]{3}$") String currency) {
}
