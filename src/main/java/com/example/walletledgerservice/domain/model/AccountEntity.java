package com.example.walletledgerservice.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.sql.Timestamp;


@Entity
@Table(name = "account")
@Getter @Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AccountEntity extends BaseEntity {
    @Column(nullable = false)
    Long ownerId;

    @Column(nullable = false, length = 3)
    String currency;

    @Column(nullable = false)
    long balanceMinor;

    @Version
    @Column(nullable = false)
    Long version;

    @Column(insertable = false, updatable = false)
    Timestamp createdAt;
}
