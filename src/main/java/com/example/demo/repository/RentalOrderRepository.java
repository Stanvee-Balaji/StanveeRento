package com.example.demo.repository;

import com.example.demo.entity.RentalOrderEntity;
import com.example.demo.entity.RentalOrderEntity.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

public interface RentalOrderRepository extends JpaRepository<RentalOrderEntity, UUID>, JpaSpecificationExecutor<RentalOrderEntity> {

    Optional<RentalOrderEntity> findByOrderId(String orderId);

    /** Row lock: every status transition runs under this, so duplicate callbacks serialize. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from RentalOrderEntity o where o.orderId = :orderId")
    Optional<RentalOrderEntity> findByOrderIdForUpdate(@Param("orderId") String orderId);

    List<RentalOrderEntity> findByStatusAndExpiresAtBefore(OrderStatus status, LocalDateTime t);

    List<RentalOrderEntity> findByStatusInAndCreatedAtBeforeOrderByCreatedAtAsc(Collection<OrderStatus> statuses, LocalDateTime t);

    long countByStatus(OrderStatus status);

    long countByFulfilmentStatus(FulfilmentStatus status);

    long countByCreatedAtAfter(LocalDateTime t);

    @Query("select coalesce(sum(o.totalPayable), 0) from RentalOrderEntity o where o.paymentStatus = :ps")
    BigDecimal sumTotalByPaymentStatus(@Param("ps") PaymentStatus ps);

    @Query("select coalesce(sum(o.depositAmount), 0) from RentalOrderEntity o where o.depositStatus = :ds")
    BigDecimal sumDepositByStatus(@Param("ds") DepositStatus ds);
}
