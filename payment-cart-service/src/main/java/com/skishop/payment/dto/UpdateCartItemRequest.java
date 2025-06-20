package com.skishop.payment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * カートアイテム更新リクエスト
 * 
 * @param quantity 数量
 */
public record UpdateCartItemRequest(
    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    Integer quantity
) {}
