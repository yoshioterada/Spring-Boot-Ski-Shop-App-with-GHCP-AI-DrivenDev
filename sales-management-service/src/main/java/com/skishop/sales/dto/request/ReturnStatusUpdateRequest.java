package com.skishop.sales.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 返品ステータス更新リクエスト
 */
public record ReturnStatusUpdateRequest(
    @NotBlank(message = "ステータスは必須です")
    String status,
    
    String reason,
    
    String adminComments
) {}
