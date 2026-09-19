


package com.example.demo.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.*;

// NOTE: Admin login/CRUD DTOs live in AdminDto (separate file, separate
// service/controller). Role/Module/Permission DTOs live here since they are
// managed exclusively by the Super Admin. There is no CreateSuperAdminRequest
// / SuperAdminResponse — exactly one Super Admin exists, seeded at startup
// (see DataInitializer). No API creates another one.
public final class SuperAdminDto {

    private SuperAdminDto() {}

    // ═══════════════════════════════════════════
    //  SUPER ADMIN — AUTH / PROFILE / DASHBOARD
    // ═══════════════════════════════════════════

    public static class LoginRequest {
        @NotBlank @Email
        private String email;
        @NotBlank
        private String password;

        public String getEmail()            { return email; }
        public void   setEmail(String v)    { email = v; }
        public String getPassword()         { return password; }
        public void   setPassword(String v) { password = v; }
    }

    public static class LoginResponse {
        private String accessToken, tokenType;
        private long expiresInSeconds;
        private SuperAdminProfileResponse profile;

        public String getAccessToken()                        { return accessToken; }
        public void   setAccessToken(String v)                { accessToken = v; }
        public String getTokenType()                          { return tokenType; }
        public void   setTokenType(String v)                  { tokenType = v; }
        public long   getExpiresInSeconds()                   { return expiresInSeconds; }
        public void   setExpiresInSeconds(long v)             { expiresInSeconds = v; }
        public SuperAdminProfileResponse getProfile()         { return profile; }
        public void   setProfile(SuperAdminProfileResponse v) { profile = v; }
    }

    public static class RefreshTokenRequest {
        @NotBlank
        private String token;
        public String getToken()         { return token; }
        public void   setToken(String v) { token = v; }
    }

    public static class LogoutResponse {
        private boolean loggedOut;
        public LogoutResponse() {}
        public LogoutResponse(boolean v)      { loggedOut = v; }
        public boolean isLoggedOut()          { return loggedOut; }
        public void    setLoggedOut(boolean v){ loggedOut = v; }
    }

    public static class SuperAdminProfileResponse {
        private UUID id;
        private String fullName, email, phone;
        private boolean active;
        private LocalDateTime lastLoginAt;

        public UUID   getId()                         { return id; }
        public void   setId(UUID v)                   { id = v; }
        public String getFullName()                   { return fullName; }
        public void   setFullName(String v)           { fullName = v; }
        public String getEmail()                      { return email; }
        public void   setEmail(String v)              { email = v; }
        public String getPhone()                      { return phone; }
        public void   setPhone(String v)              { phone = v; }
        public boolean isActive()                     { return active; }
        public void   setActive(boolean v)            { active = v; }
        public LocalDateTime getLastLoginAt()         { return lastLoginAt; }
        public void   setLastLoginAt(LocalDateTime v) { lastLoginAt = v; }
    }

    public static class DashboardSummaryResponse {
        private long totalAdmins, activeAdmins, inactiveAdmins;
        private Map<String, Long> roleDistribution;

        public long getTotalAdmins()                         { return totalAdmins; }
        public void setTotalAdmins(long v)                   { totalAdmins = v; }
        public long getActiveAdmins()                        { return activeAdmins; }
        public void setActiveAdmins(long v)                  { activeAdmins = v; }
        public long getInactiveAdmins()                      { return inactiveAdmins; }
        public void setInactiveAdmins(long v)                { inactiveAdmins = v; }
        public Map<String, Long> getRoleDistribution()       { return roleDistribution; }
        public void setRoleDistribution(Map<String, Long> v) { roleDistribution = v; }
    }

    public static class RecentActivityResponse {
        private List<AuditLogResponse> activities;
        public List<AuditLogResponse> getActivities()       { return activities; }
        public void setActivities(List<AuditLogResponse> v) { activities = v; }
    }

    public static class AuditLogResponse {
        private UUID id, adminId;
        private String adminName, adminType, adminRole, action, module, targetLabel, ipAddress;
        private Map<String, Object> beforeState, afterState;
        private LocalDateTime createdAt;

