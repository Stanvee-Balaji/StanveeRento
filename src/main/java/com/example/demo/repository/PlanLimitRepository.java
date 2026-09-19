package com.example.demo.repository;

import com.example.demo.entity.PlanLimitEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PlanLimitRepository extends JpaRepository<PlanLimitEntity, UUID> {

    List<PlanLimitEntity> findByPlan_Id(UUID planId);

    void deleteByPlan_Id(UUID planId);
}
