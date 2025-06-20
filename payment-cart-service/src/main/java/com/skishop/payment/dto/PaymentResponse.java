package com.skishop.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private UUID paymentId;
    private String status;
    private BigDecimal amount;
    private String currency;
    private String transactionId;
    private String paymentMethod;
    private LocalDateTime completedAt;
    private String failureReason;
}
