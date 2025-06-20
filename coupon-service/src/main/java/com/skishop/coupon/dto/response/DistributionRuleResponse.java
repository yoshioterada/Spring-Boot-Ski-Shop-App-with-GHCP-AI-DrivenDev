package com.skishop.coupon.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 配布ルール応答
 */
public record DistributionRuleResponse(
    String ruleId,
    String campaignId,
    String ruleName,
    String distributionType,
    LocalDateTime distributionStartTime,
    LocalDateTime distributionEndTime,
    Integer distributionCount,
    Integer priority,
    List<String> targetUserTypes,
    List<String> targetUserIds,
    String description,
    Boolean isActive,
    String status,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
