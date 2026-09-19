package com.example.demo.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class AdminDto {

    private AdminDto() {}

    // NOTE: No "permissions" field anywhere here by design.
    // Access is fully derived from roleId at read time.
    public static class CreateAdminRequest {
        @NotBlank private String fullName;
        @NotBlank @Email private String email;
        private String phone;
        @NotBlank @Size(min = 8, max = 100) private String password;
        @NotNull private UUID roleId;

        public String getFullName() { return fullName; }
        public void setFullName(String v) { fullName = v; }
        public String getEmail() { return email; }
        public void setEmail(String v) { email = v; }
        public String getPhone() { return phone; }
        public void setPhone(String v) { phone = v; }
        public String getPassword() { return password; }
        public void setPassword(String v) { password = v; }
        public UUID getRoleId() { return roleId; }
        public void setRoleId(UUID v) { roleId = v; }
    }

    public static class UpdateAdminRequest {
        @NotBlank private String fullName;
        private String phone;
        @NotNull private UUID roleId;

        public String getFullName() { return fullName; }
        public void setFullName(String v) { fullName = v; }
        public String getPhone() { return phone; }
        public void setPhone(String v) { phone = v; }
        public UUID getRoleId() { return roleId; }
        public void setRoleId(UUID v) { roleId = v; }
    }

    public static class ChangeRoleRequest {
        @NotNull private UUID roleId;
        public UUID getRoleId() { return roleId; }
        public void setRoleId(UUID v) { roleId = v; }
    }

    public static class StatusRequest {
        @NotNull private Boolean active;
        public Boolean getActive() { return active; }
        public void setActive(Boolean v) { active = v; }
    }

    public static class ResetPasswordRequest {
        @NotBlank @Size(min = 8, max = 100) private String newPassword;
        public String getNewPassword() { return newPassword; }
        public void setNewPassword(String v) { newPassword = v; }
    }

    public static class LoginRequest {
        @NotBlank @Email private String email;
        @NotBlank private String password;
        public String getEmail() { return email; }
        public void setEmail(String v) { email = v; }
        public String getPassword() { return password; }
        public void setPassword(String v) { password = v; }
    }

    public static class LoginResponse {
        private String accessToken;
        private String tokenType;
        private long expiresInSeconds;
        private AdminResponse profile;

        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String v) { accessToken = v; }
        public String getTokenType() { return tokenType; }
        public void setTokenType(String v) { tokenType = v; }
        public long getExpiresInSeconds() { return expiresInSeconds; }
        public void setExpiresInSeconds(long v) { expiresInSeconds = v; }
        public AdminResponse getProfile() { return profile; }
        public void setProfile(AdminResponse v) { profile = v; }
    }

    public static class AdminResponse {
        private UUID id, createdBy, roleId;
        private String fullName, email, phone, roleName, lastLoginIp;
        private boolean active;
        private List<String> effectivePermissions; // resolved live from role_permissions, read-only
        private LocalDateTime lastLoginAt, createdAt, updatedAt, deletedAt;

        public UUID getId() { return id; }
        public void setId(UUID v) { id = v; }
        public UUID getCreatedBy() { return createdBy; }
        public void setCreatedBy(UUID v) { createdBy = v; }
        public UUID getRoleId() { return roleId; }
        public void setRoleId(UUID v) { roleId = v; }
        public String getFullName() { return fullName; }
        public void setFullName(String v) { fullName = v; }
        public String getEmail() { return email; }
        public void setEmail(String v) { email = v; }
        public String getPhone() { return phone; }
        public void setPhone(String v) { phone = v; }
        public String getRoleName() { return roleName; }
        public void setRoleName(String v) { roleName = v; }
        public String getLastLoginIp() { return lastLoginIp; }
        public void setLastLoginIp(String v) { lastLoginIp = v; }
        public boolean isActive() { return active; }
        public void setActive(boolean v) { active = v; }
        public List<String> getEffectivePermissions() { return effectivePermissions; }
        public void setEffectivePermissions(List<String> v) { effectivePermissions = v; }
        public LocalDateTime getLastLoginAt() { return lastLoginAt; }
        public void setLastLoginAt(LocalDateTime v) { lastLoginAt = v; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime v) { createdAt = v; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime v) { updatedAt = v; }
        public LocalDateTime getDeletedAt() { return deletedAt; }
        public void setDeletedAt(LocalDateTime v) { deletedAt = v; }
    }

    public static class AdminPageResponse {
        private List<AdminResponse> content;
        private int page, size, totalPages;
        private long totalElements;

        public List<AdminResponse> getContent() { return content; }
        public void setContent(List<AdminResponse> v) { content = v; }
        public int getPage() { return page; }
        public void setPage(int v) { page = v; }
        public int getSize() { return size; }
        public void setSize(int v) { size = v; }
        public int getTotalPages() { return totalPages; }
        public void setTotalPages(int v) { totalPages = v; }
        public long getTotalElements() { return totalElements; }
        public void setTotalElements(long v) { totalElements = v; }
    }
}
