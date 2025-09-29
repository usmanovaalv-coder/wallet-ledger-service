package com.example.walletledgerservice.service;

import com.example.walletledgerservice.domain.model.AccountEntity;
import com.example.walletledgerservice.domain.model.JournalEntryEntity;
import com.example.walletledgerservice.domain.model.JournalLineEntity;
import com.example.walletledgerservice.domain.repository.AccountRepository;
import com.example.walletledgerservice.domain.repository.JournalEntryRepository;
import com.example.walletledgerservice.domain.repository.JournalLineRepository;
import com.example.walletledgerservice.enums.Side;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@Profile({"dev", "test"})
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TreasuryDevService {

    AccountRepository accountRepository;
    JournalEntryRepository journalEntryRepository;
    JournalLineRepository journalLineRepository;

    static long TREASURY_OWNER = 0L;
    static long ISSUER_OWNER   = -1L;
    static String USD = "USD";

    /**
     * Guarantees that the treasury has enough USD for the next transfer.
     * If it doesn't, it will "mint" (double-write) the amount of the deficit.
     * Returns the id of the treasury account.
     */
    @Transactional
    public long ensureUsdTreasuryWithFunds(long requiredAmountMinor) {
        AccountEntity treasury = findOrCreate(TREASURY_OWNER, USD);

        if (treasury.getBalanceMinor() >= requiredAmountMinor) {
            return treasury.getId();
        }

        long deficit = requiredAmountMinor - treasury.getBalanceMinor();
        AccountEntity issuer = findOrCreate(ISSUER_OWNER, USD);

        var entry = createJournalEntry("DEV-MINT-USD-" + UUID.randomUUID(), "Dev/Test mint USD to treasury");

        persistLines(entry, treasury, issuer, deficit);

        treasury.setBalanceMinor(treasury.getBalanceMinor() + deficit);
        issuer.setBalanceMinor(issuer.getBalanceMinor() - deficit);

        log.info("Minted {} {} to treasury (treasuryId={}), issuerId={}", deficit, USD, treasury.getId(), issuer.getId());
        return treasury.getId();
    }

    private AccountEntity findOrCreate(long ownerId, String currency) {
        Optional<AccountEntity> existing = accountRepository.findByOwnerIdAndCurrency(ownerId, currency);
        if (existing.isPresent()) return existing.get();

        AccountEntity a = new AccountEntity();
        a.setOwnerId(ownerId);
        a.setCurrency(currency);
        a.setBalanceMinor(0);
        try {
            return accountRepository.saveAndFlush(a);
        } catch (DataIntegrityViolationException e) {
            return accountRepository.findByOwnerIdAndCurrency(ownerId, currency).orElseThrow(() -> e);
        }
    }

    private JournalEntryEntity createJournalEntry(String idempotencyKey, String description) {
        JournalEntryEntity entry = new JournalEntryEntity();
        entry.setIdempotencyKey(idempotencyKey);
        entry.setDescription(description);
        return journalEntryRepository.saveAndFlush(entry);
    }

    private void persistLines(JournalEntryEntity entry, AccountEntity treasury, AccountEntity issuer, long amount) {
        JournalLineEntity debitTreasury = new JournalLineEntity();
        debitTreasury.setEntry(entry);
        debitTreasury.setAccount(treasury);
        debitTreasury.setSide(Side.DEBIT);
        debitTreasury.setAmountMinor(amount);
        debitTreasury.setCurrency(USD);

        JournalLineEntity creditIssuer = new JournalLineEntity();
        creditIssuer.setEntry(entry);
        creditIssuer.setAccount(issuer);
        creditIssuer.setSide(Side.CREDIT);
        creditIssuer.setAmountMinor(amount);
        creditIssuer.setCurrency(USD);

        journalLineRepository.saveAll(List.of(debitTreasury, creditIssuer));
    }
}
