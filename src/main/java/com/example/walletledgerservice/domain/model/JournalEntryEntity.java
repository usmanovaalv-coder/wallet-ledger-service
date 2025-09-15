package com.example.walletledgerservice.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.sql.Timestamp;

@Entity
@Table(name = "journal_entry")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class JournalEntryEntity extends BaseEntity {
    @Column(nullable = false, unique = true, length = 80)
    String idempotencyKey;

    @Column(length = 120)
    String externalRef;

    @Column(insertable = false, updatable = false)
    Timestamp createdAt;
}
