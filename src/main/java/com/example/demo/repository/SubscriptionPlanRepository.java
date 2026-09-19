package com.example.demo.repository;

import com.example.demo.entity.SubscriptionPlanEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubscriptionPlanRepository
        extends JpaRepository<SubscriptionPlanEntity, UUID>, JpaSpecificationExecutor<SubscriptionPlanEntity> {

    Optional<SubscriptionPlanEntity> findByIdAndDeletedAtIsNull(UUID id);

    Optional<SubscriptionPlanEntity> findByPlanNameIgnoreCaseAndDeletedAtIsNull(String planName);

    List<SubscriptionPlanEntity> findByDeletedAtIsNullOrderByCreatedAtAsc();

    List<SubscriptionPlanEntity> findByDeletedAtIsNullAndStatusOrderByCreatedAtAsc(String status);
}
