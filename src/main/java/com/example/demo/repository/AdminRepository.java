package com.example.demo.repository;

import com.example.demo.entity.AdminEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AdminRepository extends JpaRepository<AdminEntity, UUID>, JpaSpecificationExecutor<AdminEntity> {

    Optional<AdminEntity> findByEmailIgnoreCaseAndDeletedAtIsNull(String email);

    Optional<AdminEntity> findByIdAndDeletedAtIsNull(UUID id);

    List<AdminEntity> findByDeletedAtIsNullAndRole_Id(UUID roleId);

    long countByDeletedAtIsNull();

    long countByDeletedAtIsNullAndActive(boolean active);
}
