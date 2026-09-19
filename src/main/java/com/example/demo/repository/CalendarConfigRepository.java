package com.example.demo.repository;

import com.example.demo.entity.CalendarConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CalendarConfigRepository extends JpaRepository<CalendarConfigEntity, UUID> {

    // NOTE: entity field is named `active` (getter isActive()) — must use
    // "ActiveTrue" in derived query names, not "IsActiveTrue".

    Optional<CalendarConfigEntity> findByScopeAndActiveTrue(String scope); // GLOBAL

    Optional<CalendarConfigEntity> findByCategory_IdAndScopeAndActiveTrue(UUID categoryId, String scope);

    Optional<CalendarConfigEntity> findByProduct_IdAndScopeAndActiveTrue(UUID productId, String scope);

    List<CalendarConfigEntity> findByActiveTrue();
}