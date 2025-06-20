package com.skishop.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * User Role Entity
 * 
 * ユーザーのロール・権限情報を管理するエンティティ
 */
@Entity
@Table(name = "user_roles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRole {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "role_name", length = 100, nullable = false)
    private String roleName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "permissions", columnDefinition = "TEXT")
    private Map<String, Object> permissions;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    /**
     * ロールが有効かチェック
     */
    public boolean isValidRole() {
        return Boolean.TRUE.equals(isActive) && 
               (expiresAt == null || expiresAt.isAfter(Instant.now()));
    }

    /**
     * ロールを無効化
     */
    public void deactivate() {
        this.isActive = false;
    }
}
