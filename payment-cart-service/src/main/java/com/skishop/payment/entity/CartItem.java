package com.skishop.payment.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * カートアイテムエンティティ（PostgreSQL）
 * カートに含まれる商品の詳細情報を管理
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "cart_items", indexes = {
    @Index(name = "idx_cart_items_cart_id", columnList = "cartId"),
    @Index(name = "idx_cart_items_product_id", columnList = "productId")
})
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * カートID（外部キー）
     */
    @Column(nullable = false)
    private UUID cartId;

    /**
     * 商品ID
     */
    @Column(nullable = false)
    private UUID productId;

    /**
     * 数量
     */
    @Column(nullable = false)
    private Integer quantity;

    /**
     * 単価（カート追加時の価格）
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    /**
     * 小計（単価 × 数量）
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    /**
     * 商品詳細情報（JSON形式）
     * カート追加時の商品情報のスナップショット
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> productDetails;

    /**
     * 作成日時
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新日時
     */
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * カートとの関連（多対一）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cartId", insertable = false, updatable = false)
    private Cart cart;

    /**
     * エンティティ作成前処理
     */
    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        calculateTotalPrice();
    }

    /**
     * エンティティ更新前処理
     */
    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
        calculateTotalPrice();
    }

    /**
     * 小計を計算
     */
    public void calculateTotalPrice() {
        if (unitPrice != null && quantity != null) {
            totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));
        }
    }
}
