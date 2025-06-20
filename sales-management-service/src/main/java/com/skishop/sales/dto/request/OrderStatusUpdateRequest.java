package com.skishop.sales.dto.request;

import jakarta.validation.constraints.*;

/**
 * 注文状態更新リクエストDTO
 * Java 21のRecordを使用してイミュータブルなデータ構造を定義
 */
public record OrderStatusUpdateRequest(
    @NotBlank(message = "注文状態は必須です")
    @Pattern(regexp = "PENDING|CONFIRMED|PROCESSING|SHIPPED|DELIVERED|CANCELLED|RETURNED", 
             message = "有効な注文状態を指定してください")
    String status,

    String notes) {
}

/**
 * 支払い状態更新リクエストDTO
 * Java 21のRecordを使用
 */
record PaymentStatusUpdateRequest(
    @NotBlank(message = "支払い状態は必須です")
    @Pattern(regexp = "PENDING|PAID|FAILED|REFUNDED|PARTIALLY_REFUNDED", 
             message = "有効な支払い状態を指定してください")
    String paymentStatus,

    String transactionId,

    String notes) {
}
