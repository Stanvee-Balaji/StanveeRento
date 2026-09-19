package com.example.demo.repository;

import com.example.demo.entity.AuditLogEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, UUID>, JpaSpecificationExecutor<AuditLogEntity> {
}
