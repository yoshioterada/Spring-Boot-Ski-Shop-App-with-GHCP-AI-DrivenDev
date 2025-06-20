package com.skishop.sales.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 返品詳細レスポンス
 */
public record ReturnDetailResponse(
    String returnId,
    String orderId,
    String userId,
    String status,
    String reason,
    String description,
    List<ReturnItemDetail> items,
    String refundAmount,
    String refundMethod,
    String adminComments,
    LocalDateTime requestedAt,
    LocalDateTime processedAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public record ReturnItemDetail(
        String productId,
        String productName,
        Integer quantity,
        String reason,
        String condition
    ) {}
}
