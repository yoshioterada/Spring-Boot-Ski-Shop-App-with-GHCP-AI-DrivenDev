package com.skishop.auth.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * User MFA (Multi-Factor Authentication) Entity
 * 
 * ユーザーの多要素認証設定を管理するエンティティ
 */
@Entity
@Table(name = "user_mfa")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserMFA {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "mfa_type", length = 50, nullable = false)
    private String mfaType;  // TOTP, SMS, EMAIL

    @Column(name = "secret_key", length = 255)
    private String secretKey;

    @Builder.Default
    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "backup_codes", columnDefinition = "TEXT")
    private List<String> backupCodes;

    @Column(name = "last_used")
    private Instant lastUsed;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * MFAが有効かチェック
     */
    public boolean isActive() {
        return isEnabled != null && isEnabled;
    }

    /**
     * Lombokが生成するisEnabledメソッドのエイリアス
     */
    public Boolean getIsEnabled() {
        return this.isEnabled;
    }

    /**
     * Lombokが生成するsetEnabledメソッドのエイリアス
     */
    public void setIsEnabled(Boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    /**
     * MFAを有効化
     */
    public void enable() {
        this.isEnabled = true;
    }

    /**
     * MFAを無効化
     */
    public void disable() {
        this.isEnabled = false;
    }

    /**
     * 最終使用時刻を更新
     */
    public void updateLastUsed() {
        this.lastUsed = Instant.now();
    }
}
