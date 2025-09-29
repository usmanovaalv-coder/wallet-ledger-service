package com.example.walletledgerservice.domain.model;

import com.example.walletledgerservice.enums.Side;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Entity
@Setter
@Getter
@Table(name = "journal_line")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JournalLineEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id", nullable = false)
    JournalEntryEntity entry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    AccountEntity account;

    @Column(nullable = false)
    long amountMinor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 6)
    Side side;

    @Column(nullable = false, length = 3)
    String currency;
}
