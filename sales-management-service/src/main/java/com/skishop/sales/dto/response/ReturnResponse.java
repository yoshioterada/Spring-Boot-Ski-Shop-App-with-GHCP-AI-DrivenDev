package com.skishop.sales.dto.response;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 返品レスポンスDTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnResponse {

    private String id;
    private String returnNumber;
    private String orderId;
    private String orderItemId;
    private String customerId;
    private String reason;
    private String reasonDetail;
    private Integer quantity;
    private BigDecimal refundAmount;
    private String status;
    private LocalDateTime requestedAt;
    private LocalDateTime approvedAt;
    private LocalDateTime receivedAt;
    private LocalDateTime refundedAt;
    private String adminNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private OrderItemInfo orderItemInfo;

    /**
     * 注文明細情報
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemInfo {

        private String productId;
        private String productName;
        private String sku;
        private BigDecimal unitPrice;
        private Integer originalQuantity;
    }
}
