package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

// Mapping table: role_permissions (many-to-many between RoleEntity and PermissionEntity)
@Entity
@Table(
    name = "role_permissions",
    uniqueConstraints = @UniqueConstraint(columnNames = {"role_id", "permission_id"})
)
public class RolePermissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private RoleEntity role;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "permission_id", nullable = false)
    private PermissionEntity permission;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public RoleEntity getRole() { return role; }
    public void setRole(RoleEntity role) { this.role = role; }
    public PermissionEntity getPermission() { return permission; }
    public void setPermission(PermissionEntity permission) { this.permission = permission; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
