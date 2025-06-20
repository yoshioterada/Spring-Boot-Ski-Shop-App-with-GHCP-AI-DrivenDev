package com.skishop.payment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * 返金リクエスト
 * 
 * @param amount 返金金額
 * @param reason 返金理由
 */
public record RefundRequest(
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    BigDecimal amount,
    
    String reason
) {}
