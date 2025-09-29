package com.example.walletledgerservice.domain.repository;

import com.example.walletledgerservice.domain.model.JournalLineEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JournalLineRepository extends JpaRepository<JournalLineEntity, Long> {
    @Query("""
           SELECT l
           FROM JournalLineEntity l
           JOIN FETCH l.account a
           WHERE l.entry.id = :entryId
           """)
    List<JournalLineEntity> findByEntryIdWithAccount(Long entryId);
}
