package com.example.demo.repository;

import com.example.demo.entity.SuperAdminEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface SuperAdminRepository extends JpaRepository<SuperAdminEntity, UUID> {
    Optional<SuperAdminEntity> findByEmailIgnoreCaseAndDeletedAtIsNull(String email);
    
    
    long countByDeletedAtIsNull();
    
}
