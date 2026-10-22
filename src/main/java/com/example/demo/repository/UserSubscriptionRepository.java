package com.example.demo.repository;

import com.example.demo.entity.UserSubscriptionEntity;
import com.example.demo.entity.UserSubscriptionEntity.Status;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

public interface UserSubscriptionRepository
        extends JpaRepository<UserSubscriptionEntity, UUID>, JpaSpecificationExecutor<UserSubscriptionEntity> {

    Optional<UserSubscriptionEntity> findByOrderId(String orderId);

    /** Row lock: two simultaneous IPNs / admin clicks for the same order are serialized -> no double activation. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from UserSubscriptionEntity p where p.orderId = :orderId")
    Optional<UserSubscriptionEntity> findByOrderIdForUpdate(@Param("orderId") String orderId);

    List<UserSubscriptionEntity> findByUsernameOrderByCreatedAtDesc(String username);

    Optional<UserSubscriptionEntity> findFirstByUsernameAndPlanIdAndStatusAndCreatedAtAfterOrderByCreatedAtDesc(
            String username, UUID planId, Status status, LocalDateTime after);

    Optional<UserSubscriptionEntity> findFirstByUsernameAndStatusAndStartDateLessThanEqualAndEndDateAfterOrderByEndDateDesc(
            String username, Status status, LocalDateTime now1, LocalDateTime now2);

    List<UserSubscriptionEntity> findByStatusInAndCreatedAtBeforeOrderByCreatedAtAsc(
            Collection<Status> statuses, LocalDateTime cutoff);

    List<UserSubscriptionEntity> findByStatusAndEndDateBefore(Status status, LocalDateTime time);

    long countByStatus(Status status);

    long countByStatusAndStartDateLessThanEqualAndEndDateAfter(Status status, LocalDateTime now1, LocalDateTime now2);

    @Query("select max(p.endDate) from UserSubscriptionEntity p " +
           "where p.username = :username and p.status = :status and p.endDate > :now")
    LocalDateTime findLatestEnd(@Param("username") String username, @Param("status") Status status,
                                @Param("now") LocalDateTime now);

    @Query("select sum(p.price) from UserSubscriptionEntity p where p.status in :statuses")
    BigDecimal sumPrice(@Param("statuses") Collection<Status> statuses);

    @Query("select sum(p.price) from UserSubscriptionEntity p where p.status in :statuses and p.paidAt >= :since")
    BigDecimal sumPriceSince(@Param("statuses") Collection<Status> statuses, @Param("since") LocalDateTime since);

    @Query("select p.planName, count(p), sum(p.price) from UserSubscriptionEntity p " +
           "where p.status in :statuses group by p.planName order by sum(p.price) desc")
    List<Object[]> revenueByPlan(@Param("statuses") Collection<Status> statuses);
}
