package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public final class CategoryDto {

    private CategoryDto() {}

    // ═══════════════════════════════════════════
    //  REQUESTS
    // ═══════════════════════════════════════════

    public static class CreateCategoryRequest {
        @NotBlank(message = "Category name must not be blank")
        private String name;

        public String getName() { return name; }
        public void setName(String v) { name = v; }
    }

    public static class UpdateCategoryRequest {
        @NotBlank(message = "Category name must not be blank")
        private String name;

        public String getName() { return name; }
        public void setName(String v) { name = v; }
    }

    public static class ActiveFlagRequest {
        @NotNull(message = "active flag must not be null")
        private Boolean active;

        public Boolean getActive() { return active; }
        public void setActive(Boolean v) { active = v; }
    }

    // ═══════════════════════════════════════════
    //  RESPONSES
    // ═══════════════════════════════════════════

    public static class CategoryResponse {
        private UUID id;
        private String name;
        private boolean isActive;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime deletedAt;

        public UUID getId() { return id; }
        public void setId(UUID v) { id = v; }
        public String getName() { return name; }
        public void setName(String v) { name = v; }
        public boolean isActive() { return isActive; }
        public void setActive(boolean v) { isActive = v; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime v) { createdAt = v; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime v) { updatedAt = v; }
        public LocalDateTime getDeletedAt() { return deletedAt; }
        public void setDeletedAt(LocalDateTime v) { deletedAt = v; }
    }

    public static class CategoryListItemResponse {
        private UUID id;
        private String name;
        private boolean isActive;
        private LocalDateTime createdAt;

        public UUID getId() { return id; }
        public void setId(UUID v) { id = v; }
        public String getName() { return name; }
        public void setName(String v) { name = v; }
        public boolean isActive() { return isActive; }
        public void setActive(boolean v) { isActive = v; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime v) { createdAt = v; }
    }
}
