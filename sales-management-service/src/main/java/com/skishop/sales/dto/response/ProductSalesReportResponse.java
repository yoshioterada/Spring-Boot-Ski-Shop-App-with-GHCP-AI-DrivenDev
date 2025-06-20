package com.skishop.sales.dto.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品売上レポートレスポンス
 */
public record ProductSalesReportResponse(
    String reportType,
    String period,
    List<ProductSalesData> products,
    ProductSalesSummary summary
) {
    public record ProductSalesData(
        String productId,
        String productName,
        String category,
        Integer quantitySold,
        BigDecimal revenue,
        BigDecimal averagePrice,
        Integer totalOrders,
        String rank
    ) {}
    
    public record ProductSalesSummary(
        Integer totalProducts,
        BigDecimal totalRevenue,
        Integer totalQuantitySold,
        String topSellingProduct,
        String highestRevenueProduct
    ) {}
}
