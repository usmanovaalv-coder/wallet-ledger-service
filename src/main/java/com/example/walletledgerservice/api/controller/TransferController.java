package com.example.walletledgerservice.api.controller;

import com.example.walletledgerservice.api.dto.TransferCommand;
import com.example.walletledgerservice.api.dto.TransferRequest;
import com.example.walletledgerservice.api.dto.TransferResponse;
import com.example.walletledgerservice.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Transfers", description = "Money transfers (double-entry)")
@Validated
@RestController
@RequestMapping("/api/v1/transfers")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TransferController {

    TransferService transferService;

    @Operation(
            summary = "Create transfer",
            description = "Creates a money transfer between two accounts as a journal entry with two lines (DEBIT/CREDIT)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Transfer created",
                    content = @Content(schema = @Schema(implementation = TransferResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation error or currency mismatch",
                    content = @Content), // опиши ApiError, если у тебя есть общий класс ошибок
            @ApiResponse(responseCode = "404", description = "Account not found",
                    content = @Content),
            @ApiResponse(responseCode = "409", description = "Conflict (idempotency duplicate or insufficient funds)",
                    content = @Content)
    })
    @PostMapping
    public ResponseEntity<TransferResponse> createTransfer(
            @RequestHeader(value = "Idempotency-Key") @NotBlank @Size(min = 1, max = 80) String idempotencyKey,
            @Valid @RequestBody TransferRequest request
    ) {
        var cmd = TransferCommand.of(idempotencyKey, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transferService.createTransfer(cmd));
    }
}
