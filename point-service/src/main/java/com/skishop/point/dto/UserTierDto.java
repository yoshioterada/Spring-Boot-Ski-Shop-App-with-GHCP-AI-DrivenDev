package com.skishop.point.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserTierDto {
    
    private UUID id;
    private UUID userId;
    private String tierLevel;
    private String tierName;
    private Integer totalPointsEarned;
    private Integer currentPoints;
    private BigDecimal pointMultiplier;
    private Map<String, Object> benefits;
    private String nextTier;
    private Integer pointsToNextTier;
    private LocalDateTime tierUpgradedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
