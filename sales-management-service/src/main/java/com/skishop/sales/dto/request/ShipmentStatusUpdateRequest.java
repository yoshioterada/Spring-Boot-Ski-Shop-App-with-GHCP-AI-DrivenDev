package com.skishop.sales.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 配送ステータス更新リクエスト
 */
public record ShipmentStatusUpdateRequest(
    @NotBlank(message = "ステータスは必須です")
    String status,
    
    String location,
    
    String comments
) {}
