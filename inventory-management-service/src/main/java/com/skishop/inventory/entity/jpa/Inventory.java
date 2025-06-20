package com.skishop.inventory.entity.jpa;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 在庫エンティティ（PostgreSQL）
 * 商品の在庫数量と予約数量を管理
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "inventory", indexes = {
    @Index(name = "idx_inventory_product_id", columnList = "productId"),
    @Index(name = "idx_inventory_location_code", columnList = "locationCode"),
    @Index(name = "idx_inventory_status", columnList = "status")
})
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * 商品ID（MongoDBのProduct.idを参照）
     */
    @Column(nullable = false)
    private String productId;

    /**
     * 在庫数量
     */
    @Column(nullable = false)
    private Integer quantity;

    /**
     * 予約済み数量
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer reservedQuantity = 0;

    /**
     * 倉庫・場所コード
     */
    @Column(nullable = false)
    private String locationCode;

    /**
     * 在庫ステータス
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InventoryStatus status;

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
        if (reservedQuantity == null) {
            reservedQuantity = 0;
        }
        if (status == null) {
            status = InventoryStatus.IN_STOCK;
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
     * 利用可能在庫数量を計算
     */
    public Integer getAvailableQuantity() {
        return quantity - reservedQuantity;
    }

    /**
     * 在庫ステータス列挙型（Java 21モダン記法）
     */
    public enum InventoryStatus {
        IN_STOCK("在庫あり"),
        LOW_STOCK("在庫少"),
        OUT_OF_STOCK("在庫切れ"),
        DISCONTINUED("販売終了");

        private final String displayName;

        InventoryStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
        
        /**
         * 在庫数量に基づくステータス判定（Java 21のswitch式使用）
         */
        public static InventoryStatus fromQuantity(int availableQuantity, int lowStockThreshold) {
            if (availableQuantity <= 0) {
                return OUT_OF_STOCK;
            } else if (availableQuantity <= lowStockThreshold) {
                return LOW_STOCK;
            } else {
                return IN_STOCK;
            }
        }
    }
}
