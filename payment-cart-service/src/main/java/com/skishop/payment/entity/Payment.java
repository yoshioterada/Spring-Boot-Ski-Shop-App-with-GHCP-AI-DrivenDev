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
 * 決済エンティティ（PostgreSQL）
 * 決済処理と履歴を管理
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_payments_user_id", columnList = "userId"),
    @Index(name = "idx_payments_cart_id", columnList = "cartId"),
    @Index(name = "idx_payments_status", columnList = "status"),
    @Index(name = "idx_payments_intent_id", columnList = "paymentIntentId")
})
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * ユーザーID
     */
    @Column(nullable = false)
    private UUID userId;

    /**
     * カートID
     */
    @Column(nullable = false)
    private UUID cartId;

    /**
     * 決済インテントID（Stripe等）
     */
    @Column(unique = true, length = 200)
    private String paymentIntentId;

    /**
     * 決済方法
     */
    @Column(nullable = false, length = 50)
    private String paymentMethod;

    /**
     * 決済金額
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    /**
     * 通貨コード
     */
    @Column(nullable = false, length = 3)
    private String currency;

    /**
     * 決済ステータス
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    /**
     * 決済ゲートウェイプロバイダー
     */
    @Column(nullable = false, length = 50)
    private String gatewayProvider;

    /**
     * ゲートウェイレスポンス（JSON形式）
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> gatewayResponse;

    /**
     * 失敗理由
     */
    @Column(length = 500)
    private String failureReason;

    /**
     * 返金額
     */
    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal refundedAmount = BigDecimal.ZERO;

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
     * 決済完了日時
     */
    private LocalDateTime completedAt;

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
        if (status == null) {
            status = PaymentStatus.PENDING;
        }
        if (currency == null) {
            currency = "JPY";
        }
    }

    /**
     * エンティティ更新前処理
     */
    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
        if (status == PaymentStatus.COMPLETED && completedAt == null) {
            completedAt = LocalDateTime.now();
        }
    }

    /**
     * 決済ステータス列挙型
     */
    public enum PaymentStatus {
        PENDING("処理中"),
        REQUIRES_ACTION("アクション必要"),
        CONFIRMED("確認済み"),
        COMPLETED("完了"),
        FAILED("失敗"),
        CANCELLED("キャンセル"),
        REFUNDED("返金済み"),
        PARTIALLY_REFUNDED("部分返金済み");

        private final String displayName;

        PaymentStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}
