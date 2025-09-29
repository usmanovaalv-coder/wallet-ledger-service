package com.example.walletledgerservice.service;

import com.example.walletledgerservice.api.dto.TransferCommand;
import com.example.walletledgerservice.api.dto.TransferResponse;
import com.example.walletledgerservice.domain.model.AccountEntity;
import com.example.walletledgerservice.domain.model.JournalEntryEntity;
import com.example.walletledgerservice.domain.model.JournalLineEntity;
import com.example.walletledgerservice.domain.repository.AccountRepository;
import com.example.walletledgerservice.domain.repository.JournalEntryRepository;
import com.example.walletledgerservice.domain.repository.JournalLineRepository;
import com.example.walletledgerservice.enums.Side;
import com.example.walletledgerservice.exception.NotFoundException;
import jakarta.validation.ValidationException;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TransferService {

    AccountRepository accountRepository;
    JournalEntryRepository journalEntryRepository;
    JournalLineRepository journalLineRepository;

    @Transactional
    public TransferResponse createTransfer(@NotNull TransferCommand cmd) {
        final String key = cmd.idempotencyKey();
        final long amount = cmd.amountMinor();
        final String currency = cmd.currency();

        if (Objects.equals(cmd.fromAccountId(), cmd.toAccountId())) {
            throw new ValidationException("fromAccountId must differ from toAccountId");
        }

        log.info("Transfer request: key={}, from={}, to={}, amount={}, ccy={}, extRef={}",
                key, cmd.fromAccountId(), cmd.toAccountId(), amount, currency, cmd.externalRef());

        TransferResponse idempotent = findIdempotentIfAny(key);
        if (idempotent != null) return idempotent;

        Accounts pair = loadAccountsForUpdate(cmd.fromAccountId(), cmd.toAccountId());

        ensureCurrency(pair.from, currency, "fromAccountId");
        ensureCurrency(pair.to, currency, "toAccountId");
        ensureSufficientFunds(pair.from, amount);

        JournalEntryEntity entry = persistEntryIdempotently(
                key,
                trim(cmd.description(), 255),
                trim(cmd.externalRef(), 120)
        );

        persistLines(entry, pair, amount, currency);

        pair.from.setBalanceMinor(pair.from.getBalanceMinor() - amount);
        pair.to.setBalanceMinor(pair.to.getBalanceMinor() + amount);

        log.info("Transfer posted: entryId={}, from={} (newBal={}), to={} (newBal={}), amount={}, ccy={}",
                entry.getId(), pair.from.getId(), pair.from.getBalanceMinor(),
                pair.to.getId(), pair.to.getBalanceMinor(), amount, currency);

        return mapEntryToResponse(entry);
    }

    private TransferResponse findIdempotentIfAny(String key) {
        return journalEntryRepository.findByIdempotencyKey(key)
                .map(e -> {
                    log.info("Idempotent hit: key={}, entryId={}", key, e.getId());
                    return mapEntryToResponse(e);
                })
                .orElse(null);
    }

    private Accounts loadAccountsForUpdate(Long fromId, Long toId) {
        List<Long> sorted = Stream.of(fromId, toId).sorted().toList();

        AccountEntity first = loadForUpdate(sorted.get(0));
        AccountEntity second = loadForUpdate(sorted.get(1));

        AccountEntity from = first.getId().equals(fromId) ? first : second;
        AccountEntity to = (from == first) ? second : first;

        return new Accounts(from, to);
    }

    private AccountEntity loadForUpdate(Long id) {
        return accountRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Account not found: id=" + id));
    }

    private static void ensureCurrency(AccountEntity a, String expected, String field) {
        if (!expected.equalsIgnoreCase(a.getCurrency())) {
            throw new ValidationException(field + " has currency " + a.getCurrency() + ", expected " + expected);
        }
    }

    private static void ensureSufficientFunds(AccountEntity from, long amount) {
        if (from.getBalanceMinor() < amount) {
            throw new ValidationException("Insufficient funds");
        }
    }

    private JournalEntryEntity persistEntryIdempotently(String key, String description, String externalRef) {
        JournalEntryEntity entry = new JournalEntryEntity();
        entry.setIdempotencyKey(key);
        entry.setDescription(description);
        entry.setExternalRef(externalRef);

        try {
            return journalEntryRepository.saveAndFlush(entry);
        } catch (DataIntegrityViolationException e) {
            JournalEntryEntity again = journalEntryRepository.findByIdempotencyKey(key)
                    .orElseThrow(() -> e);
            log.info("Idempotent raced and resolved: key={}, entryId={}", key, again.getId());
            return again;
        }
    }

    private void persistLines(JournalEntryEntity entry, Accounts pair, long amount, String currency) {
        JournalLineEntity credit = new JournalLineEntity();
        credit.setEntry(entry);
        credit.setAccount(pair.from);
        credit.setSide(Side.CREDIT);
        credit.setAmountMinor(amount);
        credit.setCurrency(currency);

        JournalLineEntity debit = new JournalLineEntity();
        debit.setEntry(entry);
        debit.setAccount(pair.to);
        debit.setSide(Side.DEBIT);
        debit.setAmountMinor(amount);
        debit.setCurrency(currency);

        journalLineRepository.saveAll(List.of(credit, debit));
    }

    private static String trim(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }

    private record Accounts(AccountEntity from, AccountEntity to) {}

    private TransferResponse mapEntryToResponse(JournalEntryEntity e) {
        var lines = journalLineRepository.findByEntryIdWithAccount(e.getId());
        if (lines.size() < 2) {
            throw new IllegalStateException("Journal entry " + e.getId() + " has unexpected lines count=" + lines.size());
        }

        JournalLineEntity debit  = lines.stream().filter(l -> l.getSide() == Side.DEBIT).findFirst()
                .orElseThrow(() -> new IllegalStateException("No DEBIT line for entry " + e.getId()));
        JournalLineEntity credit = lines.stream().filter(l -> l.getSide() == Side.CREDIT).findFirst()
                .orElseThrow(() -> new IllegalStateException("No CREDIT line for entry " + e.getId()));

        if (debit.getAmountMinor() != credit.getAmountMinor()) {
            throw new IllegalStateException("Lines amount mismatch for entry " + e.getId());
        }
        if (!debit.getCurrency().equalsIgnoreCase(credit.getCurrency())) {
            throw new IllegalStateException("Lines currency mismatch for entry " + e.getId());
        }

        long amount = debit.getAmountMinor();
        String ccy = debit.getCurrency();

        return new TransferResponse(
                e.getId(),
                credit.getAccount().getId(),
                debit.getAccount().getId(),
                amount,
                ccy,
                toOffsetUtc(e.getCreatedAt()),
                e.getDescription(),
                e.getExternalRef()
        );
    }

    private static OffsetDateTime toOffsetUtc(java.sql.Timestamp ts) {
        if (ts == null) return null;
        return ts.toInstant().atOffset(ZoneOffset.UTC);
    }
}