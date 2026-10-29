package com.example.demo.repository;

import com.example.demo.entity.DepositEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DepositRepository extends JpaRepository<DepositEntity, UUID>, JpaSpecificationExecutor<DepositEntity> {

    Optional<DepositEntity> findByOrderId(String orderId);

    List<DepositEntity> findByUsernameOrderByCreatedAtDesc(String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from DepositEntity d where d.orderId = :orderId")
    Optional<DepositEntity> findByOrderIdForUpdate(@Param("orderId") String orderId);

    /** one row: [count, sumDepositPaid, sumDeducted, sumRefunded] */
    @Query("select count(d), coalesce(sum(d.depositPaid),0), coalesce(sum(d.deductedTotal),0), coalesce(sum(d.refundedTotal),0) from DepositEntity d")
    List<Object[]> totals();
}
