package com.skishop.frontend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * カートDTO（payment-cart-service対応）
 */
public record CartDto(
    @JsonProperty("id")
    String id,
    
    @JsonProperty("userId")
    String userId,
    
    @JsonProperty("items")
    List<CartItemDto> items,
    
    @JsonProperty("totalAmount")
    int totalAmount, // payment-cart-serviceではint型で返される
    
    @JsonProperty("currency")
    String currency,
    
    @JsonProperty("itemCount")
    int itemCount
) {
    // 既存のテンプレートとの互換性のためのメソッド
    public BigDecimal subtotal() {
        return BigDecimal.valueOf(totalAmount);
    }
    
    public BigDecimal tax() {
        // 消費税10%として計算
        return BigDecimal.valueOf(totalAmount * 0.1);
    }
    
    public BigDecimal shipping() {
        // 送料は5000円以上で無料
        return totalAmount >= 5000 ? BigDecimal.ZERO : BigDecimal.valueOf(500);
    }
    
    public BigDecimal total() {
        return subtotal().add(tax()).add(shipping());
    }
}
