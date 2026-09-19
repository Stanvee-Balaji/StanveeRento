package com.example.demo.repository;

import com.example.demo.entity.InventoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository extends JpaRepository<InventoryEntity, UUID> {
    List<InventoryEntity> findByProduct_Id(UUID productId);
    void deleteByProduct_Id(UUID productId);
    Optional<InventoryEntity> findBySkuIgnoreCase(String sku);
}
