package com.example.walletledgerservice.service;

import com.example.walletledgerservice.api.dto.AccountDto;
import com.example.walletledgerservice.api.dto.CreateAccountRequest;
import com.example.walletledgerservice.domain.model.AccountEntity;
import com.example.walletledgerservice.domain.repository.AccountRepository;
import com.example.walletledgerservice.exception.ConflictException;
import com.example.walletledgerservice.exception.NotFoundException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AccountService {

    AccountRepository accountRepository;

    @Transactional
    public AccountDto create(CreateAccountRequest request) {
        String currency = request.currency().toUpperCase(Locale.ROOT);
        Long ownerId = request.ownerId();

        log.info("Creating account: ownerId={}, currency={}", ownerId, currency);

        accountRepository.findByOwnerIdAndCurrency(ownerId, currency)
                .ifPresent(a -> {
                    log.warn("Account already exists: ownerId={}, currency={}, existingId={}",
                            ownerId, currency, a.getId());
                    throw new ConflictException(
                            "Account already exists for owner=" + ownerId + ", currency=" + currency);
                });

        AccountEntity entity = new AccountEntity();
        entity.setOwnerId(ownerId);
        entity.setCurrency(currency);

        try {
            entity = accountRepository.save(entity);
            log.info("Account created: id={}, ownerId={}, currency={}", entity.getId(), ownerId, currency);

        } catch (DataIntegrityViolationException e) {
            log.warn("Create race detected: ownerId={}, currency={}", ownerId, currency);

            throw new ConflictException(
                    "Account already exists (race). owner=" + ownerId + ", currency=" + currency, e);
        }

        return toDto(entity);
    }

    @Transactional(readOnly = true)
    public AccountDto get(Long id) {
        return accountRepository.findById(id)
                .map(a -> {
                    log.debug("Loaded account id={}", id);
                    return toDto(a);
                })
                .orElseThrow(() -> new NotFoundException("Account not found: id=" + id));
    }

    private static AccountDto toDto(AccountEntity e) {
        return new AccountDto(e.getId(), e.getOwnerId(), e.getCurrency(), e.getBalanceMinor());
    }
}
