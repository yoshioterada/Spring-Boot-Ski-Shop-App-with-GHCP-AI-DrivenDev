package com.skishop.point.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ユーザーティアエンティティ
 * ユーザーの現在のティア状態を管理
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user_tiers", indexes = {
    @Index(name = "idx_user_tiers_user_id", columnList = "userId", unique = true),
    @Index(name = "idx_user_tiers_tier_level", columnList = "tierLevel")
})
public class UserTier {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID userId;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String tierLevel = "bronze";

    @Column(nullable = false)
    @Builder.Default
    private Integer totalPointsEarned = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer currentPoints = 0;

    private LocalDateTime tierUpgradedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tierLevel", referencedColumnName = "tierLevel", insertable = false, updatable = false)
    private TierDefinition tierDefinition;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * ポイントを加算する
     */
    public void addPoints(Integer points) {
        this.currentPoints += points;
        this.totalPointsEarned += points;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * ポイントを減算する
     */
    public void deductPoints(Integer points) {
        if (this.currentPoints < points) {
            throw new IllegalArgumentException("Insufficient points");
        }
        this.currentPoints -= points;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * ティアをアップグレードする
     */
    public void upgradeTier(String newTierLevel) {
        this.tierLevel = newTierLevel;
        this.tierUpgradedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
