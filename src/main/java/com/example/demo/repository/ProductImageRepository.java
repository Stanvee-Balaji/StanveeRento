package com.example.demo.repository;

import com.example.demo.entity.ProductImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ProductImageRepository extends JpaRepository<ProductImageEntity, UUID> {
    List<ProductImageEntity> findByProduct_Id(UUID productId);
    void deleteByProduct_Id(UUID productId);
}
