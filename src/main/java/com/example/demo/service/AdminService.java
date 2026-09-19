package com.example.demo.service;

import com.example.demo.dto.AdminDto;
import com.example.demo.entity.*;
import com.example.demo.repository.*;
import com.example.demo.util.JwtUtil;
import com.example.demo.util.PasswordUtil;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class AdminService {

    private final AdminRepository adminRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final SuperAdminRepository superAdminRepository;
    private final PasswordUtil passwordUtil;
    private final JwtUtil jwtUtil;
    private final SuperAdminService superAdminService;

    public AdminService(
            AdminRepository adminRepository,
            RoleRepository roleRepository,
            RolePermissionRepository rolePermissionRepository,
            SuperAdminRepository superAdminRepository,
            PasswordUtil passwordUtil,
            JwtUtil jwtUtil,
            SuperAdminService superAdminService) {

        this.adminRepository = adminRepository;
        this.roleRepository = roleRepository;
        this.rolePermissionRepository = rolePermissionRepository;
        this.superAdminRepository = superAdminRepository;
        this.passwordUtil = passwordUtil;
        this.jwtUtil = jwtUtil;
        this.superAdminService = superAdminService;
    }

    // ---------------- ADMIN LOGIN ----------------

    @Transactional
    public AdminDto.LoginResponse login(AdminDto.LoginRequest request, String ip) {

        AdminEntity admin = adminRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(request.getEmail())
                .orElseThrow(() -> new SuperAdminService.UnauthorizedException("Invalid email or password"));

        if (!admin.isActive()) {
            throw new SuperAdminService.UnauthorizedException("Admin account is inactive");
        }
        if (!passwordUtil.matches(request.getPassword(), admin.getPasswordHash())) {
            throw new SuperAdminService.UnauthorizedException("Invalid email or password");
        }

        admin.setLastLoginAt(LocalDateTime.now());
        admin.setLastLoginIp(ip);
        admin = adminRepository.save(admin);

        String access = jwtUtil.generateAccessToken(admin.getId(), "ADMIN", admin.getEmail());

        AdminDto.LoginResponse response = new AdminDto.LoginResponse();
        response.setAccessToken(access);
        response.setTokenType("Bearer");
        response.setExpiresInSeconds(jwtUtil.getAccessExpirationSeconds());
        response.setProfile(toResponse(admin));
        return response;
    }

    // ---------------- SUPER ADMIN: MANAGE SUB-ADMINS ----------------

    @Transactional
    public AdminDto.AdminResponse create(AdminDto.CreateAdminRequest request, UUID actorId, String ip) {

        if (adminRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(request.getEmail()).isPresent()) {
            throw new SuperAdminService.BadRequestException("Email already in use: " + request.getEmail());
        }

        RoleEntity role = findActiveRole(request.getRoleId());
        SuperAdminEntity creator = findSuperAdmin(actorId);

        AdminEntity admin = new AdminEntity();
        admin.setFullName(request.getFullName().trim());
        admin.setEmail(request.getEmail().trim().toLowerCase());
        admin.setPhone(request.getPhone());
        admin.setPasswordHash(passwordUtil.encode(request.getPassword()));
        admin.setRole(role);
        admin.setActive(true);
        admin.setCreatedBy(creator);

        admin = adminRepository.save(admin);

        superAdminService.writeAudit(actorId, creator.getFullName(), "SUPER_ADMIN",
                "CREATE", "ADMIN_MANAGEMENT", admin.getEmail(), null, safeState(admin), ip);

        return toResponse(admin);
    }

    @Transactional(readOnly = true)
    public AdminDto.AdminPageResponse list(UUID roleId, Boolean active, int page, int size) {

        Specification<AdminEntity> specification = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> p = new ArrayList<>();
            p.add(cb.isNull(root.get("deletedAt")));
            if (roleId != null) {
                p.add(cb.equal(root.get("role").get("id"), roleId));
            }
            if (active != null) {
                p.add(cb.equal(root.get("active"), active));
            }
            return cb.and(p.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<AdminEntity> result = adminRepository.findAll(
                specification,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                        Sort.by(Sort.Direction.DESC, "createdAt")));

        AdminDto.AdminPageResponse response = new AdminDto.AdminPageResponse();
        response.setContent(result.getContent().stream().map(this::toResponse).toList());
        response.setPage(result.getNumber());
        response.setSize(result.getSize());
        response.setTotalPages(result.getTotalPages());
        response.setTotalElements(result.getTotalElements());
        return response;
    }

    @Transactional(readOnly = true)
    public AdminDto.AdminResponse get(UUID id) {
        return toResponse(findAdmin(id));
    }

    @Transactional
    public AdminDto.AdminResponse update(UUID id, AdminDto.UpdateAdminRequest request, UUID actorId, String ip) {

        AdminEntity admin = findAdmin(id);
        SuperAdminEntity actor = findSuperAdmin(actorId);
        RoleEntity role = findActiveRole(request.getRoleId());
        Map<String, Object> before = safeState(admin);

        admin.setFullName(request.getFullName().trim());
        admin.setPhone(request.getPhone());
        admin.setRole(role);
        admin = adminRepository.save(admin);

        superAdminService.writeAudit(actorId, actor.getFullName(), "SUPER_ADMIN",
                "UPDATE", "ADMIN_MANAGEMENT", admin.getEmail(), before, safeState(admin), ip);

        return toResponse(admin);
    }

    /**
     * Reassigns a Sub-Admin's Role. This is the ONLY way permissions ever
     * change for an Admin — there is no direct permission assignment.
     */
    @Transactional
    public AdminDto.AdminResponse changeRole(UUID id, AdminDto.ChangeRoleRequest request, UUID actorId, String ip) {

        AdminEntity admin = findAdmin(id);
        SuperAdminEntity actor = findSuperAdmin(actorId);
        RoleEntity role = findActiveRole(request.getRoleId());
        Map<String, Object> before = safeState(admin);

        admin.setRole(role);
        admin = adminRepository.save(admin);

        superAdminService.writeAudit(actorId, actor.getFullName(), "SUPER_ADMIN",
                "ROLE_REASSIGN", "ADMIN_MANAGEMENT", admin.getEmail(), before, safeState(admin), ip);

        return toResponse(admin);
    }

    @Transactional
    public AdminDto.AdminResponse status(UUID id, boolean active, UUID actorId, String ip) {

        AdminEntity admin = findAdmin(id);
        SuperAdminEntity actor = findSuperAdmin(actorId);
        boolean before = admin.isActive();

        admin.setActive(active);
        admin = adminRepository.save(admin);

        superAdminService.writeAudit(actorId, actor.getFullName(), "SUPER_ADMIN",
                "STATUS_CHANGE", "ADMIN_MANAGEMENT", admin.getEmail(),
                Map.of("active", before), Map.of("active", active), ip);

        return toResponse(admin);
    }

    @Transactional
    public void delete(UUID id, UUID actorId, String ip) {

        AdminEntity admin = findAdmin(id);
        SuperAdminEntity actor = findSuperAdmin(actorId);
        Map<String, Object> before = safeState(admin);

        admin.setActive(false);
        admin.setDeletedAt(LocalDateTime.now());
        adminRepository.save(admin);

        superAdminService.writeAudit(actorId, actor.getFullName(), "SUPER_ADMIN",
                "DELETE", "ADMIN_MANAGEMENT", admin.getEmail(), before, safeState(admin), ip);
    }

    @Transactional
    public AdminDto.AdminResponse resetPassword(UUID id, AdminDto.ResetPasswordRequest request, UUID actorId, String ip) {

        AdminEntity admin = findAdmin(id);
        SuperAdminEntity actor = findSuperAdmin(actorId);

        admin.setPasswordHash(passwordUtil.encode(request.getNewPassword()));
        admin = adminRepository.save(admin);

        superAdminService.writeAudit(actorId, actor.getFullName(), "SUPER_ADMIN",
                "RESET_PASSWORD", "ADMIN_MANAGEMENT", admin.getEmail(),
                null, Map.of("passwordReset", true), ip);

        return toResponse(admin);
    }

    // NOTE: There is intentionally NO permissions(...) method here and no
    // PUT /admins/{id}/permissions endpoint. Direct permission assignment
    // to an Admin is not supported by design — use changeRole(...) instead.

    private AdminEntity findAdmin(UUID id) {
        return adminRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new SuperAdminService.ResourceNotFoundException("Admin not found"));
    }

    private SuperAdminEntity findSuperAdmin(UUID id) {
        return superAdminRepository.findById(id)
                .filter(x -> x.getDeletedAt() == null)
                .orElseThrow(() -> new SuperAdminService.ResourceNotFoundException("Super Admin not found"));
    }

    private RoleEntity findActiveRole(UUID roleId) {
        RoleEntity role = roleRepository.findByIdAndDeletedAtIsNull(roleId)
                .orElseThrow(() -> new SuperAdminService.BadRequestException("Role not found: " + roleId));
        if (!role.isActive()) {
            throw new SuperAdminService.BadRequestException("Role is inactive: " + role.getRoleName());
        }
        return role;
    }

    private Map<String, Object> safeState(AdminEntity admin) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("id", admin.getId());
        state.put("fullName", admin.getFullName());
        state.put("email", admin.getEmail());
        state.put("phone", admin.getPhone());
        state.put("roleId", admin.getRole().getId());
        state.put("roleName", admin.getRole().getRoleName());
        state.put("active", admin.isActive());
        return state;
    }

    private List<String> effectivePermissions(AdminEntity admin) {
        return rolePermissionRepository.findByRole_Id(admin.getRole().getId()).stream()
                .map(rp -> rp.getPermission())
                .filter(p -> p.isActive() && p.getDeletedAt() == null
                        && p.getModule() != null && p.getModule().isActive())
                .map(p -> p.getCode())
                .sorted()
                .toList();
    }

    private AdminDto.AdminResponse toResponse(AdminEntity a) {

        AdminDto.AdminResponse r = new AdminDto.AdminResponse();
        r.setId(a.getId());
        r.setCreatedBy(a.getCreatedBy() == null ? null : a.getCreatedBy().getId());
        r.setFullName(a.getFullName());
        r.setEmail(a.getEmail());
        r.setPhone(a.getPhone());
        r.setRoleId(a.getRole().getId());
        r.setRoleName(a.getRole().getRoleName());
        r.setActive(a.isActive());
        r.setLastLoginAt(a.getLastLoginAt());
        r.setLastLoginIp(a.getLastLoginIp());
        r.setCreatedAt(a.getCreatedAt());
        r.setUpdatedAt(a.getUpdatedAt());
        r.setDeletedAt(a.getDeletedAt());
        // Resolved live, purely informational — never stored on the Admin.
        r.setEffectivePermissions(a.isActive() && a.getRole().isActive()
                ? effectivePermissions(a) : List.of());
        return r;
    }
}
