package com.skishop.sales.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * 配送レポートレスポンス
 */
public record ShippingReportResponse(
    String reportType,
    String period,
    ShippingSummary summary,
    List<ShippingData> data,
    List<CarrierPerformance> carrierPerformance
) {
    public record ShippingSummary(
        Integer totalShipments,
        Integer deliveredShipments,
        Integer pendingShipments,
        BigDecimal averageDeliveryTime,
        BigDecimal onTimeDeliveryRate
    ) {}
    
    public record ShippingData(
        String date,
        Integer shipmentsCreated,
        Integer shipmentsDelivered,
        BigDecimal averageDeliveryTime
    ) {}
    
    public record CarrierPerformance(
        String carrier,
        Integer totalShipments,
        BigDecimal onTimeRate,
        BigDecimal averageDeliveryTime,
        BigDecimal costPerShipment
    ) {}
}
