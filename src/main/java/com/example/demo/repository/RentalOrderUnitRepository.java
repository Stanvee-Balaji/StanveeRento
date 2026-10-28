package com.example.demo.repository;

import com.example.demo.entity.RentalOrderUnitEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface RentalOrderUnitRepository extends JpaRepository<RentalOrderUnitEntity, UUID> {

    List<RentalOrderUnitEntity> findByOrderPk(UUID orderPk);

    List<RentalOrderUnitEntity> findByOrderPkAndActiveTrue(UUID orderPk);

    long countByOrderPkAndActiveTrue(UUID orderPk);

    /** Units of a product held by a live order for any day in [fromDate, toDate] (cleaning buffer included via blockedUntil). */
    @Query("select distinct u.inventoryId from RentalOrderUnitEntity u where u.productId = :pid and u.active = true "
         + "and u.startDate <= :toDate and u.blockedUntil >= :fromDate")
    List<UUID> findOccupiedInventoryIds(@Param("pid") UUID productId,
                                        @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    @Query("select u from RentalOrderUnitEntity u where u.productId = :pid and u.active = true "
         + "and u.startDate <= :toDate and u.blockedUntil >= :fromDate")
    List<RentalOrderUnitEntity> findActiveOverlapping(@Param("pid") UUID productId,
                                                      @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    @Query("select count(u) from RentalOrderUnitEntity u where u.inventoryId = :iid and u.active = true "
         + "and u.startDate <= :toDate and u.blockedUntil >= :fromDate")
    long countOverlapForUnit(@Param("iid") UUID inventoryId,
                             @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate);

    /** Live units whose rental or cleaning window covers the given day (used by the inventory dashboard). */
    @Query("select u from RentalOrderUnitEntity u where u.active = true and u.startDate <= :d and u.blockedUntil >= :d")
    List<RentalOrderUnitEntity> findActiveCovering(@Param("d") LocalDate day);
}
