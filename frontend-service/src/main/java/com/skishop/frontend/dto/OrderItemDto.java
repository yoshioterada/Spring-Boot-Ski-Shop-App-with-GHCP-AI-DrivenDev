package com.skishop.frontend.dto;

import java.math.BigDecimal;

/**
 * 注文アイテムDTO
 */
public record OrderItemDto(
    String productId,
    String productName,
    BigDecimal unitPrice,
    int quantity,
    BigDecimal totalPrice
) {}
