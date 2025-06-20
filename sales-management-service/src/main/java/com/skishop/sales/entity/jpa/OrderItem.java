package com.skishop.sales.entity.jpa;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 注文明細エンティティ（PostgreSQL）
 * 注文に含まれる商品の詳細情報を管理
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "order_items", indexes = {
    @Index(name = "idx_order_items_order_id", columnList = "orderId"),
    @Index(name = "idx_order_items_product_id", columnList = "productId")
})
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * 注文ID（外部キー）
     */
    @Column(nullable = false)
    private UUID orderId;

    /**
     * 商品ID
     */
    @Column(nullable = false, length = 100)
    private String productId;

    /**
     * 商品名（注文時のスナップショット）
     */
    @Column(nullable = false, length = 200)
    private String productName;

    /**
     * 商品SKU
     */
    @Column(nullable = false, length = 100)
    private String sku;

    /**
     * 単価（注文時の価格）
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    /**
     * 数量
     */
    @Column(nullable = false)
    private Integer quantity;

    /**
     * 小計（単価 × 数量）
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    /**
     * 適用されたクーポンID
     */
    private String appliedCouponId;

    /**
     * クーポン割引額
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal couponDiscountAmount;

    /**
     * 使用されたポイント数
     */
    private Integer usedPoints;

    /**
     * ポイント割引額
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal pointDiscountAmount;

    /**
     * 注文との関連（多対一）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orderId", insertable = false, updatable = false)
    private Order order;

    /**
     * 小計を計算するメソッド
     * Java 21の改善されたnull処理を活用
     */
    public void calculateSubtotal() {
        if (unitPrice != null && quantity != null) {
            this.subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        } else {
            this.subtotal = BigDecimal.ZERO;
        }
    }

    /**
     * 実際の支払額を計算（クーポン・ポイント割引適用後）
     * Java 21のOptionalとStreamを使用してより関数型的に記述
     */
    public BigDecimal getActualAmount() {
        var discounts = List.of(
            Optional.ofNullable(couponDiscountAmount),
            Optional.ofNullable(pointDiscountAmount)
        );
        
        var totalDiscount = discounts.stream()
            .filter(Optional::isPresent)
            .map(Optional::get)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return Optional.ofNullable(subtotal)
            .orElse(BigDecimal.ZERO)
            .subtract(totalDiscount)
            .max(BigDecimal.ZERO);
    }
}
