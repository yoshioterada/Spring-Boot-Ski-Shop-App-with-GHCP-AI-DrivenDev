package com.skishop.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 推薦機能関連のDTO定義
 * Java 21のRecord機能を使用した不変データクラス群
 */
public final class RecommendationDto {

    // プライベートコンストラクタを追加してユーティリティクラスにする
    private RecommendationDto() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * 推薦リクエストDTO
     * 
     * @param limit 取得数制限（1-50、デフォルト10）
     * @param category カテゴリフィルタ
     * @param minPrice 最小価格
     * @param maxPrice 最大価格
     * @param excludeProductIds 除外商品ID
     * @param preferences ユーザー設定
     */
    public record RecommendationRequest(
        @Min(value = 1, message = "Limit must be at least 1")
        @Max(value = 50, message = "Limit cannot exceed 50")
        Integer limit,
        String category,
        Double minPrice,
        Double maxPrice,
        List<String> excludeProductIds,
        Map<String, Object> preferences
    ) {
        
        // レコードのコンパクトコンストラクタでバリデーションとデフォルト値設定
        public RecommendationRequest {
            if (limit == null) {
                limit = 10;
            }
            if (excludeProductIds == null) {
                excludeProductIds = List.of();
            }
            if (preferences == null) {
                preferences = Map.of();
            }
        }
        
        /**
         * 基本的な推薦リクエストを作成
         */
        public static RecommendationRequest defaultRequest() {
            return new RecommendationRequest(10, null, null, null, List.of(), Map.of());
        }
        
        /**
         * カテゴリ指定の推薦リクエストを作成
         */
        public static RecommendationRequest forCategory(String category, Integer limit) {
            return new RecommendationRequest(limit, category, null, null, List.of(), Map.of());
        }
        
        /**
         * 価格範囲指定の推薦リクエストを作成
         */
        public static RecommendationRequest withPriceRange(Double minPrice, Double maxPrice, Integer limit) {
            return new RecommendationRequest(limit, null, minPrice, maxPrice, List.of(), Map.of());
        }
    }

    /**
     * 推薦レスポンスDTO
     * 
     * @param recommendations 推薦商品リスト
     * @param strategy 使用した推薦戦略
     * @param metadata メタデータ
     * @param generatedAt 生成時刻
     */
    public record RecommendationResponse(
        List<ProductRecommendationDto> recommendations,
        String strategy,
        Map<String, Object> metadata,
        LocalDateTime generatedAt
    ) {
        
        public RecommendationResponse {
            if (recommendations == null) {
                recommendations = List.of();
            }
            if (metadata == null) {
                metadata = Map.of();
            }
            if (generatedAt == null) {
                generatedAt = LocalDateTime.now();
            }
        }
        
        /**
         * 推薦件数を取得
         */
        public int getRecommendationCount() {
            return recommendations.size();
        }
        
        /**
         * 平均スコアを計算
         */
        public double getAverageScore() {
            return recommendations.stream()
                .mapToDouble(ProductRecommendationDto::score)
                .average()
                .orElse(0.0);
        }
    }

    /**
     * 商品推薦詳細DTO
     * 
     * @param productId 商品ID
     * @param productName 商品名
     * @param category カテゴリ
     * @param price 価格
     * @param imageUrl 画像URL
     * @param score 推薦スコア
     * @param reason 推薦理由
     * @param features 特徴リスト
     */
    public record ProductRecommendationDto(
        String productId,
        String productName,
        String category,
        Double price,
        String imageUrl,
        Double score,
        String reason,
        List<String> features
    ) {
        
        public ProductRecommendationDto {
            if (features == null) {
                features = List.of();
            }
        }
        
        /**
         * 推薦レベルを判定（スコアベース）
         */
        public RecommendationLevel getRecommendationLevel() {
            if (score == null) return RecommendationLevel.LOW;
            
            if (score >= 0.8) {
                return RecommendationLevel.HIGH;
            } else if (score >= 0.6) {
                return RecommendationLevel.MEDIUM;
            } else {
                return RecommendationLevel.LOW;
            }
        }
    }
    
