package com.skishop.coupon.dto;

import com.skishop.coupon.entity.Campaign;
import com.skishop.coupon.entity.Coupon;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public class CouponDto {

    // プライベートコンストラクタを追加してユーティリティクラスにする
    private CouponDto() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CouponRequest {
        
        @NotNull(message = "Campaign ID is required")
        private UUID campaignId;
        
        @NotBlank(message = "Coupon code is required")
        @Size(min = 3, max = 50, message = "Coupon code must be between 3 and 50 characters")
        private String code;
        
        @NotNull(message = "Coupon type is required")
        private Coupon.CouponType couponType;
        
        @NotNull(message = "Discount value is required")
        @DecimalMin(value = "0.01", message = "Discount value must be greater than 0")
        private BigDecimal discountValue;
        
        @NotNull(message = "Discount type is required")
        private Coupon.DiscountType discountType;
        
        @DecimalMin(value = "0", message = "Minimum amount must be greater than or equal to 0")
        private BigDecimal minimumAmount;
        
        @DecimalMin(value = "0", message = "Maximum discount must be greater than or equal to 0")
        private BigDecimal maximumDiscount;
        
        @Min(value = 1, message = "Usage limit must be at least 1")
        private Integer usageLimit;
        
        @NotNull(message = "Expiration date is required")
        @Future(message = "Expiration date must be in the future")
        private LocalDateTime expiresAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CouponResponse {
        private UUID id;
        private UUID campaignId;
        private String campaignName;
        private String code;
        private Coupon.CouponType couponType;
        private BigDecimal discountValue;
        private Coupon.DiscountType discountType;
        private BigDecimal minimumAmount;
        private BigDecimal maximumDiscount;
        private Integer usageLimit;
        private Integer usedCount;
        private Boolean isActive;
        private LocalDateTime expiresAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkCouponRequest {
        
        @NotNull(message = "Campaign ID is required")
        private UUID campaignId;
        
        @Min(value = 1, message = "Count must be at least 1")
        @Max(value = 10000, message = "Count cannot exceed 10000")
        private Integer count;
        
        @NotBlank(message = "Code pattern is required")
        private String codePattern;
        
        @NotNull(message = "Coupon configuration is required")
        private CouponConfig couponConfig;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CouponConfig {
        
        @NotNull(message = "Coupon type is required")
        private Coupon.CouponType couponType;
        
        @NotNull(message = "Discount value is required")
        @DecimalMin(value = "0.01", message = "Discount value must be greater than 0")
        private BigDecimal discountValue;
        
        @DecimalMin(value = "0", message = "Minimum amount must be greater than or equal to 0")
        private BigDecimal minimumAmount;
        
        @DecimalMin(value = "0", message = "Maximum discount must be greater than or equal to 0")
        private BigDecimal maximumDiscount;
        
        @Min(value = 1, message = "Usage limit must be at least 1")
        private Integer usageLimit;
        
        @NotNull(message = "Expiration date is required")
        @Future(message = "Expiration date must be in the future")
        private LocalDateTime expiresAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkCouponResponse {
        private UUID batchId;
        private UUID campaignId;
        private Integer requestedCount;
        private Integer generatedCount;
        private java.util.List<String> generatedCodes;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CouponValidationRequest {
        
        @NotBlank(message = "Coupon code is required")
        private String code;
        
        @NotNull(message = "Cart amount is required")
        @DecimalMin(value = "0.01", message = "Cart amount must be greater than 0")
        private BigDecimal cartAmount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CouponValidationResponse {
        private Boolean isValid;
        private CouponInfo coupon;
        private DiscountInfo discount;
        private String errorMessage;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CouponInfo {
        private UUID id;
        private String code;
        private Coupon.DiscountType discountType;
        private BigDecimal discountValue;
        private BigDecimal minimumAmount;
        private BigDecimal maximumDiscount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiscountInfo {
        private BigDecimal amount;
        private BigDecimal finalAmount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CouponRedeemRequest {
        
        @NotBlank(message = "Coupon code is required")
        private String code;
        
        @NotNull(message = "Order ID is required")
        private UUID orderId;
        
        @NotNull(message = "Order amount is required")
        @DecimalMin(value = "0.01", message = "Order amount must be greater than 0")
        private BigDecimal orderAmount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CouponRedeemResponse {
        private UUID usageId;
        private BigDecimal discountApplied;
        private BigDecimal finalAmount;
        private LocalDateTime redeemedAt;
    }
}
