package com.example.demo.repository;

import com.example.demo.entity.InventoryBlockEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryBlockRepository extends JpaRepository<InventoryBlockEntity, UUID>,
        JpaSpecificationExecutor<InventoryBlockEntity> {

    // NOTE: entity field is named `active` (getter isActive()) — Spring Data
    // derives query properties from the FIELD name, not the getter, so these
    // must say "ActiveTrue", not "IsActiveTrue".

    Optional<InventoryBlockEntity> findByIdAndActiveTrue(UUID id);

    List<InventoryBlockEntity> findByInventory_IdAndActiveTrue(UUID inventoryId);

    // Overlap check: any active block on this inventory unit whose range intersects [from, to].
    List<InventoryBlockEntity> findByInventory_IdAndActiveTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            UUID inventoryId, LocalDate to, LocalDate from);

    List<InventoryBlockEntity> findByProduct_IdAndActiveTrueAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            UUID productId, LocalDate to, LocalDate from);

    List<InventoryBlockEntity> findByProduct_IdAndActiveTrue(UUID productId);
}