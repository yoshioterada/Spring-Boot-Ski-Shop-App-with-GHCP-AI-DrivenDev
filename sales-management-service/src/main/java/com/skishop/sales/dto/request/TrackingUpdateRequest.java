package com.skishop.sales.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 追跡情報更新リクエスト
 */
public record TrackingUpdateRequest(
    @NotBlank(message = "追跡番号は必須です")
    String trackingNumber,
    
    @NotBlank(message = "配送業者は必須です")
    String carrier,
    
    String estimatedDeliveryDate
) {}
