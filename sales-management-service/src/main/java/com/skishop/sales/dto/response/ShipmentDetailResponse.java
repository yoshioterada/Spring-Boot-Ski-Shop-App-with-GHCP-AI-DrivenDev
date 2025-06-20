package com.skishop.sales.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 配送詳細レスポンス
 */
public record ShipmentDetailResponse(
    String shipmentId,
    String orderId,
    String status,
    String trackingNumber,
    String carrier,
    String shippingMethod,
    ShippingAddress shippingAddress,
    List<ShipmentItem> items,
    String estimatedDeliveryDate,
    String actualDeliveryDate,
    List<TrackingEvent> trackingEvents,
    LocalDateTime shippedAt,
    LocalDateTime deliveredAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public record ShippingAddress(
        String fullName,
        String address1,
        String address2,
        String city,
        String state,
        String postalCode,
        String country,
        String phoneNumber
    ) {}
    
    public record ShipmentItem(
        String productId,
        String productName,
        Integer quantity,
        String weight,
        String dimensions
    ) {}
    
    public record TrackingEvent(
        String status,
        String location,
        String description,
        LocalDateTime timestamp
    ) {}
}
