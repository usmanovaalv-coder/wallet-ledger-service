package com.example.walletledgerservice.domain.repository;

import com.example.walletledgerservice.domain.model.JournalLineEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JournalLineRepository extends JpaRepository<JournalLineEntity, Long> {
}
