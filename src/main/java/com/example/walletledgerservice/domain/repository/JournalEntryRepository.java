package com.example.walletledgerservice.domain.repository;

import com.example.walletledgerservice.domain.model.JournalEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntryEntity, Long> {

    Optional<JournalEntryEntity> findByIdempotencyKey(String idempotencyKey);

}
