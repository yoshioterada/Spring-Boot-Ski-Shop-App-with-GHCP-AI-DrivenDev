package com.skishop.point.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointRedemptionRequest {
    
    @NotNull(message = "User ID is required")
    private UUID userId;
    
    @NotNull(message = "Points to redeem is required")
    @Positive(message = "Points must be positive")
    private Integer pointsToRedeem;
    
    @NotNull(message = "Redemption type is required")
    private String redemptionType; // 'discount', 'product', 'cashback'
    
    private Map<String, String> details;
}
