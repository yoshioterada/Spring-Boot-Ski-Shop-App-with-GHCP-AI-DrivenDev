package com.skishop.frontend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * カートアイテム更新リクエスト（payment-cart-service用）
 */
public record UpdateCartItemRequest(
    @JsonProperty("quantity")
    int quantity
) {
}
