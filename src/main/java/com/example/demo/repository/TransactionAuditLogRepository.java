package com.example.demo.repository;

import com.example.demo.entity.TransactionAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionAuditLogRepository extends JpaRepository<TransactionAuditLog, Long> {
    List<TransactionAuditLog> findByOrderIdOrderByCreatedAtAsc(String orderId);
}