    /**
     * 推薦レベル列挙型（密封インターフェース）
     */
    public sealed interface RecommendationLevel 
        permits RecommendationLevel.High, RecommendationLevel.Medium, RecommendationLevel.Low {
        
        record High() implements RecommendationLevel {}
        record Medium() implements RecommendationLevel {}
        record Low() implements RecommendationLevel {}
        
        static final RecommendationLevel HIGH = new High();
        static final RecommendationLevel MEDIUM = new Medium();
        static final RecommendationLevel LOW = new Low();
    }

    /**
     * 検索リクエストDTO
     * 
     * @param query 検索クエリ
     * @param limit 取得件数制限（1-100、デフォルト20）
     * @param category カテゴリフィルタ
     * @param minPrice 最小価格
     * @param maxPrice 最大価格
     * @param filters その他フィルタ
     */
    public record SearchRequest(
        @NotBlank(message = "Query is required")
        String query,
        
        @Min(value = 1, message = "Limit must be at least 1")
        @Max(value = 100, message = "Limit cannot exceed 100")
        Integer limit,
        
        String category,
        Double minPrice,
        Double maxPrice,
        List<String> filters
    ) {
        
        public SearchRequest {
            if (limit == null) {
                limit = 20;
            }
            if (filters == null) {
                filters = List.of();
            }
        }
        
        /**
         * 基本検索リクエストを作成
         */
        public static SearchRequest of(String query) {
            return new SearchRequest(query, 20, null, null, null, List.of());
        }
        
        /**
         * カテゴリ指定検索リクエストを作成
         */
        public static SearchRequest withCategory(String query, String category) {
            return new SearchRequest(query, 20, category, null, null, List.of());
        }
    }

    /**
     * 検索レスポンスDTO
     * 
     * @param results 検索結果リスト
     * @param totalCount 総件数
     * @param query 検索クエリ
     * @param suggestions 検索候補
     * @param facets ファセット情報
     */
    public record SearchResponse(
        List<ProductSearchResultDto> results,
        Integer totalCount,
        String query,
        List<String> suggestions,
        Map<String, Object> facets
    ) {
        
        public SearchResponse {
            if (results == null) {
                results = List.of();
            }
            if (suggestions == null) {
                suggestions = List.of();
            }
            if (facets == null) {
                facets = Map.of();
            }
        }
        
        /**
         * 検索結果件数を取得
         */
        public int getResultCount() {
            return results.size();
        }
        
        /**
         * ページング情報がある場合のページ数計算
         */
        public int calculateTotalPages(int pageSize) {
            return (totalCount + pageSize - 1) / pageSize;
        }
    }

    /**
     * 商品検索結果詳細DTO
     * 
     * @param productId 商品ID
     * @param productName 商品名
     * @param description 商品説明
     * @param category カテゴリ
     * @param price 価格
     * @param imageUrl 画像URL
     * @param relevanceScore 関連度スコア
     * @param highlights ハイライト部分
     */
    public record ProductSearchResultDto(
        String productId,
        String productName,
        String description,
        String category,
        Double price,
        String imageUrl,
        Double relevanceScore,
        List<String> highlights
    ) {
        
        public ProductSearchResultDto {
            if (highlights == null) {
                highlights = List.of();
            }
        }
        
        /**
         * 関連度レベルを判定
         */
        public RelevanceLevel getRelevanceLevel() {
            if (relevanceScore == null) return RelevanceLevel.LOW;
            
            if (relevanceScore >= 0.9) {
                return RelevanceLevel.VERY_HIGH;
            } else if (relevanceScore >= 0.7) {
                return RelevanceLevel.HIGH;
            } else if (relevanceScore >= 0.5) {
                return RelevanceLevel.MEDIUM;
            } else {
                return RelevanceLevel.LOW;
            }
        }
    }
    
    /**
     * 関連度レベル列挙型（密封インターフェース）
     */
    public sealed interface RelevanceLevel 
        permits RelevanceLevel.VeryHigh, RelevanceLevel.High, 
                RelevanceLevel.Medium, RelevanceLevel.Low {
        
        record VeryHigh() implements RelevanceLevel {}
        record High() implements RelevanceLevel {}
        record Medium() implements RelevanceLevel {}
        record Low() implements RelevanceLevel {}
        
        static final RelevanceLevel VERY_HIGH = new VeryHigh();
        static final RelevanceLevel HIGH = new High();
        static final RelevanceLevel MEDIUM = new Medium();
        static final RelevanceLevel LOW = new Low();
    }
}
