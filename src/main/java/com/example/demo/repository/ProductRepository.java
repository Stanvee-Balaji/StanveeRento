package com.example.demo.repository;

import com.example.demo.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<ProductEntity, UUID>,
        JpaSpecificationExecutor<ProductEntity> {
	
	
	
	
    Optional<ProductEntity> findByIdAndDeletedAtIsNull(UUID id);

    // ← ADD THIS ONE LINE to your existing ProductRepository
    long countByCategoryIdAndDeletedAtIsNull(UUID categoryId);
    
    
    long countByCategory_IdAndActiveTrueAndVisibleTrueAndDeletedAtIsNull(UUID categoryId);
      
      
      
      
}
