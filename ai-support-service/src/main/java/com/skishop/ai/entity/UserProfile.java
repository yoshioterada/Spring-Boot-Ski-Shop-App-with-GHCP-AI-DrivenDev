package com.skishop.ai.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * ユーザープロファイル エンティティ
 * 
 * <p>Java 21のrecord機能を使用した不変データクラス</p>
 * <p>MongoDB Documentとして永続化され、AI推奨エンジンの入力として使用される</p>
 * 
 * @param userId ユーザーID（MongoDB _id）
 * @param preferences ユーザー設定・好み
 * @param purchaseHistory 購入履歴（商品ID一覧）
 * @param viewedProducts 閲覧履歴（商品ID一覧）
 * @param searchHistory 検索履歴
 * @param categoryPreferences カテゴリ別好み度スコア
 * @param loyaltyTier ロイヤリティティア
 * @param totalSpent 総購入金額
 * @param lastActivity 最終活動日時
 * @param favoriteCategories お気に入りカテゴリ
 * @param behaviorMetrics 行動メトリクス
 * @param createdAt 作成日時
 * @param updatedAt 更新日時
 * 
 * @since 1.0.0
 */
@Document(collection = "user_profiles")
public record UserProfile(
    @Id
    String userId,
    Map<String, Object> preferences,
    List<String> purchaseHistory,
    List<String> viewedProducts,
    List<String> searchHistory,
    Map<String, Double> categoryPreferences,
    String loyaltyTier,
    Double totalSpent,
    LocalDateTime lastActivity,
    List<String> favoriteCategories,
    Map<String, Object> behaviorMetrics,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    
    /**
     * 新規ユーザープロファイル作成用ファクトリメソッド
     */
    public static UserProfile createNew(String userId) {
        var now = LocalDateTime.now();
        return new UserProfile(
            userId,
            Map.of(),
            List.of(),
            List.of(),
            List.of(),
            Map.of(),
            "BRONZE",
            0.0,
            now,
            List.of(),
            Map.of(),
            now,
            now
        );
    }
    
    /**
     * プロファイル更新用ファクトリメソッド
     */
    public UserProfile withUpdatedActivity() {
        return new UserProfile(
            userId,
            preferences,
            purchaseHistory,
            viewedProducts,
            searchHistory,
            categoryPreferences,
            loyaltyTier,
            totalSpent,
            LocalDateTime.now(), // lastActivity更新
            favoriteCategories,
            behaviorMetrics,
            createdAt,
            LocalDateTime.now()  // updatedAt更新
        );
    }
}
