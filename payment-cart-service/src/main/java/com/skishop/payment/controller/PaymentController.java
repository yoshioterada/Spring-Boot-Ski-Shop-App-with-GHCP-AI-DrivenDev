package com.skishop.payment.controller;

import com.skishop.payment.dto.*;
import com.skishop.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/intent")
    public ResponseEntity<ApiResponse<PaymentIntentResponse>> createPaymentIntent(
            @Valid @RequestBody CreatePaymentIntentRequest request,
            Authentication authentication) {
        
        log.info("Creating payment intent for user: {} and cart: {}", 
                authentication.getName(), request.cartId());
        
        UUID userId = UUID.fromString(authentication.getName());
        PaymentIntentResponse response = paymentService.createPaymentIntent(userId, request);
        
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(response, "Payment intent created successfully"));
    }

    @PostMapping("/{paymentId}/process")
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(
            @PathVariable UUID paymentId,
            @Valid @RequestBody ProcessPaymentRequest request,
            Authentication authentication) {
        
        log.info("Processing payment: {} for user: {}", paymentId, authentication.getName());
        
        UUID userId = UUID.fromString(authentication.getName());
        PaymentResponse response = paymentService.processPayment(userId, paymentId, request);
        
        return ResponseEntity.ok(ApiResponse.success(response, "Payment processed successfully"));
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentStatus(
            @PathVariable UUID paymentId,
            Authentication authentication) {
        
        log.debug("Getting payment status for payment: {} and user: {}", 
                paymentId, authentication.getName());
        
        UUID userId = UUID.fromString(authentication.getName());
        PaymentResponse response = paymentService.getPaymentStatus(userId, paymentId);
        
        return ResponseEntity.ok(ApiResponse.success(response, "Payment status retrieved successfully"));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getPaymentHistory(
            Authentication authentication) {
        
        log.debug("Getting payment history for user: {}", authentication.getName());
        
        UUID userId = UUID.fromString(authentication.getName());
        List<PaymentResponse> response = paymentService.getPaymentHistory(userId);
        
        return ResponseEntity.ok(ApiResponse.success(response, "Payment history retrieved successfully"));
    }

    @PostMapping("/{paymentId}/refund")
    public ResponseEntity<ApiResponse<PaymentResponse>> processRefund(
            @PathVariable UUID paymentId,
            @Valid @RequestBody RefundRequest request,
            Authentication authentication) {
        
        log.info("Processing refund for payment: {} and user: {}", 
                paymentId, authentication.getName());
        
        UUID userId = UUID.fromString(authentication.getName());
        PaymentResponse response = paymentService.processRefund(userId, paymentId, request);
        
        return ResponseEntity.ok(ApiResponse.success(response, "Refund processed successfully"));
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {
        
        log.info("Handling payment webhook");
        
        try {
            paymentService.handlePaymentWebhook(payload, signature);
            return ResponseEntity.ok("Webhook handled successfully");
        } catch (Exception e) {
            log.error("Failed to handle webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("Failed to handle webhook: " + e.getMessage());
        }
    }
}
