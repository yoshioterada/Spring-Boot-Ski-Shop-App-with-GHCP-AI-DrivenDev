package com.skishop.payment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.Map;
import java.util.UUID;

/**
 * カートアイテム追加リクエスト
 * 
 * @param productId 商品ID
 * @param quantity 数量
 * @param productDetails 商品詳細情報
 */
public record AddCartItemRequest(
    @NotNull(message = "Product ID is required")
    UUID productId,
    
    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    Integer quantity,
    
    Map<String, Object> productDetails
) {}
