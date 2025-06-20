package com.skishop.sales.dto.response;

import java.util.List;

/**
 * 配送一覧レスポンス
 */
public record ShipmentListResponse(
    List<ShipmentSummary> shipments,
    Integer totalCount,
    Integer totalPages,
    Integer currentPage,
    Integer pageSize
) {
    public record ShipmentSummary(
        String shipmentId,
        String orderId,
        String status,
        String trackingNumber,
        String carrier,
        String estimatedDeliveryDate,
        String shippedAt,
        String deliveredAt
    ) {}
}
