package com.skishop.sales.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * 返品レポートレスポンス
 */
public record ReturnReportResponse(
    String reportType,
    String period,
    ReturnSummary summary,
    List<ReturnData> data,
    List<ReturnReason> topReasons
) {
    public record ReturnSummary(
        Integer totalReturns,
        Integer processedReturns,
        Integer pendingReturns,
        BigDecimal returnRate,
        BigDecimal totalRefundAmount
    ) {}
    
    public record ReturnData(
        String date,
        Integer returnsReceived,
        Integer returnsProcessed,
        BigDecimal refundAmount
    ) {}
    
    public record ReturnReason(
        String reason,
        Integer count,
        String percentage
    ) {}
}
