package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class WishlistDto {

    // ───────── REQUEST ─────────

    public static class AddItemRequest {
        @NotNull(message = "productId is required")
        private UUID productId;

        public UUID getProductId() { return productId; }
        public void setProductId(UUID productId) { this.productId = productId; }
    }

    // ───────── RESPONSES ─────────

    public static class WishlistItemResponse {
        private UUID wishlistItemId;
        private UUID productId;
        private String productName;
        private String categoryName;
        private String colour;
        private String occasion;
        private String coverImageUrl;
        private BigDecimal perDayPrice;
        private BigDecimal offerPrice;
        private boolean available;        // false if product is hidden / inactive / deleted
        private LocalDateTime addedAt;

        public UUID getWishlistItemId() { return wishlistItemId; }
        public void setWishlistItemId(UUID wishlistItemId) { this.wishlistItemId = wishlistItemId; }
        public UUID getProductId() { return productId; }
        public void setProductId(UUID productId) { this.productId = productId; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public String getCategoryName() { return categoryName; }
        public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
        public String getColour() { return colour; }
        public void setColour(String colour) { this.colour = colour; }
        public String getOccasion() { return occasion; }
        public void setOccasion(String occasion) { this.occasion = occasion; }
        public String getCoverImageUrl() { return coverImageUrl; }
        public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }
        public BigDecimal getPerDayPrice() { return perDayPrice; }
        public void setPerDayPrice(BigDecimal perDayPrice) { this.perDayPrice = perDayPrice; }
        public BigDecimal getOfferPrice() { return offerPrice; }
        public void setOfferPrice(BigDecimal offerPrice) { this.offerPrice = offerPrice; }
        public boolean isAvailable() { return available; }
        public void setAvailable(boolean available) { this.available = available; }
        public LocalDateTime getAddedAt() { return addedAt; }
        public void setAddedAt(LocalDateTime addedAt) { this.addedAt = addedAt; }
    }

    public static class WishlistResponse {
        private String username;
        private int itemCount;
        private List<WishlistItemResponse> items;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public int getItemCount() { return itemCount; }
        public void setItemCount(int itemCount) { this.itemCount = itemCount; }
        public List<WishlistItemResponse> getItems() { return items; }
        public void setItems(List<WishlistItemResponse> items) { this.items = items; }
    }

    public static class WishlistCountResponse {
        private int itemCount;
        public WishlistCountResponse() {}
        public WishlistCountResponse(int itemCount) { this.itemCount = itemCount; }
        public int getItemCount() { return itemCount; }
        public void setItemCount(int itemCount) { this.itemCount = itemCount; }
    }

    public static class WishlistCheckResponse {
        private UUID productId;
        private boolean wishlisted;
        public WishlistCheckResponse() {}
        public WishlistCheckResponse(UUID productId, boolean wishlisted) {
            this.productId = productId;
            this.wishlisted = wishlisted;
        }
        public UUID getProductId() { return productId; }
        public void setProductId(UUID productId) { this.productId = productId; }
        public boolean isWishlisted() { return wishlisted; }
        public void setWishlisted(boolean wishlisted) { this.wishlisted = wishlisted; }
    }
}
