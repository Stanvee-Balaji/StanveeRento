package com.example.demo.repository;

import com.example.demo.entity.PermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PermissionRepository extends JpaRepository<PermissionEntity, UUID>, JpaSpecificationExecutor<PermissionEntity> {

    Optional<PermissionEntity> findByCodeIgnoreCaseAndDeletedAtIsNull(String code);

    Optional<PermissionEntity> findByIdAndDeletedAtIsNull(UUID id);

    List<PermissionEntity> findByDeletedAtIsNullOrderByCodeAsc();

    List<PermissionEntity> findByModule_IdAndDeletedAtIsNullOrderByCodeAsc(UUID moduleId);

    boolean existsByModule_IdAndDeletedAtIsNull(UUID moduleId);
}