        public UUID   getId()                               { return id; }
        public void   setId(UUID v)                         { id = v; }
        public UUID   getAdminId()                          { return adminId; }
        public void   setAdminId(UUID v)                    { adminId = v; }
        public String getAdminName()                        { return adminName; }
        public void   setAdminName(String v)                { adminName = v; }
        public String getAdminType()                        { return adminType; }
        public void   setAdminType(String v)                { adminType = v; }
        public String getAdminRole()                        { return adminRole; }
        public void   setAdminRole(String v)                { adminRole = v; }
        public String getAction()                           { return action; }
        public void   setAction(String v)                   { action = v; }
        public String getModule()                           { return module; }
        public void   setModule(String v)                   { module = v; }
        public String getTargetLabel()                      { return targetLabel; }
        public void   setTargetLabel(String v)              { targetLabel = v; }
        public String getIpAddress()                        { return ipAddress; }
        public void   setIpAddress(String v)                { ipAddress = v; }
        public Map<String, Object> getBeforeState()         { return beforeState; }
        public void   setBeforeState(Map<String, Object> v) { beforeState = v; }
        public Map<String, Object> getAfterState()          { return afterState; }
        public void   setAfterState(Map<String, Object> v)  { afterState = v; }
        public LocalDateTime getCreatedAt()                 { return createdAt; }
        public void   setCreatedAt(LocalDateTime v)         { createdAt = v; }
    }

    public static class AuditLogPageResponse {
        private List<AuditLogResponse> content;
        private int page, size, totalPages;
        private long totalElements;

        public List<AuditLogResponse> getContent()       { return content; }
        public void setContent(List<AuditLogResponse> v) { content = v; }
        public int  getPage()                            { return page; }
        public void setPage(int v)                       { page = v; }
        public int  getSize()                            { return size; }
        public void setSize(int v)                       { size = v; }
        public int  getTotalPages()                      { return totalPages; }
        public void setTotalPages(int v)                 { totalPages = v; }
        public long getTotalElements()                   { return totalElements; }
        public void setTotalElements(long v)             { totalElements = v; }
    }

    // ═══════════════════════════════════════════
    //  ROLE
    // ═══════════════════════════════════════════

    public static class CreateRoleRequest {
        @NotBlank private String roleName;
        private String description;
        private List<UUID> permissionIds;

        public String     getRoleName()                  { return roleName; }
        public void       setRoleName(String v)          { roleName = v; }
        public String     getDescription()               { return description; }
        public void       setDescription(String v)       { description = v; }
        public List<UUID> getPermissionIds()             { return permissionIds; }
        public void       setPermissionIds(List<UUID> v) { permissionIds = v; }
    }

    public static class UpdateRoleRequest {
        private String description;
        public String getDescription()         { return description; }
        public void   setDescription(String v) { description = v; }
    }

    public static class UpdateRolePermissionsRequest {
        @NotNull private List<UUID> permissionIds;
        public List<UUID> getPermissionIds()             { return permissionIds; }
        public void       setPermissionIds(List<UUID> v) { permissionIds = v; }
    }

    public static class RoleStatusRequest {
        @NotNull private Boolean active;
        public Boolean getActive()          { return active; }
        public void    setActive(Boolean v) { active = v; }
    }

    public static class RoleResponse {
        private UUID id;
        private String roleName, description;
        private boolean active;
        private List<PermissionResponse> permissions;
        private LocalDateTime createdAt, updatedAt;

        public UUID   getId()                                  { return id; }
        public void   setId(UUID v)                            { id = v; }
        public String getRoleName()                            { return roleName; }
        public void   setRoleName(String v)                    { roleName = v; }
        public String getDescription()                         { return description; }
        public void   setDescription(String v)                 { description = v; }
        public boolean isActive()                              { return active; }
        public void   setActive(boolean v)                     { active = v; }
        public List<PermissionResponse> getPermissions()       { return permissions; }
        public void   setPermissions(List<PermissionResponse> v) { permissions = v; }
        public LocalDateTime getCreatedAt()                    { return createdAt; }
        public void   setCreatedAt(LocalDateTime v)            { createdAt = v; }
        public LocalDateTime getUpdatedAt()                    { return updatedAt; }
        public void   setUpdatedAt(LocalDateTime v)            { updatedAt = v; }
    }

