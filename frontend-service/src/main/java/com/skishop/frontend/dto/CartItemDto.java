package com.skishop.frontend.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.Map;

/**
 * カートアイテムDTO（payment-cart-service対応）
 */
public record CartItemDto(
    @JsonProperty("id")
    String id,
    
    @JsonProperty("productId")
    String productId,
    
    @JsonProperty("quantity")
    int quantity,
    
    @JsonProperty("unitPrice")
    int unitPrice, // payment-cart-serviceではint型で返される
    
    @JsonProperty("totalPrice")
    int totalPrice, // payment-cart-serviceではint型で返される
    
    @JsonProperty("productDetails")
    Map<String, Object> productDetails
) {
    // 既存のテンプレートとの互換性のためのメソッド
    public String productName() {
        if (productDetails != null) {
            Object name = productDetails.get("name");
            return name != null ? name.toString() : "商品名なし";
        }
        return "商品名なし";
    }
    
    public String imageUrl() {
        if (productDetails != null) {
            Object imageUrl = productDetails.get("imageUrl");
            return imageUrl != null ? imageUrl.toString() : "/images/no-image.png";
        }
        return "/images/no-image.png";
    }
    
    // BigDecimal版のgetterメソッド（テンプレートとの互換性）
    public BigDecimal getUnitPriceBigDecimal() {
        return BigDecimal.valueOf(unitPrice);
    }
    
    public BigDecimal getTotalPriceBigDecimal() {
        return BigDecimal.valueOf(totalPrice);
    }
}
