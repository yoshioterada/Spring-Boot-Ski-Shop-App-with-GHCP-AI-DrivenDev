package com.skishop.payment.service;

import com.skishop.payment.dto.*;
import com.skishop.payment.entity.Payment;

import java.util.List;
import java.util.UUID;

public interface PaymentService {

    PaymentIntentResponse createPaymentIntent(UUID userId, CreatePaymentIntentRequest request);

    PaymentResponse processPayment(UUID userId, UUID paymentId, ProcessPaymentRequest request);

    PaymentResponse getPaymentStatus(UUID userId, UUID paymentId);

    List<PaymentResponse> getPaymentHistory(UUID userId);

    PaymentResponse processRefund(UUID userId, UUID paymentId, RefundRequest request);

    void handlePaymentWebhook(String payload, String signature);

    void validatePaymentIntent(UUID paymentId);

    Payment getPaymentById(UUID paymentId);

    void updatePaymentStatus(UUID paymentId, Payment.PaymentStatus status);
}
