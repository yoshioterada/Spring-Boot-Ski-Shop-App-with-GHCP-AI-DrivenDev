package com.skishop.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ユーザー設定エンティティ
 * ユーザーごとの個別設定を管理
 */
@Entity
@Table(name = "user_preferences")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "pref_key", nullable = false, length = 100)
    private String prefKey;

    @Column(name = "pref_value", columnDefinition = "TEXT")
    private String prefValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "pref_type", nullable = false, length = 20)
    private PreferenceType prefType;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 設定のデータ型
     */
    public enum PreferenceType {
        STRING,
        INTEGER,
        BOOLEAN,
        JSON,
        DECIMAL
    }

    /**
     * 事前定義された設定キー
     */
    public static final String LANGUAGE = "language";
    public static final String TIMEZONE = "timezone";
    public static final String CURRENCY = "currency";
    public static final String EMAIL_NOTIFICATIONS = "email_notifications";
    public static final String PUSH_NOTIFICATIONS = "push_notifications";
    public static final String NEWSLETTER_SUBSCRIBED = "newsletter_subscribed";
    public static final String PREFERRED_BRANDS = "preferred_brands";
    public static final String SKI_LEVEL = "ski_level";
    public static final String EQUIPMENT_PREFERENCES = "equipment_preferences";

    /**
     * Boolean値として取得
     */
    public Boolean getBooleanValue() {
        return (prefType == PreferenceType.BOOLEAN && prefValue != null) 
            ? Boolean.parseBoolean(prefValue) 
            : null;
    }

    /**
     * Integer値として取得
     */
    public Integer getIntegerValue() {
        if (prefType != PreferenceType.INTEGER || prefValue == null) {
            return null;
        }
        try {
            return Integer.parseInt(prefValue);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * String値として取得
     */
    public String getStringValue() {
        return prefValue;
    }
}
