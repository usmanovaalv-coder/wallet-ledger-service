package com.example.walletledgerservice.api.dto;

public record AccountDto(Long id, Long ownerId, String currency, long balanceMinor) {
}
