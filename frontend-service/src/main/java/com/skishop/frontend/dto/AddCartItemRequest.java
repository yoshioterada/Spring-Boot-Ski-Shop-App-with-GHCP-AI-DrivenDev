package com.skishop.frontend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.UUID;

/**
 * カートアイテム追加リクエスト（payment-cart-service用）
 */
public record AddCartItemRequest(
    @JsonProperty("productId")
    UUID productId,
    
    @JsonProperty("quantity")
    int quantity,
    
    @JsonProperty("productDetails")
    Map<String, Object> productDetails
) {
}
