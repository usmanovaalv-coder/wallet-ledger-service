package com.example.walletledgerservice;

import com.example.walletledgerservice.api.dto.AccountDto;
import com.example.walletledgerservice.api.dto.CreateAccountRequest;
import com.example.walletledgerservice.domain.model.AccountEntity;
import com.example.walletledgerservice.domain.repository.AccountRepository;
import com.example.walletledgerservice.exception.ConflictException;
import com.example.walletledgerservice.exception.NotFoundException;
import com.example.walletledgerservice.service.AccountService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AccountServiceTest {
    @Mock
    AccountRepository accountRepository;

    @InjectMocks
    AccountService accountService;

    static final Long ACCOUNT_ID = 1L;
    static final Long OWNER_ID = 42L;
    static final String USD_CURRENCY = "USD";

    @Test
    void create_ok_returnsDto() {
        var req = new CreateAccountRequest(OWNER_ID, "usd");

        when(accountRepository.findByOwnerIdAndCurrency(OWNER_ID, USD_CURRENCY))
                .thenReturn(Optional.empty());

        var saved = new AccountEntity();
        saved.setId(ACCOUNT_ID);
        saved.setOwnerId(OWNER_ID);
        saved.setCurrency(USD_CURRENCY);
        saved.setBalanceMinor(0);
        when(accountRepository.save(any(AccountEntity.class))).thenReturn(saved);

        AccountDto dto = accountService.create(req);

        assertThat(dto.id()).isEqualTo(ACCOUNT_ID);
        assertThat(dto.ownerId()).isEqualTo(OWNER_ID);
        assertThat(dto.currency()).isEqualTo(USD_CURRENCY);
        assertThat(dto.balanceMinor()).isZero();

        var captor = ArgumentCaptor.forClass(AccountEntity.class);
        verify(accountRepository).save(captor.capture());
        assertThat(captor.getValue().getCurrency()).isEqualTo(USD_CURRENCY);
    }

    @Test
    void create_duplicate_throwsConflict() {
        var req = new CreateAccountRequest(OWNER_ID, USD_CURRENCY);

        when(accountRepository.findByOwnerIdAndCurrency(OWNER_ID, USD_CURRENCY))
                .thenReturn(Optional.of(new AccountEntity()));

        assertThatThrownBy(() -> accountService.create(req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Account already exists");
        verify(accountRepository, never()).save(any());
    }

    @Test
    void create_race_conflictByConstraint_throwsConflict() {
        var req = new CreateAccountRequest(OWNER_ID, USD_CURRENCY);

        when(accountRepository.findByOwnerIdAndCurrency(OWNER_ID, USD_CURRENCY))
                .thenReturn(Optional.empty());

        when(accountRepository.save(any(AccountEntity.class)))
                .thenThrow(new DataIntegrityViolationException("unique constraint"));

        assertThatThrownBy(() -> accountService.create(req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("race");
    }

    @Test
    void get_ok_returnsDto() {
        var e = new AccountEntity();
        e.setId(ACCOUNT_ID);
        e.setOwnerId(OWNER_ID);
        e.setCurrency(USD_CURRENCY);
        e.setBalanceMinor(123);

        when(accountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(e));

        AccountDto dto = accountService.get(ACCOUNT_ID);

        assertThat(dto.id()).isEqualTo(ACCOUNT_ID);
        assertThat(dto.ownerId()).isEqualTo(OWNER_ID);
        assertThat(dto.currency()).isEqualTo(USD_CURRENCY);
        assertThat(dto.balanceMinor()).isEqualTo(123);
    }

    @Test
    void get_notFound_throwsNoSuchElement() {
        when(accountRepository.findById(Long.MAX_VALUE)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> accountService.get(Long.MAX_VALUE))
                .isInstanceOf(NotFoundException.class);
    }
}
