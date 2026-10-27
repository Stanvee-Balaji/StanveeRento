package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class AddressDto {

    // ───────── REQUEST (used for both add and update) ─────────

    public static class AddressRequest {

        @NotBlank(message = "label is required")
        @Pattern(regexp = "(?i)HOME|WORK|OTHER", message = "label must be HOME, WORK or OTHER")
        private String label;

        @NotBlank(message = "fullName is required")
        @Size(max = 100, message = "fullName must be at most 100 characters")
        private String fullName;

        @NotBlank(message = "phone is required")
        @Pattern(regexp = "^[6-9]\\d{9}$", message = "phone must be a valid 10-digit Indian mobile number")
        private String phone;

        @NotBlank(message = "addressLine1 is required")
        @Size(max = 255, message = "addressLine1 must be at most 255 characters")
        private String addressLine1;

        @Size(max = 255, message = "addressLine2 must be at most 255 characters")
        private String addressLine2;

        @Size(max = 255, message = "landmark must be at most 255 characters")
        private String landmark;

        @NotBlank(message = "city is required")
        @Size(max = 100)
        private String city;

        @NotBlank(message = "state is required")
        @Size(max = 100)
        private String state;

        @NotBlank(message = "pincode is required")
        @Pattern(regexp = "^[1-9]\\d{5}$", message = "pincode must be a valid 6-digit Indian pincode")
        private String pincode;

        /** Optional – defaults to "India". */
        @Size(max = 100)
        private String country;

        /** Optional – the first address is always made default automatically. */
        private Boolean isDefault;

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getAddressLine1() { return addressLine1; }
        public void setAddressLine1(String addressLine1) { this.addressLine1 = addressLine1; }
        public String getAddressLine2() { return addressLine2; }
        public void setAddressLine2(String addressLine2) { this.addressLine2 = addressLine2; }
        public String getLandmark() { return landmark; }
        public void setLandmark(String landmark) { this.landmark = landmark; }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        public String getState() { return state; }
        public void setState(String state) { this.state = state; }
        public String getPincode() { return pincode; }
        public void setPincode(String pincode) { this.pincode = pincode; }
        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }
        public Boolean getIsDefault() { return isDefault; }
        public void setIsDefault(Boolean isDefault) { this.isDefault = isDefault; }
    }

    // ───────── RESPONSES ─────────

    public static class AddressResponse {
        private UUID addressId;
        private String label;
        private String fullName;
        private String phone;
        private String addressLine1;
        private String addressLine2;
        private String landmark;
        private String city;
        private String state;
        private String pincode;
        private String country;
        private Boolean isDefault;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public UUID getAddressId() { return addressId; }
        public void setAddressId(UUID addressId) { this.addressId = addressId; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getAddressLine1() { return addressLine1; }
        public void setAddressLine1(String addressLine1) { this.addressLine1 = addressLine1; }
        public String getAddressLine2() { return addressLine2; }
        public void setAddressLine2(String addressLine2) { this.addressLine2 = addressLine2; }
        public String getLandmark() { return landmark; }
        public void setLandmark(String landmark) { this.landmark = landmark; }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        public String getState() { return state; }
        public void setState(String state) { this.state = state; }
        public String getPincode() { return pincode; }
        public void setPincode(String pincode) { this.pincode = pincode; }
        public String getCountry() { return country; }
        public void setCountry(String country) { this.country = country; }
        public Boolean getIsDefault() { return isDefault; }
        public void setIsDefault(Boolean isDefault) { this.isDefault = isDefault; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    }

    public static class AddressListResponse {
        private String username;
        private int addressCount;
        private List<AddressResponse> addresses;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public int getAddressCount() { return addressCount; }
        public void setAddressCount(int addressCount) { this.addressCount = addressCount; }
        public List<AddressResponse> getAddresses() { return addresses; }
        public void setAddresses(List<AddressResponse> addresses) { this.addresses = addresses; }
    }
}
