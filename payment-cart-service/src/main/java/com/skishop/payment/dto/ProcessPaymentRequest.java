package com.skishop.payment.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 決済処理リクエスト
 * 
 * @param paymentMethodId 決済方法ID
 * @param billingDetails 請求先詳細
 * @param savePaymentMethod 決済方法を保存するか
 */
public record ProcessPaymentRequest(
    @NotBlank(message = "Payment method ID is required")
    String paymentMethodId,
    
    BillingDetailsRequest billingDetails,
    
    boolean savePaymentMethod
) {}
