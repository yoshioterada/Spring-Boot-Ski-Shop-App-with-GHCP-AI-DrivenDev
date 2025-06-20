package com.skishop.point.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * ティア定義エンティティ
 * ポイントシステムのティア階層を定義
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "tier_definitions")
public class TierDefinition {

    @Id
    @Column(length = 20)
    private String tierLevel;

    @Column(nullable = false, length = 50)
    private String tierName;

    @Column(nullable = false)
    private Integer minPointsRequired;

    @Column(nullable = false, precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal pointMultiplier = BigDecimal.valueOf(1.00);

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> benefits = Map.of();

    @Column(length = 20)
    private String nextTier;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
