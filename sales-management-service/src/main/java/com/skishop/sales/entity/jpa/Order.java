package com.skishop.sales.entity.jpa;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 注文エンティティ（PostgreSQL）
 * 顧客の注文情報を管理
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_orders_customer_id", columnList = "customerId"),
    @Index(name = "idx_orders_status", columnList = "status"),
    @Index(name = "idx_orders_order_date", columnList = "orderDate"),
    @Index(name = "idx_orders_order_number", columnList = "orderNumber")
})
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * 注文番号（ユニーク）
     */
    @Column(nullable = false, unique = true, length = 50)
    private String orderNumber;

    /**
     * 顧客ID
     */
    @Column(nullable = false)
    private String customerId;

    /**
     * 注文日時
     */
    @Column(nullable = false)
    private LocalDateTime orderDate;

    /**
     * 注文ステータス
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    /**
     * 支払い状態
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus paymentStatus;

    /**
     * 支払い方法
     */
    @Column(nullable = false, length = 50)
    private String paymentMethod;

    /**
     * 小計金額
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotalAmount;

    /**
     * 税額
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal taxAmount;

    /**
     * 配送料
     */
    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal shippingFee = BigDecimal.ZERO;

    /**
     * 割引額
     */
    @Column(nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /**
     * 合計金額
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    /**
     * クーポンコード
     */
    @Column(length = 50)
    private String couponCode;

    /**
     * 使用ポイント数
     */
    @Builder.Default
    private Integer usedPoints = 0;

    /**
     * ポイント割引額
     */
    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal pointDiscountAmount = BigDecimal.ZERO;

    /**
     * 配送先住所
     */
    @Embedded
    private ShippingAddress shippingAddress;

    /**
     * 通貨コード
     */
    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currencyCode = "JPY";

    /**
     * 備考
     */
    @Column(length = 1000)
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
     * 注文アイテムリスト
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItem> items;

    /**
     * エンティティ作成前処理
     */
    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (orderDate == null) {
            orderDate = now;
        }
        updatedAt = now;
        if (status == null) {
            status = OrderStatus.PENDING;
        }
        if (currencyCode == null) {
            currencyCode = "JPY";
        }
        if (shippingFee == null) {
            shippingFee = BigDecimal.ZERO;
        }
        if (discountAmount == null) {
            discountAmount = BigDecimal.ZERO;
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
     * 注文ステータス列挙型
     * Java 21の拡張されたEnum機能を活用
     */
    public enum OrderStatus {
        PENDING("注文受付中", "お客様からの注文を受け付けました"),
        CONFIRMED("注文確定", "注文内容を確認し、処理を開始しました"),
        PROCESSING("処理中", "商品の準備を行っています"),
        SHIPPED("出荷済み", "商品を出荷しました"),
        DELIVERED("配送完了", "商品の配送が完了しました"),
        CANCELLED("キャンセル", "注文がキャンセルされました"),
        RETURNED("返品", "商品が返品されました");

        private final String displayName;
        private final String description;

        OrderStatus(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }

        /**
         * Java 21のSwitch式を使用してステータスカテゴリを判定
         */
        public StatusCategory getCategory() {
            return switch (this) {
                case PENDING, CONFIRMED -> StatusCategory.ACTIVE;
                case PROCESSING, SHIPPED -> StatusCategory.IN_PROGRESS;
                case DELIVERED -> StatusCategory.COMPLETED;
                case CANCELLED, RETURNED -> StatusCategory.TERMINATED;
            };
        }

        /**
         * ステータスカテゴリ列挙型
         */
        public enum StatusCategory {
            ACTIVE, IN_PROGRESS, COMPLETED, TERMINATED
        }
    }

    /**
     * 支払い状態列挙型
     * Java 21の拡張されたEnum機能を活用
     */
    public enum PaymentStatus {
        PENDING("支払い待ち", false),
        PAID("支払い完了", true),
        FAILED("支払い失敗", false),
        REFUNDED("返金完了", false),
        PARTIALLY_REFUNDED("部分返金", false);

        private final String displayName;
        private final boolean isSuccessful;

        PaymentStatus(String displayName, boolean isSuccessful) {
            this.displayName = displayName;
            this.isSuccessful = isSuccessful;
        }

        public String getDisplayName() {
            return displayName;
        }

        public boolean isSuccessful() {
            return isSuccessful;
        }

        /**
         * Java 21のSwitch式を使用して支払い状態の詳細を取得
         */
        public PaymentDetails getPaymentDetails() {
            return switch (this) {
                case PENDING -> new PaymentDetails("決済処理中です", "#FFA500", false);
                case PAID -> new PaymentDetails("お支払いが完了しました", "#008000", true);
                case FAILED -> new PaymentDetails("決済に失敗しました", "#FF0000", false);
                case REFUNDED -> new PaymentDetails("全額返金されました", "#0000FF", true);
                case PARTIALLY_REFUNDED -> new PaymentDetails("一部返金されました", "#800080", false);
            };
        }

        /**
         * 支払い詳細情報を保持するRecord
         */
        public record PaymentDetails(String message, String colorCode, boolean isFinal) {}
    }

    /**
     * 配送先住所の埋め込み可能クラス
     */
    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ShippingAddress {
        @Column(name = "shipping_postal_code", length = 10)
        private String postalCode;
        
        @Column(name = "shipping_prefecture", length = 20)
        private String prefecture;
        
        @Column(name = "shipping_city", length = 50)
        private String city;
        
        @Column(name = "shipping_address_line1", length = 100)
        private String addressLine1;
        
        @Column(name = "shipping_address_line2", length = 100)
        private String addressLine2;
        
        @Column(name = "shipping_recipient_name", length = 100)
        private String recipientName;
        
        @Column(name = "shipping_phone", length = 20)
        private String phone;
    }

    /**
     * 請求先住所の埋め込み可能クラス
     */
    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BillingAddress {
        @Column(name = "billing_postal_code", length = 10)
        private String postalCode;
        
        @Column(name = "billing_prefecture", length = 20)
        private String prefecture;
        
        @Column(name = "billing_city", length = 50)
        private String city;
        
        @Column(name = "billing_address_line1", length = 100)
        private String addressLine1;
        
        @Column(name = "billing_address_line2", length = 100)
        private String addressLine2;
        
        @Column(name = "billing_recipient_name", length = 100)
        private String recipientName;
        
        @Column(name = "billing_phone", length = 20)
        private String phone;
    }
}
