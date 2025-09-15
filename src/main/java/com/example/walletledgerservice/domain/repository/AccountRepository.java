package com.example.walletledgerservice.domain.repository;

import com.example.walletledgerservice.domain.model.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<AccountEntity, Long> {
    Optional<AccountEntity> findByOwnerIdAndCurrency(Long ownerId, String currency);
}
