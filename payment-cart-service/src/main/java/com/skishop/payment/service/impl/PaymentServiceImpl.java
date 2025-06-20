package com.skishop.payment.service.impl;

import com.skishop.payment.dto.*;
import com.skishop.payment.entity.Cart;
import com.skishop.payment.entity.Payment;
import com.skishop.payment.repository.PaymentRepository;
import com.skishop.payment.service.PaymentService;
import com.skishop.payment.service.CartService;
import com.skishop.payment.mapper.PaymentMapper;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final CartService cartService;
    private final PaymentMapper paymentMapper;

    @Value("${stripe.secret-key:sk_test_dummy}")
    private String stripeSecretKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeSecretKey;
    }

    @Override
    @Transactional
    public PaymentIntentResponse createPaymentIntent(UUID userId, CreatePaymentIntentRequest request) {
        log.info("Creating payment intent for user: {} and cart: {}", userId, request.cartId());

        // Validate cart
        cartService.validateCartForPayment(request.cartId());
        Cart cart = cartService.getOrCreateCart(userId);

        try {
            // Create Stripe PaymentIntent
            Map<String, Object> params = new HashMap<>();
            params.put("amount", cart.getTotalAmount().multiply(BigDecimal.valueOf(100)).longValue()); // Convert to cents
            params.put("currency", request.currency().toLowerCase());
            params.put("payment_method_types", List.of("card"));

            PaymentIntent paymentIntent = PaymentIntent.create(params);

            // Save payment record
            Payment payment = Payment.builder()
                .userId(userId)
                .cartId(request.cartId())
                .paymentIntentId(paymentIntent.getId())
                .paymentMethod(request.paymentMethod())
                .amount(cart.getTotalAmount())
                .currency(request.currency())
                .status(Payment.PaymentStatus.PENDING)
                .gatewayProvider("stripe")
                .build();

            // Store Stripe response
            Map<String, Object> gatewayResponse = new HashMap<>();
            gatewayResponse.put("id", paymentIntent.getId());
            gatewayResponse.put("client_secret", paymentIntent.getClientSecret());
            gatewayResponse.put("status", paymentIntent.getStatus());
            payment.setGatewayResponse(gatewayResponse);

            payment = paymentRepository.save(payment);

            return paymentMapper.toPaymentIntentResponse(payment);

        } catch (StripeException e) {
            log.error("Failed to create payment intent: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create payment intent: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public PaymentResponse processPayment(UUID userId, UUID paymentId, ProcessPaymentRequest request) {
        log.info("Processing payment: {} for user: {}", paymentId, userId);

        Payment payment = getPaymentById(paymentId);
        
        if (!payment.getUserId().equals(userId)) {
            throw new RuntimeException("Payment does not belong to user");
        }

        if (payment.getStatus() != Payment.PaymentStatus.PENDING) {
            throw new RuntimeException("Payment is not in pending status");
        }

        try {
            // Confirm payment with Stripe
            PaymentIntent paymentIntent = PaymentIntent.retrieve(payment.getPaymentIntentId());
            
            Map<String, Object> confirmParams = new HashMap<>();
            confirmParams.put("payment_method", request.paymentMethodId());
            
            paymentIntent = paymentIntent.confirm(confirmParams);

            // Update payment status based on Stripe response using switch expression
            Payment.PaymentStatus newStatus = switch (paymentIntent.getStatus()) {
                case "succeeded" -> {
                    payment.setCompletedAt(LocalDateTime.now());
                    yield Payment.PaymentStatus.COMPLETED;
                }
                case "requires_action" -> Payment.PaymentStatus.REQUIRES_ACTION;
                case "canceled" -> Payment.PaymentStatus.CANCELLED;
                default -> Payment.PaymentStatus.FAILED;
            };

            payment.setStatus(newStatus);
            payment.setUpdatedAt(LocalDateTime.now());

            // Update gateway response
            Map<String, Object> updatedResponse = payment.getGatewayResponse();
            updatedResponse.put("status", paymentIntent.getStatus());
            updatedResponse.put("payment_intent_id", paymentIntent.getId());
            payment.setGatewayResponse(updatedResponse);

            payment = paymentRepository.save(payment);

            // If payment succeeded, clear the cart
            if (newStatus == Payment.PaymentStatus.COMPLETED) {
                cartService.clearCart(userId);
            }

            return paymentMapper.toPaymentResponse(payment);

        } catch (StripeException e) {
            log.error("Failed to process payment: {}", e.getMessage(), e);
            
            payment.setStatus(Payment.PaymentStatus.FAILED);
            payment.setUpdatedAt(LocalDateTime.now());
            
            Map<String, Object> failureResponse = payment.getGatewayResponse();
            failureResponse.put("failure_reason", e.getMessage());
            payment.setGatewayResponse(failureResponse);
            
            paymentRepository.save(payment);
            
            throw new RuntimeException("Failed to process payment: " + e.getMessage());
        }
    }

    @Override
    public PaymentResponse getPaymentStatus(UUID userId, UUID paymentId) {
        log.debug("Getting payment status for payment: {} and user: {}", paymentId, userId);
        
        Payment payment = getPaymentById(paymentId);
        
        if (!payment.getUserId().equals(userId)) {
            throw new RuntimeException("Payment does not belong to user");
        }

        return paymentMapper.toPaymentResponse(payment);
    }

    @Override
    public List<PaymentResponse> getPaymentHistory(UUID userId) {
        log.debug("Getting payment history for user: {}", userId);
        
        List<Payment> payments = paymentRepository.findByUserId(userId);
        return paymentMapper.toPaymentResponseList(payments);
    }

    @Override
    @Transactional
    public PaymentResponse processRefund(UUID userId, UUID paymentId, RefundRequest request) {
        log.info("Processing refund for payment: {} and user: {}", paymentId, userId);

        Payment payment = getPaymentById(paymentId);
        
        if (!payment.getUserId().equals(userId)) {
            throw new RuntimeException("Payment does not belong to user");
        }

        if (payment.getStatus() != Payment.PaymentStatus.COMPLETED) {
            throw new RuntimeException("Only completed payments can be refunded");
        }

        try {
            // Process refund with Stripe
            Map<String, Object> refundParams = new HashMap<>();
            refundParams.put("payment_intent", payment.getPaymentIntentId());
            refundParams.put("amount", request.amount().multiply(BigDecimal.valueOf(100)).longValue());
            
            if (request.reason() != null) {
                refundParams.put("reason", request.reason());
            }

            com.stripe.model.Refund refund = com.stripe.model.Refund.create(refundParams);

            // Update payment status
            payment.setStatus(Payment.PaymentStatus.REFUNDED);
            payment.setUpdatedAt(LocalDateTime.now());

            // Update gateway response with refund info
            Map<String, Object> updatedResponse = payment.getGatewayResponse();
            updatedResponse.put("refund_id", refund.getId());
            updatedResponse.put("refund_status", refund.getStatus());
            updatedResponse.put("refund_amount", refund.getAmount());
            payment.setGatewayResponse(updatedResponse);

            payment = paymentRepository.save(payment);

            return paymentMapper.toPaymentResponse(payment);

        } catch (StripeException e) {
            log.error("Failed to process refund: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process refund: " + e.getMessage());
        }
    }

    @Override
    public void handlePaymentWebhook(String payload, String signature) {
        log.info("Handling payment webhook");
        
        // This would implement Stripe webhook validation and processing
        // For now, just log the event
        log.debug("Webhook payload: {}", payload);
    }

    @Override
    public void validatePaymentIntent(UUID paymentId) {
        Payment payment = getPaymentById(paymentId);
        
        if (payment.getStatus() != Payment.PaymentStatus.PENDING) {
            throw new RuntimeException("Payment is not in pending status");
        }

        // Additional validation logic can be added here
    }

    @Override
    public Payment getPaymentById(UUID paymentId) {
        return paymentRepository.findById(paymentId)
            .orElseThrow(() -> new RuntimeException("Payment not found"));
    }

    @Override
    @Transactional
    public void updatePaymentStatus(UUID paymentId, Payment.PaymentStatus status) {
        Payment payment = getPaymentById(paymentId);
        payment.setStatus(status);
        payment.setUpdatedAt(LocalDateTime.now());
        
        if (status == Payment.PaymentStatus.COMPLETED) {
            payment.setCompletedAt(LocalDateTime.now());
        }
        
        paymentRepository.save(payment);
    }
}
