package com.example.demo.repository;

import com.example.demo.entity.PlanFeatureEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PlanFeatureRepository extends JpaRepository<PlanFeatureEntity, UUID> {

    List<PlanFeatureEntity> findByPlan_Id(UUID planId);

    void deleteByPlan_Id(UUID planId);
}
