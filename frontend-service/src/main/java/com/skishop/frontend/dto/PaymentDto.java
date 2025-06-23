package com.skishop.frontend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支払い情報DTO
 */
public record PaymentDto(
    String id,
    String method,
    String status,
    BigDecimal amount,
    LocalDateTime processedAt
) {}
