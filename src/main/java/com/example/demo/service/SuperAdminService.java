

package com.example.demo.service;

import com.example.demo.dto.SuperAdminDto;
import com.example.demo.entity.*;
import com.example.demo.repository.*;
import com.example.demo.util.*;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles everything a Super Admin does EXCEPT Admin CRUD/login, which lives
 * in the separate AdminDto / AdminService / AdminController.
 *
 * Covers: auth/profile/dashboard, audit log, and full CRUD for Roles,
 * Modules, and Permissions.
 *
 * There is intentionally NO createSuperAdmin(...) — exactly one Super Admin
 * exists, seeded at startup (see DataInitializer). There is also no direct
 * "assign permissions to Admin" method — permissions flow only through Roles
 * (edit a Role's permission set, or reassign an Admin's Role via AdminService).
 */
@Service
public class SuperAdminService {

    private final SuperAdminRepository superAdminRepository;
    private final AdminRepository adminRepository;
    private final RoleRepository roleRepository;
    private final ModuleRepository moduleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordUtil passwordUtil;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    public SuperAdminService(
            SuperAdminRepository superAdminRepository,
            AdminRepository adminRepository,
            RoleRepository roleRepository,
            ModuleRepository moduleRepository,
            PermissionRepository permissionRepository,
            RolePermissionRepository rolePermissionRepository,
            AuditLogRepository auditLogRepository,
            PasswordUtil passwordUtil,
            JwtUtil jwtUtil,
            ObjectMapper objectMapper) {

        this.superAdminRepository = superAdminRepository;
        this.adminRepository = adminRepository;
        this.roleRepository = roleRepository;
        this.moduleRepository = moduleRepository;
        this.permissionRepository = permissionRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.auditLogRepository = auditLogRepository;
        this.passwordUtil = passwordUtil;
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
    }

    // ═══════════════════════════════════════════
    //  AUTH / PROFILE
    // ═══════════════════════════════════════════

