package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(
    name = "stanveeShop_user",
    indexes = {
        @Index(name = "idx_stanveeShop_user_username", columnList = "username", unique = true)
    }
)
public class StanveeUser {

    // IST zone id, reused wherever we need "current India time"
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // This is Stanvee's "loginid" e.g. SV4189392 — unique per user
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "name")
    private String name;

    @Column(name = "email")
    private String email;

    @Column(name = "mobile_no")
    private String mobileNo;

    // AES-encrypted, never stored/returned as plaintext
    @Column(name = "password_enc", nullable = false, length = 500)
    private String passwordEnc;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now(IST);

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now(IST);

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt = LocalDateTime.now(IST);

    // ── lifecycle callbacks to keep timestamps accurate on persist/update ──
    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now(IST);
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now(IST);
    }

    // ── getters/setters ──
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMobileNo() { return mobileNo; }
    public void setMobileNo(String mobileNo) { this.mobileNo = mobileNo; }

    public String getPasswordEnc() { return passwordEnc; }
    public void setPasswordEnc(String passwordEnc) { this.passwordEnc = passwordEnc; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }
}