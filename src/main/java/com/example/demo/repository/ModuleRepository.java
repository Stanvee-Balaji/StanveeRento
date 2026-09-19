package com.example.demo.repository;

import com.example.demo.entity.ModuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ModuleRepository extends JpaRepository<ModuleEntity, UUID>, JpaSpecificationExecutor<ModuleEntity> {

    Optional<ModuleEntity> findByCodeIgnoreCaseAndDeletedAtIsNull(String code);

    Optional<ModuleEntity> findByIdAndDeletedAtIsNull(UUID id);

    List<ModuleEntity> findByDeletedAtIsNullOrderByNameAsc();
}
