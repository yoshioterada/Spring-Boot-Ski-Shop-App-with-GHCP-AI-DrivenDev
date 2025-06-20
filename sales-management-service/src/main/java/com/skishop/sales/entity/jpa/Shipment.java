package com.skishop.sales.entity.jpa;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 配送エンティティ（PostgreSQL）
 * 注文の配送情報を管理
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "shipments", indexes = {
    @Index(name = "idx_shipments_order_id", columnList = "orderId"),
    @Index(name = "idx_shipments_tracking_number", columnList = "trackingNumber"),
    @Index(name = "idx_shipments_status", columnList = "status")
})
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * 注文ID（外部キー）
     */
    @Column(nullable = false)
    private UUID orderId;

    /**
     * 配送業者
     */
    @Column(nullable = false, length = 100)
    private String carrier;

    /**
     * 追跡番号
     */
    @Column(length = 100)
    private String trackingNumber;

    /**
     * 配送状態
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShipmentStatus status;

    /**
     * 配送先住所
     */
    @Embedded
    private ShippingAddress shippingAddress;

    /**
     * 発送日時
     */
    private LocalDateTime shippedAt;

    /**
     * 配達予定日時
     */
    private LocalDateTime estimatedDeliveryAt;

    /**
     * 配達完了日時
     */
    private LocalDateTime deliveredAt;

    /**
     * 備考
     */
    @Column(length = 500)
    private String notes;

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
     * 注文との関連（多対一）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orderId", insertable = false, updatable = false)
    private Order order;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 配送状態
     */
    public enum ShipmentStatus {
        PREPARING,     // 準備中
        SHIPPED,       // 発送済み
        IN_TRANSIT,    // 配送中
        OUT_FOR_DELIVERY, // 配達中
        DELIVERED,     // 配達完了
        FAILED_DELIVERY,  // 配達失敗
        RETURNED       // 返送
    }

    /**
     * 配送先住所
     */
    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShippingAddress {
        
        @Column(name = "shipping_postal_code", length = 10)
        private String postalCode;
        
        @Column(name = "shipping_prefecture", length = 50)
        private String prefecture;
        
        @Column(name = "shipping_city", length = 100)
        private String city;
        
        @Column(name = "shipping_address_line1", length = 200)
        private String addressLine1;
        
        @Column(name = "shipping_address_line2", length = 200)
        private String addressLine2;
        
        @Column(name = "shipping_recipient_name", length = 100)
        private String recipientName;
        
        @Column(name = "shipping_phone_number", length = 20)
        private String phoneNumber;
    }
}