    @Transactional
    public SuperAdminDto.LoginResponse login(SuperAdminDto.LoginRequest request, String ip) {

        SuperAdminEntity admin = superAdminRepository
                .findByEmailIgnoreCaseAndDeletedAtIsNull(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!admin.isActive()) {
            throw new UnauthorizedException("Super Admin account is inactive");
        }
        if (!passwordUtil.matches(request.getPassword(), admin.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        admin.setLastLoginAt(LocalDateTime.now());
        admin.setLastLoginIp(ip);
        superAdminRepository.save(admin);

        String access = jwtUtil.generateAccessToken(admin.getId(), "SUPER_ADMIN", admin.getEmail());
        return buildLoginResponse(access, admin);
    }

    public SuperAdminDto.LogoutResponse logout(String token, SuperAdminUtil util) {
        util.blacklistToken(token);
        return new SuperAdminDto.LogoutResponse(true);
    }

    @Transactional(readOnly = true)
    public SuperAdminDto.SuperAdminProfileResponse profile(UUID id) {
        return toProfileResponse(findSuperAdmin(id));
    }

    public SuperAdminDto.LoginResponse refreshToken(String token, SuperAdminUtil util) {
        try {
            if (util.isBlacklisted(jwtUtil.getTokenId(token))) {
                throw new UnauthorizedException("Refresh token is invalid or expired");
            }

            UUID id = jwtUtil.getUserId(token);
            String tokenType = jwtUtil.getTokenType(token);

            if (!"REFRESH".equals(tokenType) && !"ACCESS".equals(tokenType)) {
                throw new UnauthorizedException("Refresh token is invalid or expired");
            }

            SuperAdminEntity admin = findSuperAdmin(id);
            if (!admin.isActive()) {
                throw new UnauthorizedException("Super Admin account is inactive");
            }

            String access = jwtUtil.generateAccessToken(admin.getId(), "SUPER_ADMIN", admin.getEmail());
            return buildLoginResponse(access, admin);

        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            throw new UnauthorizedException("Refresh token is invalid or expired");
        }
    }

    // ═══════════════════════════════════════════
    //  DASHBOARD
    // ═══════════════════════════════════════════

    @Transactional(readOnly = true)
    public SuperAdminDto.DashboardSummaryResponse dashboardSummary() {

        SuperAdminDto.DashboardSummaryResponse response = new SuperAdminDto.DashboardSummaryResponse();

        response.setTotalAdmins(adminRepository.countByDeletedAtIsNull());
        response.setActiveAdmins(adminRepository.countByDeletedAtIsNullAndActive(true));
        response.setInactiveAdmins(adminRepository.countByDeletedAtIsNullAndActive(false));

        Map<String, Long> distribution = new LinkedHashMap<>();
        adminRepository.findAll((root, query, cb) -> cb.isNull(root.get("deletedAt")))
                .forEach(admin -> distribution.merge(admin.getRole().getRoleName(), 1L, Long::sum));

        response.setRoleDistribution(distribution);
        return response;
    }

    @Transactional(readOnly = true)
    public SuperAdminDto.RecentActivityResponse recentActivity(int limit) {

        int safeLimit = Math.min(Math.max(limit, 1), 100);
        Page<AuditLogEntity> page = auditLogRepository.findAll(
                PageRequest.of(0, safeLimit, Sort.by(Sort.Direction.DESC, "createdAt")));

        SuperAdminDto.RecentActivityResponse response = new SuperAdminDto.RecentActivityResponse();
        response.setActivities(page.getContent().stream().map(this::toAuditResponse).toList());
        return response;
    }

    // ═══════════════════════════════════════════
    //  AUDIT LOG
    // ═══════════════════════════════════════════

    @Transactional(readOnly = true)
    public SuperAdminDto.AuditLogPageResponse auditLogs(
            UUID adminId, String module, String action,
            LocalDate dateFrom, LocalDate dateTo, int page, int size) {

        Specification<AuditLogEntity> specification = buildAuditSpecification(adminId, module, action, dateFrom, dateTo);

        Page<AuditLogEntity> result = auditLogRepository.findAll(
                specification,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                        Sort.by(Sort.Direction.DESC, "createdAt")));

        return auditPage(result);
    }

    @Transactional(readOnly = true)
    public SuperAdminDto.AuditLogResponse auditLog(UUID id) {
        AuditLogEntity entity = auditLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Audit log not found"));
        return toAuditResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<SuperAdminDto.AuditLogResponse> exportAuditLogs(
            UUID adminId, String module, String action, LocalDate dateFrom, LocalDate dateTo) {

        return auditLogRepository
                .findAll(buildAuditSpecification(adminId, module, action, dateFrom, dateTo),
                        Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream().map(this::toAuditResponse).toList();
    }

    private Specification<AuditLogEntity> buildAuditSpecification(
            UUID adminId, String module, String action, LocalDate dateFrom, LocalDate dateTo) {

        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (adminId != null) predicates.add(cb.equal(root.get("adminId"), adminId));
            if (module != null && !module.isBlank())
                predicates.add(cb.equal(cb.lower(root.get("module")), module.toLowerCase()));
            if (action != null && !action.isBlank())
                predicates.add(cb.equal(cb.lower(root.get("action")), action.toLowerCase()));
            if (dateFrom != null) predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), dateFrom.atStartOfDay()));
            if (dateTo != null) predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), dateTo.atTime(LocalTime.MAX)));

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    /**
     * Shared audit writer — called by this service AND by AdminService for
     * Admin-management actions (the actor there is always a Super Admin).
     */
    public void writeAudit(
            UUID actorId, String actorName, String actorRole, String action,
            String module, String target, Object before, Object after, String ip) {

        AuditLogEntity log = new AuditLogEntity();
        log.setAdminId(actorId);
        log.setAdminName(actorName);
        log.setAdminType("SUPER_ADMIN");
        log.setAdminRole(actorRole);
        log.setAction(action);
        log.setModule(module);
        log.setTargetLabel(target);
        log.setBeforeState(toJsonNode(before));
        log.setAfterState(toJsonNode(after));
        log.setIpAddress(ip);

        auditLogRepository.save(log);
    }

    // ═══════════════════════════════════════════
    //  ROLE
    // ═══════════════════════════════════════════

    @Transactional
    public SuperAdminDto.RoleResponse createRole(SuperAdminDto.CreateRoleRequest request, UUID actorId, String ip) {

        if (roleRepository.findByRoleNameIgnoreCaseAndDeletedAtIsNull(request.getRoleName()).isPresent()) {
            throw new BadRequestException("Role already exists: " + request.getRoleName());
        }

        SuperAdminEntity actor = findSuperAdmin(actorId);

        RoleEntity role = new RoleEntity();
        role.setRoleName(request.getRoleName().trim());
        role.setDescription(request.getDescription());
        role.setActive(true);
        role = roleRepository.save(role);

        attachPermissions(role, request.getPermissionIds());

        writeAudit(actorId, actor.getFullName(), "SUPER_ADMIN",
                "CREATE", "ROLE_MANAGEMENT", role.getRoleName(), null, roleState(role), ip);

        return toRoleResponse(role);
    }

    @Transactional(readOnly = true)
    public List<SuperAdminDto.RoleResponse> listRoles() {
        return roleRepository.findByDeletedAtIsNullOrderByRoleNameAsc()
                .stream().map(this::toRoleResponse).toList();
    }

    @Transactional(readOnly = true)
    public SuperAdminDto.RoleResponse getRole(UUID id) {
        return toRoleResponse(findRole(id));
    }

    @Transactional
    public SuperAdminDto.RoleResponse updateRole(UUID id, SuperAdminDto.UpdateRoleRequest request, UUID actorId, String ip) {

        RoleEntity role = findRole(id);
        SuperAdminEntity actor = findSuperAdmin(actorId);
        Map<String, Object> before = roleState(role);

        role.setDescription(request.getDescription());
        role = roleRepository.save(role);

        writeAudit(actorId, actor.getFullName(), "SUPER_ADMIN",
                "UPDATE", "ROLE_MANAGEMENT", role.getRoleName(), before, roleState(role), ip);

        return toRoleResponse(role);
    }

    /**
     * Full replace of a Role's permission set. Every Sub-Admin currently
     * assigned to this Role gets the new permission set IMMEDIATELY and
     * automatically — permissions are always resolved live through
     * role_permissions on every request.
     */
    @Transactional
    public SuperAdminDto.RoleResponse updateRolePermissions(UUID id, SuperAdminDto.UpdateRolePermissionsRequest request, UUID actorId, String ip) {

        RoleEntity role = findRole(id);
        SuperAdminEntity actor = findSuperAdmin(actorId);

        Map<String, Object> before = roleState(role);

        rolePermissionRepository.deleteByRole_Id(role.getId());
        attachPermissions(role, request.getPermissionIds());

        writeAudit(actorId, actor.getFullName(), "SUPER_ADMIN",
                "UPDATE_PERMISSIONS", "ROLE_MANAGEMENT", role.getRoleName(), before, roleState(role), ip);

        return toRoleResponse(role);
    }

    @Transactional
    public SuperAdminDto.RoleResponse statusRole(UUID id, boolean active, UUID actorId, String ip) {

        RoleEntity role = findRole(id);
        SuperAdminEntity actor = findSuperAdmin(actorId);
        boolean before = role.isActive();

        role.setActive(active);
        role = roleRepository.save(role);

        writeAudit(actorId, actor.getFullName(), "SUPER_ADMIN",
                "STATUS_CHANGE", "ROLE_MANAGEMENT", role.getRoleName(),
                Map.of("active", before), Map.of("active", active), ip);

        return toRoleResponse(role);
    }

    @Transactional
    public void deleteRole(UUID id, UUID actorId, String ip) {

        RoleEntity role = findRole(id);
        SuperAdminEntity actor = findSuperAdmin(actorId);

        if (!adminRepository.findByDeletedAtIsNullAndRole_Id(role.getId()).isEmpty()) {
            throw new BadRequestException("Role is assigned to an Admin and cannot be deleted");
        }

        rolePermissionRepository.deleteByRole_Id(role.getId());

        role.setActive(false);
        role.setDeletedAt(LocalDateTime.now());
        roleRepository.save(role);

        writeAudit(actorId, actor.getFullName(), "SUPER_ADMIN",
                "DELETE", "ROLE_MANAGEMENT", role.getRoleName(),
                Map.of("active", true), Map.of("active", false, "deleted", true), ip);
    }

    private void attachPermissions(RoleEntity role, List<UUID> permissionIds) {
        if (permissionIds == null) return;
        for (UUID permissionId : permissionIds) {
            PermissionEntity permission = permissionRepository.findByIdAndDeletedAtIsNull(permissionId)
                    .orElseThrow(() -> new BadRequestException("Permission not found: " + permissionId));

            if (rolePermissionRepository.findByRole_IdAndPermission_Id(role.getId(), permission.getId()).isPresent()) {
                continue;
            }

            RolePermissionEntity mapping = new RolePermissionEntity();
            mapping.setRole(role);
            mapping.setPermission(permission);
            rolePermissionRepository.save(mapping);
        }
    }

    private RoleEntity findRole(UUID id) {
        return roleRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + id));
    }

    private List<PermissionEntity> permissionsOf(UUID roleId) {
        return rolePermissionRepository.findByRole_Id(roleId).stream()
                .map(RolePermissionEntity::getPermission)
                .filter(p -> p.getDeletedAt() == null)
                .toList();
    }

    private Map<String, Object> roleState(RoleEntity role) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("id", role.getId());
        state.put("roleName", role.getRoleName());
        state.put("active", role.isActive());
        state.put("permissionCodes", permissionsOf(role.getId()).stream()
                .map(PermissionEntity::getCode).collect(Collectors.toList()));
        return state;
    }

    private SuperAdminDto.RoleResponse toRoleResponse(RoleEntity role) {

        SuperAdminDto.RoleResponse r = new SuperAdminDto.RoleResponse();
        r.setId(role.getId());
        r.setRoleName(role.getRoleName());
        r.setDescription(role.getDescription());
        r.setActive(role.isActive());
        r.setCreatedAt(role.getCreatedAt());
        r.setUpdatedAt(role.getUpdatedAt());
        r.setPermissions(permissionsOf(role.getId()).stream().map(this::toPermissionResponse).toList());
        return r;
    }

    // ═══════════════════════════════════════════
    //  MODULE
    // ═══════════════════════════════════════════

    @Transactional
    public SuperAdminDto.ModuleResponse createModule(SuperAdminDto.CreateModuleRequest request, UUID actorId, String ip) {

        if (moduleRepository.findByCodeIgnoreCaseAndDeletedAtIsNull(request.getCode()).isPresent()) {
            throw new BadRequestException("Module code already exists: " + request.getCode());
        }

        SuperAdminEntity actor = findSuperAdmin(actorId);

        ModuleEntity module = new ModuleEntity();
        module.setName(request.getName().trim());
        module.setCode(request.getCode().trim().toUpperCase());
        module.setDescription(request.getDescription());
        module.setActive(true);

        module = moduleRepository.save(module);

        writeAudit(actorId, actor.getFullName(), "SUPER_ADMIN",
                "CREATE", "MODULE_MANAGEMENT", module.getCode(), null, moduleState(module), ip);

        return toModuleResponse(module);
    }

    @Transactional(readOnly = true)
    public List<SuperAdminDto.ModuleResponse> listModules() {
        return moduleRepository.findByDeletedAtIsNullOrderByNameAsc()
                .stream().map(this::toModuleResponse).toList();
    }

    @Transactional(readOnly = true)
    public SuperAdminDto.ModuleResponse getModule(UUID id) {
        return toModuleResponse(findModule(id));
    }

    @Transactional
    public SuperAdminDto.ModuleResponse updateModule(UUID id, SuperAdminDto.UpdateModuleRequest request, UUID actorId, String ip) {

        ModuleEntity module = findModule(id);
        SuperAdminEntity actor = findSuperAdmin(actorId);
        Map<String, Object> before = moduleState(module);

        module.setName(request.getName().trim());
        module.setDescription(request.getDescription());
        module = moduleRepository.save(module);

        writeAudit(actorId, actor.getFullName(), "SUPER_ADMIN",
                "UPDATE", "MODULE_MANAGEMENT", module.getCode(), before, moduleState(module), ip);

        return toModuleResponse(module);
    }

    @Transactional
    public SuperAdminDto.ModuleResponse statusModule(UUID id, boolean active, UUID actorId, String ip) {

        ModuleEntity module = findModule(id);
        SuperAdminEntity actor = findSuperAdmin(actorId);
        boolean before = module.isActive();

        module.setActive(active);
        module = moduleRepository.save(module);

        // Deactivating a module immediately denies access to every permission
        // under it — checked live on every request, no propagation needed.
        writeAudit(actorId, actor.getFullName(), "SUPER_ADMIN",
                "STATUS_CHANGE", "MODULE_MANAGEMENT", module.getCode(),
                Map.of("active", before), Map.of("active", active), ip);

        return toModuleResponse(module);
    }

    @Transactional
    public void deleteModule(UUID id, UUID actorId, String ip) {

        ModuleEntity module = findModule(id);
        SuperAdminEntity actor = findSuperAdmin(actorId);

        if (permissionRepository.existsByModule_IdAndDeletedAtIsNull(id)) {
            throw new BadRequestException("Module has active permissions and cannot be deleted");
        }

        module.setActive(false);
        module.setDeletedAt(LocalDateTime.now());
        moduleRepository.save(module);

        writeAudit(actorId, actor.getFullName(), "SUPER_ADMIN",
                "DELETE", "MODULE_MANAGEMENT", module.getCode(),
                Map.of("active", true), Map.of("active", false, "deleted", true), ip);
    }

    private ModuleEntity findModule(UUID id) {
        return moduleRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Module not found: " + id));
    }

    private Map<String, Object> moduleState(ModuleEntity m) {
        return Map.of("id", m.getId(), "name", m.getName(), "code", m.getCode(), "active", m.isActive());
    }

    private SuperAdminDto.ModuleResponse toModuleResponse(ModuleEntity m) {
        SuperAdminDto.ModuleResponse r = new SuperAdminDto.ModuleResponse();
        r.setId(m.getId());
        r.setName(m.getName());
        r.setCode(m.getCode());
        r.setDescription(m.getDescription());
        r.setActive(m.isActive());
        r.setCreatedAt(m.getCreatedAt());
        r.setUpdatedAt(m.getUpdatedAt());
        return r;
    }

    // ═══════════════════════════════════════════
    //  PERMISSION
    // ═══════════════════════════════════════════

    @Transactional
    public SuperAdminDto.PermissionResponse createPermission(SuperAdminDto.CreatePermissionRequest request, UUID actorId, String ip) {

        if (permissionRepository.findByCodeIgnoreCaseAndDeletedAtIsNull(request.getCode()).isPresent()) {
            throw new BadRequestException("Permission code already exists: " + request.getCode());
        }

        ModuleEntity module = moduleRepository.findByIdAndDeletedAtIsNull(request.getModuleId())
                .orElseThrow(() -> new BadRequestException("Module not found: " + request.getModuleId()));

        SuperAdminEntity actor = findSuperAdmin(actorId);

        PermissionEntity permission = new PermissionEntity();
        permission.setModule(module);
        permission.setCode(request.getCode().trim().toUpperCase());
        permission.setAction(request.getAction().trim().toUpperCase());
        permission.setDescription(request.getDescription());
        permission.setActive(true);

        permission = permissionRepository.save(permission);

        writeAudit(actorId, actor.getFullName(), "SUPER_ADMIN",
                "CREATE", "PERMISSION_MANAGEMENT", permission.getCode(), null, permissionState(permission), ip);

        return toPermissionResponse(permission);
    }

    @Transactional(readOnly = true)
    public List<SuperAdminDto.PermissionResponse> listPermissions(UUID moduleId) {
        List<PermissionEntity> entities = moduleId == null
                ? permissionRepository.findByDeletedAtIsNullOrderByCodeAsc()
                : permissionRepository.findByModule_IdAndDeletedAtIsNullOrderByCodeAsc(moduleId);
        return entities.stream().map(this::toPermissionResponse).toList();
    }

    @Transactional(readOnly = true)
    public SuperAdminDto.PermissionResponse getPermission(UUID id) {
        return toPermissionResponse(findPermission(id));
    }

    @Transactional
    public SuperAdminDto.PermissionResponse updatePermission(UUID id, SuperAdminDto.UpdatePermissionRequest request, UUID actorId, String ip) {

        PermissionEntity permission = findPermission(id);
        SuperAdminEntity actor = findSuperAdmin(actorId);
        Map<String, Object> before = permissionState(permission);

        permission.setAction(request.getAction().trim().toUpperCase());
        permission.setDescription(request.getDescription());
        permission = permissionRepository.save(permission);

        writeAudit(actorId, actor.getFullName(), "SUPER_ADMIN",
                "UPDATE", "PERMISSION_MANAGEMENT", permission.getCode(), before, permissionState(permission), ip);

        return toPermissionResponse(permission);
    }

    @Transactional
    public SuperAdminDto.PermissionResponse statusPermission(UUID id, boolean active, UUID actorId, String ip) {

        PermissionEntity permission = findPermission(id);
        SuperAdminEntity actor = findSuperAdmin(actorId);
        boolean before = permission.isActive();

        permission.setActive(active);
        permission = permissionRepository.save(permission);

        // No propagation step needed: permission.isActive() is checked live
        // on every request, so this takes effect immediately.
        writeAudit(actorId, actor.getFullName(), "SUPER_ADMIN",
                "STATUS_CHANGE", "PERMISSION_MANAGEMENT", permission.getCode(),
                Map.of("active", before), Map.of("active", active), ip);

        return toPermissionResponse(permission);
    }

    @Transactional
    public void deletePermission(UUID id, UUID actorId, String ip) {

        PermissionEntity permission = findPermission(id);
        SuperAdminEntity actor = findSuperAdmin(actorId);

        rolePermissionRepository.deleteByPermission_Id(id);

        permission.setActive(false);
        permission.setDeletedAt(LocalDateTime.now());
        permissionRepository.save(permission);

        writeAudit(actorId, actor.getFullName(), "SUPER_ADMIN",
                "DELETE", "PERMISSION_MANAGEMENT", permission.getCode(),
                Map.of("active", true), Map.of("active", false, "deleted", true), ip);
    }

    private PermissionEntity findPermission(UUID id) {
        return permissionRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission not found: " + id));
    }

    private Map<String, Object> permissionState(PermissionEntity p) {
        return Map.of("id", p.getId(), "code", p.getCode(), "moduleId", p.getModule().getId(), "active", p.isActive());
    }

    private SuperAdminDto.PermissionResponse toPermissionResponse(PermissionEntity p) {
        SuperAdminDto.PermissionResponse r = new SuperAdminDto.PermissionResponse();
        r.setId(p.getId());
        r.setModuleId(p.getModule().getId());
        r.setModuleName(p.getModule().getName());
        r.setCode(p.getCode());
        r.setAction(p.getAction());
        r.setDescription(p.getDescription());
        r.setActive(p.isActive());
        r.setCreatedAt(p.getCreatedAt());
        r.setUpdatedAt(p.getUpdatedAt());
        return r;
    }

    // ═══════════════════════════════════════════
    //  SHARED HELPERS
    // ═══════════════════════════════════════════

    SuperAdminEntity findSuperAdmin(UUID id) {
        return superAdminRepository.findById(id)
                .filter(x -> x.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Super Admin not found"));
    }

    private String toJsonNode(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to create audit JSON", e);
        }
    }

    private SuperAdminDto.SuperAdminProfileResponse toProfileResponse(SuperAdminEntity entity) {
        SuperAdminDto.SuperAdminProfileResponse r = new SuperAdminDto.SuperAdminProfileResponse();
        r.setId(entity.getId());
        r.setFullName(entity.getFullName());
        r.setEmail(entity.getEmail());
        r.setPhone(entity.getPhone());
        r.setActive(entity.isActive());
        r.setLastLoginAt(entity.getLastLoginAt());
        return r;
    }

    private SuperAdminDto.AuditLogPageResponse auditPage(Page<AuditLogEntity> page) {
        SuperAdminDto.AuditLogPageResponse r = new SuperAdminDto.AuditLogPageResponse();
        r.setContent(page.getContent().stream().map(this::toAuditResponse).toList());
        r.setPage(page.getNumber());
        r.setSize(page.getSize());
        r.setTotalPages(page.getTotalPages());
        r.setTotalElements(page.getTotalElements());
        return r;
    }

    private SuperAdminDto.AuditLogResponse toAuditResponse(AuditLogEntity e) {
        SuperAdminDto.AuditLogResponse r = new SuperAdminDto.AuditLogResponse();
        r.setId(e.getId());
        r.setAdminId(e.getAdminId());
        r.setAdminName(e.getAdminName());
        r.setAdminType(e.getAdminType());
        r.setAdminRole(e.getAdminRole());
        r.setAction(e.getAction());
        r.setModule(e.getModule());
        r.setTargetLabel(e.getTargetLabel());
        r.setIpAddress(e.getIpAddress());
        r.setCreatedAt(e.getCreatedAt());
        r.setBeforeState(fromJson(e.getBeforeState()));
        r.setAfterState(fromJson(e.getAfterState()));
        return r;
    }

    private Map<String, Object> fromJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return null;
        }
    }

    private SuperAdminDto.LoginResponse buildLoginResponse(String access, SuperAdminEntity admin) {
        SuperAdminDto.LoginResponse response = new SuperAdminDto.LoginResponse();
        response.setAccessToken(access);
        response.setTokenType("Bearer");
        response.setExpiresInSeconds(jwtUtil.getAccessExpirationSeconds());
        response.setProfile(toProfileResponse(admin));
        return response;
    }

    // ═══════════════════════════════════════════
    //  EXCEPTIONS
    // ═══════════════════════════════════════════

    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) { super(message); }
    }

    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String message) { super(message); }
    }

    public static class ForbiddenException extends RuntimeException {
        public ForbiddenException(String message) { super(message); }
    }

    public static class BadRequestException extends RuntimeException {
        public BadRequestException(String message) { super(message); }
    }
}
