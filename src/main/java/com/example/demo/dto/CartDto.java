package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class CartDto {

    // ───────── REQUESTS ─────────

    public static class AddItemRequest {
        @NotNull(message = "productId is required")
        private UUID productId;
        @NotBlank(message = "size is required")
        private String size;
        @NotNull(message = "startDate is required")
        private LocalDate startDate;   // yyyy-MM-dd
        @NotNull(message = "endDate is required")
        private LocalDate endDate;     // yyyy-MM-dd (inclusive)

        public UUID getProductId() { return productId; }
        public void setProductId(UUID productId) { this.productId = productId; }
        public String getSize() { return size; }
        public void setSize(String size) { this.size = size; }
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    }

    public static class UpdateItemRequest {
        @NotBlank(message = "size is required")
        private String size;
        @NotNull(message = "startDate is required")
        private LocalDate startDate;
        @NotNull(message = "endDate is required")
        private LocalDate endDate;

        public String getSize() { return size; }
        public void setSize(String size) { this.size = size; }
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    }

    // ───────── RESPONSES ─────────

    public static class CartItemResponse {
        private UUID itemId;
        private UUID productId;
        private String productName;
        private String colour;
        private String coverImageUrl;
        private String size;
        private LocalDate startDate;
        private LocalDate endDate;
        private int days;
        private BigDecimal pricePerDay;       // offerPrice if set, else perDayPrice
        private BigDecimal lineTotal;         // pricePerDay * days
        private BigDecimal securityDeposit;
        private boolean stillAvailable;       // false if product hidden/deleted or no stock for dates

        public UUID getItemId() { return itemId; }
        public void setItemId(UUID itemId) { this.itemId = itemId; }
        public UUID getProductId() { return productId; }
        public void setProductId(UUID productId) { this.productId = productId; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public String getColour() { return colour; }
        public void setColour(String colour) { this.colour = colour; }
        public String getCoverImageUrl() { return coverImageUrl; }
        public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }
        public String getSize() { return size; }
        public void setSize(String size) { this.size = size; }
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
        public int getDays() { return days; }
        public void setDays(int days) { this.days = days; }
        public BigDecimal getPricePerDay() { return pricePerDay; }
        public void setPricePerDay(BigDecimal pricePerDay) { this.pricePerDay = pricePerDay; }
        public BigDecimal getLineTotal() { return lineTotal; }
        public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }
        public BigDecimal getSecurityDeposit() { return securityDeposit; }
        public void setSecurityDeposit(BigDecimal securityDeposit) { this.securityDeposit = securityDeposit; }
        public boolean isStillAvailable() { return stillAvailable; }
        public void setStillAvailable(boolean stillAvailable) { this.stillAvailable = stillAvailable; }
    }

    public static class CartResponse {
        private UUID cartId;              // null when the user has no cart yet
        private String username;
        private int itemCount;
        private List<CartItemResponse> items;
        private BigDecimal rentalSubtotal;
        private BigDecimal totalSecurityDeposit;
        private BigDecimal grandTotal;    // subtotal + deposit

        public UUID getCartId() { return cartId; }
        public void setCartId(UUID cartId) { this.cartId = cartId; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public int getItemCount() { return itemCount; }
        public void setItemCount(int itemCount) { this.itemCount = itemCount; }
        public List<CartItemResponse> getItems() { return items; }
        public void setItems(List<CartItemResponse> items) { this.items = items; }
        public BigDecimal getRentalSubtotal() { return rentalSubtotal; }
        public void setRentalSubtotal(BigDecimal rentalSubtotal) { this.rentalSubtotal = rentalSubtotal; }
        public BigDecimal getTotalSecurityDeposit() { return totalSecurityDeposit; }
        public void setTotalSecurityDeposit(BigDecimal totalSecurityDeposit) { this.totalSecurityDeposit = totalSecurityDeposit; }
        public BigDecimal getGrandTotal() { return grandTotal; }
        public void setGrandTotal(BigDecimal grandTotal) { this.grandTotal = grandTotal; }
    }

    public static class CartCountResponse {
        private int itemCount;
        public CartCountResponse() {}
        public CartCountResponse(int itemCount) { this.itemCount = itemCount; }
        public int getItemCount() { return itemCount; }
        public void setItemCount(int itemCount) { this.itemCount = itemCount; }
    }
}
