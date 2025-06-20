package com.skishop.point.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ポイント取引エンティティ
 * ポイントの獲得・使用・失効の履歴を管理
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "point_transactions", indexes = {
    @Index(name = "idx_point_transactions_user_created", columnList = "userId, createdAt"),
    @Index(name = "idx_point_transactions_expires_at", columnList = "expiresAt"),
    @Index(name = "idx_point_transactions_reference", columnList = "referenceId"),
    @Index(name = "idx_point_transactions_type", columnList = "transactionType")
})
public class PointTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType transactionType;

    @Column(nullable = false)
    private Integer amount;

    @Column(nullable = false)
    private Integer balanceAfter;

    @Column(nullable = false, length = 100)
    private String reason;

    @Column(length = 100)
    private String referenceId;

    private LocalDateTime expiresAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isExpired = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    /**
     * 取引タイプ列挙型
     */
    public enum TransactionType {
        EARNED("獲得"),
        REDEEMED("使用"),
        EXPIRED("失効"),
        TRANSFERRED_IN("受取"),
        TRANSFERRED_OUT("送付"),
        BONUS("ボーナス"),
        ADJUSTMENT("調整");

        private final String displayName;

        TransactionType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * ポイントを失効させる
     */
    public void expire() {
        this.isExpired = true;
    }

    /**
     * 有効期限チェック
     */
    public boolean isExpiring(int days) {
        if (expiresAt == null) {
            return false;
        }
        return expiresAt.isBefore(LocalDateTime.now().plusDays(days));
    }
}
