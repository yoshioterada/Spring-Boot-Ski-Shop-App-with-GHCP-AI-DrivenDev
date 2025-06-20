package com.skishop.coupon.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 配布ルール更新リクエスト
 */
public record DistributionRuleUpdateRequest(
    @NotBlank(message = "ルール名は必須です")
    String ruleName,
    
    @NotBlank(message = "配布タイプは必須です")
    String distributionType,
    
    LocalDateTime distributionStartTime,
    
    LocalDateTime distributionEndTime,
    
    @Min(value = 1, message = "配布数は1以上である必要があります")
    Integer distributionCount,
    
    @Min(value = 0, message = "優先度は0以上である必要があります")
    @Max(value = 100, message = "優先度は100以下である必要があります")
    Integer priority,
    
    List<String> targetUserTypes,
    
    List<String> targetUserIds,
    
    String description,
    
    Boolean isActive
) {}
