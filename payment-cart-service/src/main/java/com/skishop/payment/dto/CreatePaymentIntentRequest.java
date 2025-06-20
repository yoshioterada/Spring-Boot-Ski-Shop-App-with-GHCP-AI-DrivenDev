package com.skishop.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * 決済インテント作成リクエスト
 * 
 * @param cartId カートID
 * @param paymentMethod 決済方法
 * @param currency 通貨コード
 * @param billingDetails 請求先詳細
 */
public record CreatePaymentIntentRequest(
    @NotNull(message = "Cart ID is required")
    UUID cartId,
    
    @NotBlank(message = "Payment method is required")
    String paymentMethod,
    
    String currency,
    
    BillingDetailsRequest billingDetails
) {
    public CreatePaymentIntentRequest {
        // デフォルト値の設定
        if (currency == null || currency.isBlank()) {
            currency = "JPY";
        }
    }
}
