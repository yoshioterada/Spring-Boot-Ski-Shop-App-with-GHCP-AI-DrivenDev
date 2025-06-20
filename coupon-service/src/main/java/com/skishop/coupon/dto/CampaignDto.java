package com.skishop.coupon.dto;

import com.skishop.coupon.entity.Campaign;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public class CampaignDto {

    // プライベートコンストラクタを追加してユーティリティクラスにする
    private CampaignDto() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CampaignRequest {
        
        @NotBlank(message = "Campaign name is required")
        @Size(min = 3, max = 255, message = "Campaign name must be between 3 and 255 characters")
        private String name;
        
        @Size(max = 1000, message = "Description cannot exceed 1000 characters")
        private String description;
        
        @NotNull(message = "Campaign type is required")
        private Campaign.CampaignType campaignType;
        
        @NotNull(message = "Start date is required")
        @Future(message = "Start date must be in the future")
        private LocalDateTime startDate;
        
        @NotNull(message = "End date is required")
        @Future(message = "End date must be in the future")
        private LocalDateTime endDate;
        
        private Map<String, Object> rules;
        
        @Min(value = 1, message = "Max coupons must be at least 1")
        private Integer maxCoupons;
        
        @AssertTrue(message = "End date must be after start date")
        public boolean isEndDateAfterStartDate() {
            return endDate == null || startDate == null || endDate.isAfter(startDate);
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CampaignResponse {
        private UUID id;
        private String name;
        private String description;
        private Campaign.CampaignType campaignType;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private Boolean isActive;
        private Map<String, Object> rules;
        private Integer maxCoupons;
        private Integer generatedCoupons;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CampaignUpdateRequest {
        
        @Size(min = 3, max = 255, message = "Campaign name must be between 3 and 255 characters")
        private String name;
        
        @Size(max = 1000, message = "Description cannot exceed 1000 characters")
        private String description;
        
        private LocalDateTime startDate;
        
        private LocalDateTime endDate;
        
        private Map<String, Object> rules;
        
        @Min(value = 1, message = "Max coupons must be at least 1")
        private Integer maxCoupons;
        
        @AssertTrue(message = "End date must be after start date")
        public boolean isEndDateAfterStartDate() {
            return endDate == null || startDate == null || endDate.isAfter(startDate);
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CampaignAnalyticsResponse {
        private UUID campaignId;
        private String campaignName;
        private Integer totalCoupons;
        private Integer usedCoupons;
        private Double usageRate;
        private Double totalDiscount;
        private Double averageDiscount;
        private Integer totalOrders;
        private Double revenue;
        private Double conversionRate;
        private LocalDateTime periodStart;
        private LocalDateTime periodEnd;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CampaignListRequest {
        
        @Min(value = 0, message = "Page number must be non-negative")
        @Builder.Default
        private Integer page = 0;
        
        @Min(value = 1, message = "Page size must be at least 1")
        @Max(value = 100, message = "Page size cannot exceed 100")
        @Builder.Default
        private Integer size = 20;
        
        private Boolean isActive;
        
        private Campaign.CampaignType campaignType;
        
        private LocalDateTime startDate;
        
        private LocalDateTime endDate;
    }
}
