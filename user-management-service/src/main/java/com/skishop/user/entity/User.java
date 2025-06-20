package com.skishop.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * ユーザーエンティティ
 * スキーショップのユーザー情報を管理する
 */
@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(unique = true, nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserStatus status;

    @Builder.Default
    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified = false;

    @Builder.Default
    @Column(name = "phone_verified", nullable = false)
    private Boolean phoneVerified = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = true)
    private Role role;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<UserPreference> preferences;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private Set<UserActivity> activities;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * ユーザーの性別
     */
    public enum Gender {
        MALE,
        FEMALE,
        OTHER
    }

    /**
     * ユーザーのステータス
     */
    public enum UserStatus {
        PENDING_VERIFICATION,  // メール認証待ち
        ACTIVE,               // アクティブ
        INACTIVE,             // 非アクティブ
        SUSPENDED,            // 停止
        DELETED               // 削除済み
    }

    /**
     * デフォルトコンストラクタでのデフォルト値設定
     */
    @PrePersist
    private void prePersist() {
        if (this.status == null) {
            this.status = UserStatus.PENDING_VERIFICATION;
        }
        if (this.emailVerified == null) {
            this.emailVerified = false;
        }
        if (this.phoneVerified == null) {
            this.phoneVerified = false;
        }
    }

    /**
     * フルネームを取得
     */
    public String getFullName() {
        return "%s %s".formatted(firstName, lastName);
    }

    /**
     * メール認証済みかチェック
     */
    public boolean isEmailVerified() {
        return Boolean.TRUE.equals(emailVerified);
    }

    /**
     * 電話番号認証済みかチェック
     */
    public boolean isPhoneVerified() {
        return Boolean.TRUE.equals(phoneVerified);
    }

    /**
     * アクティブなユーザーかチェック
     */
    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }
}
