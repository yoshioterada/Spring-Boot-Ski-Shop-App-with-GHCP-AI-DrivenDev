package com.skishop.frontend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 注文DTO
 */
public record OrderDto(
    String id,
    String orderNumber,
    String customerId,
    String status,
    BigDecimal totalAmount,
    List<OrderItemDto> items,
    AddressDto shippingAddress,
    AddressDto billingAddress,
    PaymentDto payment,
    LocalDateTime orderDate,
    LocalDateTime deliveryDate
) {}
