package com.skishop.inventory.entity.jpa;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 価格エンティティ（PostgreSQL）
 * 商品の価格情報を管理
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "prices", indexes = {
    @Index(name = "idx_prices_product_id", columnList = "productId"),
    @Index(name = "idx_prices_active", columnList = "isActive"),
    @Index(name = "idx_prices_sale_dates", columnList = "saleStartDate, saleEndDate")
})
public class Price {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * 商品ID（MongoDBのProduct.idを参照）
     */
    @Column(nullable = false)
    private String productId;

    /**
     * 通常価格
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal regularPrice;

    /**
     * セール価格
     */
    @Column(precision = 10, scale = 2)
    private BigDecimal salePrice;

    /**
     * セール開始日時
     */
    private LocalDateTime saleStartDate;

    /**
     * セール終了日時
     */
    private LocalDateTime saleEndDate;

    /**
     * 通貨コード
     */
    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currencyCode = "JPY";

    /**
     * アクティブ状態
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

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
     * 作成者
     */
    private String createdBy;

    /**
     * 更新者
     */
    private String updatedBy;

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
        if (isActive == null) {
            isActive = true;
        }
        if (currencyCode == null) {
            currencyCode = "JPY";
        }
    }

    /**
     * エンティティ更新前処理
     */
    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 現在の有効価格を取得
     */
    public BigDecimal getCurrentPrice() {
        LocalDateTime now = LocalDateTime.now();
        if (salePrice != null && 
            saleStartDate != null && 
            saleEndDate != null &&
            !now.isBefore(saleStartDate) && 
            !now.isAfter(saleEndDate)) {
            return salePrice;
        }
        return regularPrice;
    }

    /**
     * セール中かどうかを判定
     */
    public boolean isOnSale() {
        LocalDateTime now = LocalDateTime.now();
        return salePrice != null && 
               saleStartDate != null && 
               saleEndDate != null &&
               !now.isBefore(saleStartDate) && 
               !now.isAfter(saleEndDate);
    }
}
