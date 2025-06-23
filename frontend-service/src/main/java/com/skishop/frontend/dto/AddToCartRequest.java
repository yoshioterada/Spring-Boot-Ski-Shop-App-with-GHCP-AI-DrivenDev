package com.skishop.frontend.dto;

import java.util.UUID;

/**
 * カートアイテム追加リクエストDTO
 */
public record AddToCartRequest(
    String productId,
    int quantity
) {
    // payment-cart-serviceのAPIで必要なUUID変換メソッド
    public UUID getProductIdAsUUID() {
        try {
            return UUID.fromString(productId);
        } catch (IllegalArgumentException e) {
            // UUIDではない場合はランダムUUIDを生成（テスト用）
            return UUID.randomUUID();
        }
    }
}