    // ═══════════════════════════════════════════
    //  MODULE
    // ═══════════════════════════════════════════

    public static class CreateModuleRequest {
        @NotBlank private String name, code;
        private String description;

        public String getName()                { return name; }
        public void   setName(String v)        { name = v; }
        public String getCode()                { return code; }
        public void   setCode(String v)        { code = v; }
        public String getDescription()         { return description; }
        public void   setDescription(String v) { description = v; }
    }

    public static class UpdateModuleRequest {
        @NotBlank private String name;
        private String description;

        public String getName()                { return name; }
        public void   setName(String v)        { name = v; }
        public String getDescription()         { return description; }
        public void   setDescription(String v) { description = v; }
    }

    public static class ModuleStatusRequest {
        @NotNull private Boolean active;
        public Boolean getActive()          { return active; }
        public void    setActive(Boolean v) { active = v; }
    }

    public static class ModuleResponse {
        private UUID id;
        private String name, code, description;
        private boolean active;
        private LocalDateTime createdAt, updatedAt;

        public UUID   getId()                         { return id; }
        public void   setId(UUID v)                   { id = v; }
        public String getName()                       { return name; }
        public void   setName(String v)               { name = v; }
        public String getCode()                       { return code; }
        public void   setCode(String v)               { code = v; }
        public String getDescription()                { return description; }
        public void   setDescription(String v)        { description = v; }
        public boolean isActive()                     { return active; }
        public void   setActive(boolean v)            { active = v; }
        public LocalDateTime getCreatedAt()           { return createdAt; }
        public void   setCreatedAt(LocalDateTime v)   { createdAt = v; }
        public LocalDateTime getUpdatedAt()           { return updatedAt; }
        public void   setUpdatedAt(LocalDateTime v)   { updatedAt = v; }
    }

    // ═══════════════════════════════════════════
    //  PERMISSION
    // ═══════════════════════════════════════════

    public static class CreatePermissionRequest {
        @NotNull  private UUID moduleId;
        @NotBlank private String code, action;
        private String description;

        public UUID   getModuleId()             { return moduleId; }
        public void   setModuleId(UUID v)       { moduleId = v; }
        public String getCode()                 { return code; }
        public void   setCode(String v)         { code = v; }
        public String getAction()               { return action; }
        public void   setAction(String v)       { action = v; }
        public String getDescription()          { return description; }
        public void   setDescription(String v)  { description = v; }
    }

    public static class UpdatePermissionRequest {
        @NotBlank private String action;
        private String description;

        public String getAction()               { return action; }
        public void   setAction(String v)       { action = v; }
        public String getDescription()          { return description; }
        public void   setDescription(String v)  { description = v; }
    }

    public static class PermissionStatusRequest {
        @NotNull private Boolean active;
        public Boolean getActive()          { return active; }
        public void    setActive(Boolean v) { active = v; }
    }

    public static class PermissionResponse {
        private UUID id, moduleId;
        private String moduleName, code, action, description;
        private boolean active;
        private LocalDateTime createdAt, updatedAt;

        public UUID   getId()                         { return id; }
        public void   setId(UUID v)                   { id = v; }
        public UUID   getModuleId()                   { return moduleId; }
        public void   setModuleId(UUID v)             { moduleId = v; }
        public String getModuleName()                 { return moduleName; }
        public void   setModuleName(String v)         { moduleName = v; }
        public String getCode()                       { return code; }
        public void   setCode(String v)               { code = v; }
        public String getAction()                     { return action; }
        public void   setAction(String v)             { action = v; }
        public String getDescription()                { return description; }
        public void   setDescription(String v)        { description = v; }
        public boolean isActive()                     { return active; }
        public void   setActive(boolean v)            { active = v; }
        public LocalDateTime getCreatedAt()           { return createdAt; }
        public void   setCreatedAt(LocalDateTime v)   { createdAt = v; }
        public LocalDateTime getUpdatedAt()           { return updatedAt; }
        public void   setUpdatedAt(LocalDateTime v)   { updatedAt = v; }
    }
}
