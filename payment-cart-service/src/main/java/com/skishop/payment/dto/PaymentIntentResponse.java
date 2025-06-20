package com.skishop.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentIntentResponse {

    private UUID paymentId;
    private String clientSecret;
    private BigDecimal amount;
    private String currency;
    private String status;
    private String paymentMethod;
}
