package com.example.walletledgerservice.api.controller;

import com.example.walletledgerservice.api.dto.TransferCommand;
import com.example.walletledgerservice.api.dto.TransferRequest;
import com.example.walletledgerservice.api.dto.TransferResponse;
import com.example.walletledgerservice.service.TransferService;
import com.example.walletledgerservice.service.TreasuryDevService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Profile({"dev", "test"})
@RequestMapping("/api/v1/dev/treasury")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DevMintController {
    TreasuryDevService treasuryDevService;
    TransferService transferService;

    /**
     * Top up your account via a regular transfer from the USD treasury.
     * If the treasury does not have enough funds, first “mint” them.
     * Example: POST / api/ v1/ dev/ mint/ usd?toAccountId=123&amountMinor=500000
     */
    @PostMapping("/mint/usd")
    public ResponseEntity<TransferResponse> mintUsd(
            @RequestParam long toAccountId,
            @RequestParam long amountMinor,
            @RequestHeader(value = "Idempotency-Key") String idempotencyKey
    ) {
        long treasuryId = treasuryDevService.ensureUsdTreasuryWithFunds(amountMinor);

        TransferRequest request = new TransferRequest(
                treasuryId,
                toAccountId,
                amountMinor,
                "USD",
                "DEV mint USD",
                "dev-mint"
        );
        TransferResponse resp = transferService.createTransfer(TransferCommand.of(idempotencyKey, request));
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }
}
