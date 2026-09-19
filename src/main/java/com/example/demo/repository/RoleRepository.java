package com.example.demo.repository;

import com.example.demo.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<RoleEntity, UUID>, JpaSpecificationExecutor<RoleEntity> {

    Optional<RoleEntity> findByRoleNameIgnoreCaseAndDeletedAtIsNull(String roleName);

    Optional<RoleEntity> findByIdAndDeletedAtIsNull(UUID id);

    List<RoleEntity> findByDeletedAtIsNullOrderByRoleNameAsc();
}
