package com.example.demo.repository;

import com.example.demo.entity.ProductPlanAccessEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ProductPlanAccessRepository extends JpaRepository<ProductPlanAccessEntity, UUID> {

    @Query("select a.planId from ProductPlanAccessEntity a where a.productId = :productId")
    List<UUID> findPlanIdsByProductId(@Param("productId") UUID productId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from ProductPlanAccessEntity a where a.productId = :productId")
    void deleteByProductId(@Param("productId") UUID productId);
}