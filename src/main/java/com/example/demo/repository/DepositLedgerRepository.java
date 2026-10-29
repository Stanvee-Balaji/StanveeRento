package com.example.demo.repository;

import com.example.demo.entity.DepositLedgerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DepositLedgerRepository extends JpaRepository<DepositLedgerEntity, UUID> {
    List<DepositLedgerEntity> findByDepositIdOrderByCreatedAtAsc(UUID depositId);
    Optional<DepositLedgerEntity> findByDepositIdAndIdempotencyKey(UUID depositId, String idempotencyKey);
}
