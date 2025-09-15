package com.example.walletledgerservice.domain.repository;

import com.example.walletledgerservice.domain.model.JournalEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JournalEntryRepository extends JpaRepository<JournalEntryEntity, Long> {
}
