package com.skishop.sales.entity.jpa;

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
 * 返品エンティティ（PostgreSQL）
 * 商品の返品情報を管理
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "returns", indexes = {
    @Index(name = "idx_returns_order_id", columnList = "orderId"),
    @Index(name = "idx_returns_order_item_id", columnList = "orderItemId"),
    @Index(name = "idx_returns_return_number", columnList = "returnNumber"),
    @Index(name = "idx_returns_status", columnList = "status")
})
public class Return {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * 返品番号（ユニーク）
     */
    @Column(nullable = false, unique = true, length = 50)
    private String returnNumber;

    /**
     * 注文ID（外部キー）
     */
    @Column(nullable = false)
    private UUID orderId;

    /**
     * 注文明細ID（外部キー）
     */
    @Column(nullable = false)
    private UUID orderItemId;

    /**
     * 顧客ID
     */
    @Column(nullable = false, length = 100)
    private String customerId;

    /**
     * 返品理由
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReturnReason reason;

    /**
     * 返品理由の詳細
     */
    @Column(length = 1000)
    private String reasonDetail;

    /**
     * 返品数量
     */
    @Column(nullable = false)
    private Integer quantity;

    /**
     * 返金額
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal refundAmount;

    /**
     * 返品状態
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReturnStatus status;

    /**
     * 返品申請日時
     */
    @Column(nullable = false)
    private LocalDateTime requestedAt;

    /**
     * 承認日時
     */
    private LocalDateTime approvedAt;

    /**
     * 商品受領日時
     */
    private LocalDateTime receivedAt;

    /**
     * 返金完了日時
     */
    private LocalDateTime refundedAt;

    /**
     * 管理者メモ
     */
    @Column(length = 1000)
    private String adminNotes;

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

    /**
     * 注文明細との関連（多対一）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orderItemId", insertable = false, updatable = false)
    private OrderItem orderItem;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (requestedAt == null) {
            requestedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 返品理由
     */
    public enum ReturnReason {
        DEFECTIVE,        // 不良品
        WRONG_ITEM,       // 商品間違い
        SIZE_ISSUE,       // サイズ問題
        NOT_AS_DESCRIBED, // 説明と異なる
        DAMAGED_SHIPPING, // 配送時破損
        CUSTOMER_CHANGED_MIND, // 顧客都合
        OTHER             // その他
    }

    /**
     * 返品状態
     */
    public enum ReturnStatus {
        REQUESTED,    // 申請中
        APPROVED,     // 承認済み
        REJECTED,     // 却下
        RECEIVED,     // 商品受領済み
        REFUNDED,     // 返金完了
        CANCELLED     // キャンセル
    }
}
