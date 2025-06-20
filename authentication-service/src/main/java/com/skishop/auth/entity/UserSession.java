package com.skishop.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * User Session Entity
 * 
 * ユーザーセッション情報を管理するエンティティ
 */
@Entity
@Table(name = "user_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "user_id", nullable = false, insertable = false, updatable = false)
    private UUID userId;

    @Column(name = "session_token", unique = true, nullable = false, length = 255)
    private String sessionToken;

    @Column(name = "refresh_token", unique = true, nullable = false, length = 255)
    private String refreshToken;

    @Column(name = "device_info", columnDefinition = "TEXT")
    private String deviceInfo;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "last_accessed")
    private Instant lastAccessed;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * セッションが有効期限内かチェック
     */
    public boolean isValid() {
        return isActive && expiresAt.isAfter(Instant.now());
    }

    /**
     * セッションを無効化
     */
    public void invalidate() {
        this.isActive = false;
    }

    /**
     * 最終アクセス時刻を更新
     */
    public void updateLastAccessed() {
        this.lastAccessed = Instant.now();
    }
}
