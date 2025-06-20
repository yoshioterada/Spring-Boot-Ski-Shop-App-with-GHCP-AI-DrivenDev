package com.skishop.ai.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 商品推奨 エンティティ
 * 
 * <p>Java 21のrecord機能を使用した不変データクラス</p>
 * <p>AI推奨エンジンによって生成された商品推奨を保存する</p>
 * 
 * @param recommendationId 推奨ID（MongoDB _id）
 * @param userId ユーザーID
 * @param productId 商品ID
 * @param recommendationType 推奨アルゴリズムタイプ
 * @param score 推奨スコア（0.0-1.0）
 * @param reason 推奨理由
 * @param features 推奨に使用された特徴量
 * @param relatedProducts 関連商品ID一覧
 * @param clicked クリック済みフラグ
 * @param purchased 購入済みフラグ
 * @param clickedAt クリック日時
 * @param purchasedAt 購入日時
 * @param createdAt 作成日時
 * @param expiresAt 有効期限
 * 
 * @since 1.0.0
 */
@Document(collection = "product_recommendations")
public record ProductRecommendation(
    @Id
    String recommendationId,
    String userId,
    String productId,
    RecommendationType recommendationType,
    Double score,
    String reason,
    Map<String, Object> features,
    List<String> relatedProducts,
    boolean clicked,
    boolean purchased,
    LocalDateTime clickedAt,
    LocalDateTime purchasedAt,
    LocalDateTime createdAt,
    LocalDateTime expiresAt
) {
    
    /**
     * 新規推奨作成用ファクトリメソッド
     */
    public static ProductRecommendation create(
            String userId,
            String productId,
            RecommendationType type,
            Double score,
            String reason) {
        var now = LocalDateTime.now();
        return new ProductRecommendation(
            null, // MongoDB generates ID
            userId,
            productId,
            type,
            score,
            reason,
            Map.of(),
            List.of(),
            false,
            false,
            null,
            null,
            now,
            now.plusDays(30) // 30日間有効
        );
    }
    
    /**
     * クリック状態に更新
     */
    public ProductRecommendation withClicked() {
        return new ProductRecommendation(
            recommendationId,
            userId,
            productId,
            recommendationType,
            score,
            reason,
            features,
            relatedProducts,
            true,
            purchased,
            LocalDateTime.now(),
            purchasedAt,
            createdAt,
            expiresAt
        );
    }
    
    /**
     * 購入状態に更新
     */
    public ProductRecommendation withPurchased() {
        return new ProductRecommendation(
            recommendationId,
            userId,
            productId,
            recommendationType,
            score,
            reason,
            features,
            relatedProducts,
            clicked,
            true,
            clickedAt,
            LocalDateTime.now(),
            createdAt,
            expiresAt
        );
    }
}
