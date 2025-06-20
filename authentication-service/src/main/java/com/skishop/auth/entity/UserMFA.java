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
    
    // Added userId field for builder compatibility
    @Column(name = "user_id", insertable = false, updatable = false)
    private UUID userId;

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

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

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
    
    // Manual getter and setter methods since Lombok may not be working properly
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    
    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
    
    public List<String> getBackupCodes() { return backupCodes; }
    public void setBackupCodes(List<String> backupCodes) { this.backupCodes = backupCodes; }
    
    public boolean isEnabled() { return isEnabled; }
    public void setEnabled(boolean enabled) { this.isEnabled = enabled; }
    
    public Instant getLastUsed() { return lastUsed; }
    public void setLastUsed(Instant lastUsed) { this.lastUsed = lastUsed; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    
    // Manual builder method since Lombok may not be working properly
    public static UserMFABuilder builder() {
        return new UserMFABuilder();
    }
    
    // Inner Builder class
    public static class UserMFABuilder {
        private UUID id;
        private UUID userId;
        private String secretKey;
        private List<String> backupCodes;
        private boolean isEnabled = false;
        private Instant lastUsed;
        private Instant createdAt;
        private Instant updatedAt;
        private Long version;
        
        public UserMFABuilder id(UUID id) { this.id = id; return this; }
        public UserMFABuilder userId(UUID userId) { this.userId = userId; return this; }
        public UserMFABuilder secretKey(String secretKey) { this.secretKey = secretKey; return this; }
        public UserMFABuilder backupCodes(List<String> backupCodes) { this.backupCodes = backupCodes; return this; }
        public UserMFABuilder isEnabled(boolean isEnabled) { this.isEnabled = isEnabled; return this; }
        public UserMFABuilder lastUsed(Instant lastUsed) { this.lastUsed = lastUsed; return this; }
        public UserMFABuilder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public UserMFABuilder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
        public UserMFABuilder version(Long version) { this.version = version; return this; }
        
        public UserMFA build() {
            UserMFA userMFA = new UserMFA();
            userMFA.setId(this.id);
            userMFA.setUserId(this.userId);
            userMFA.setSecretKey(this.secretKey);
            userMFA.setBackupCodes(this.backupCodes);
            userMFA.setEnabled(this.isEnabled);
            userMFA.setLastUsed(this.lastUsed);
            userMFA.setCreatedAt(this.createdAt);
            userMFA.setUpdatedAt(this.updatedAt);
            userMFA.setVersion(this.version);
            return userMFA;
        }
    }
}
