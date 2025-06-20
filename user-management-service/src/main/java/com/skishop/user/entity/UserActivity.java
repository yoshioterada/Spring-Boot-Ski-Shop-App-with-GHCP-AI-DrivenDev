package com.skishop.user.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * ユーザーアクティビティエンティティ
 * ユーザーの行動履歴を記録
 */
@Entity
@Table(name = "user_activities")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 50)
    private ActivityType activityType;

    @Column(length = 500)
    private String description;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 1000)
    private String userAgent;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * アクティビティの種類
     */
    public enum ActivityType {
        LOGIN,              // ログイン
        LOGOUT,             // ログアウト
        PROFILE_UPDATE,     // プロフィール更新
        PASSWORD_CHANGE,    // パスワード変更
        EMAIL_VERIFY,       // メール認証
        PHONE_VERIFY,       // 電話番号認証
        PRODUCT_VIEW,       // 商品閲覧
        PRODUCT_SEARCH,     // 商品検索
        ORDER_CREATE,       // 注文作成
        ORDER_CANCEL,       // 注文キャンセル
        CART_ADD,           // カート追加
        CART_REMOVE,        // カート削除
        WISHLIST_ADD,       // ウィッシュリスト追加
        WISHLIST_REMOVE,    // ウィッシュリスト削除
        REVIEW_CREATE,      // レビュー投稿
        SUPPORT_CONTACT,    // サポート問い合わせ
        COUPON_USE,         // クーポン使用
        POINT_EARN,         // ポイント獲得
        POINT_USE,          // ポイント使用
        ACCOUNT_SUSPEND,    // アカウント停止
        ACCOUNT_RESTORE     // アカウント復旧
    }

    /**
     * メタデータをJSON形式で設定
     */
    public void setMetadataJson(String json) {
        this.metadata = json;
    }

    /**
     * ユーザーエージェントから簡易デバイス情報を取得
     */
    public String getSimplifiedDevice() {
        return switch (userAgent) {
            case null -> "Unknown";
            case String ua when ua.contains("Mobile") -> "Mobile";
            case String ua when ua.contains("Tablet") -> "Tablet";
            default -> "Desktop";
        };
    }

    /**
     * セキュリティ関連のアクティビティかチェック
     */
    public boolean isSecurityActivity() {
        return switch (activityType) {
            case LOGIN, LOGOUT, PASSWORD_CHANGE, EMAIL_VERIFY, 
                 PHONE_VERIFY, ACCOUNT_SUSPEND, ACCOUNT_RESTORE -> true;
            default -> false;
        };
    }
}
