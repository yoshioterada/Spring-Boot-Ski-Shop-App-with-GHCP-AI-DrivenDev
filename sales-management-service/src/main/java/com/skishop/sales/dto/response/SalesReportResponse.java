package com.skishop.sales.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * 売上レポートレスポンス
 */
public record SalesReportResponse(
    String reportType,
    String period,
    SalesSummary summary,
    List<SalesData> data,
    List<TopProduct> topProducts,
    List<SalesTrend> trends
) {
    public record SalesSummary(
        BigDecimal totalRevenue,
        Integer totalOrders,
        BigDecimal averageOrderValue,
        Integer totalCustomers,
        BigDecimal conversionRate
    ) {}
    
    public record SalesData(
        String date,
        BigDecimal revenue,
        Integer orders,
        Integer customers
    ) {}
    
    public record TopProduct(
        String productId,
        String productName,
        Integer quantity,
        BigDecimal revenue
    ) {}
    
    public record SalesTrend(
        String period,
        BigDecimal value,
        String changePercentage
    ) {}
}
