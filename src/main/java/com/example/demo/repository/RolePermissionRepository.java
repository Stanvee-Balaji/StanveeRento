package com.example.demo.repository;

import com.example.demo.entity.RolePermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RolePermissionRepository extends JpaRepository<RolePermissionEntity, UUID> {

    List<RolePermissionEntity> findByRole_Id(UUID roleId);

    Optional<RolePermissionEntity> findByRole_IdAndPermission_Id(UUID roleId, UUID permissionId);

    @Query("select case when count(rp) > 0 then true else false end " +
           "from RolePermissionEntity rp " +
           "where rp.role.id = :roleId and rp.permission.id = :permissionId " +
           "and rp.role.active = true and rp.permission.active = true")
    boolean existsByRole_IdAndPermission_IdAndRole_ActiveTrueAndPermission_ActiveTrue(
            @Param("roleId") UUID roleId, @Param("permissionId") UUID permissionId);

    void deleteByRole_IdAndPermission_Id(UUID roleId, UUID permissionId);

    void deleteByRole_Id(UUID roleId);

    void deleteByPermission_Id(UUID permissionId);

    boolean existsByPermission_Id(UUID permissionId);
}
