package com.skishop.sales.dto.response;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 配送レスポンスDTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentResponse {

    private String id;
    private String orderId;
    private String carrier;
    private String trackingNumber;
    private String status;
    private ShippingAddressResponse shippingAddress;
    private LocalDateTime shippedAt;
    private LocalDateTime estimatedDeliveryAt;
    private LocalDateTime deliveredAt;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 配送先住所レスポンスDTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShippingAddressResponse {

        private String postalCode;
        private String prefecture;
        private String city;
        private String addressLine1;
        private String addressLine2;
        private String recipientName;
        private String phoneNumber;
    }
}
