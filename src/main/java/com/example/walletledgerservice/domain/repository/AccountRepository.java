package com.example.walletledgerservice.domain.repository;

import com.example.walletledgerservice.domain.model.AccountEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<AccountEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT a
                FROM AccountEntity a
            WHERE a.id = :id
            """)
    Optional<AccountEntity> findByIdForUpdate(@Param("id") Long id);

    Optional<AccountEntity> findByOwnerIdAndCurrency(Long ownerId, String currency);
}
