package com.skishop.ai.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 分析機能用DTO定義
 * 
 * <p>Java 21のRecord型を活用したAI分析機能のデータ転送オブジェクト</p>
 * <p>Sealed Interfaceと組み合わせて型安全性を確保</p>
 * 
 * @since 1.0.0
 */
public class AnalyticsDto {
    
    /**
     * ユーザー行動分析レスポンス
     */
    @Schema(description = "ユーザー行動分析結果")
    public record UserBehaviorResponse(
            @Schema(description = "ユーザーID") UUID userId,
            @Schema(description = "分析期間") AnalysisPeriod period,
            @Schema(description = "行動パターン") BehaviorPatterns behaviorPatterns,
            @Schema(description = "エンゲージメント指標") EngagementMetrics engagement,
            @Schema(description = "購買傾向") PurchasePatterns purchasePatterns,
            @Schema(description = "推奨事項") List<Recommendation> recommendations,
            @Schema(description = "分析メタデータ") AnalysisMetadata metadata
    ) {
        /**
         * ユーザー行動パターン
         */
        public record BehaviorPatterns(
                @Schema(description = "平均セッション時間(分)") double avgSessionDuration,
                @Schema(description = "ページビュー数") int totalPageViews,
                @Schema(description = "検索回数") int searchCount,
                @Schema(description = "カート追加回数") int cartAdditions,
                @Schema(description = "コンバージョン率") double conversionRate,
                @Schema(description = "最も活発な時間帯") List<String> peakActivityHours,
                @Schema(description = "使用デバイス分布") Map<String, Integer> deviceDistribution
        ) {}
        
        /**
         * エンゲージメント指標
         */
        public record EngagementMetrics(
                @Schema(description = "エンゲージメントスコア") double score,
                @Schema(description = "リピート率") double repeatRate,
                @Schema(description = "離脱率") double bounceRate,
                @Schema(description = "ロイヤルティレベル") String loyaltyLevel,
                @Schema(description = "インタラクション数") int totalInteractions,
                @Schema(description = "ソーシャルシェア数") int socialShares
        ) {}
        
        /**
         * 購買傾向
         */
        public record PurchasePatterns(
                @Schema(description = "平均注文金額") BigDecimal avgOrderValue,
                @Schema(description = "購買頻度") double purchaseFrequency,
                @Schema(description = "好みカテゴリ") List<String> preferredCategories,
                @Schema(description = "価格感度") String priceSensitivity,
                @Schema(description = "季節性") Map<String, Double> seasonality,
                @Schema(description = "最終購買日") @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime lastPurchase
        ) {}
    }
    
    /**
     * 商品感情分析レスポンス
     */
    @Schema(description = "商品感情分析結果")
    public record SentimentAnalysisResponse(
            @Schema(description = "商品ID") String productId,
            @Schema(description = "分析期間") AnalysisPeriod period,
            @Schema(description = "全体感情スコア") SentimentScore overallSentiment,
            @Schema(description = "レビュー分析") ReviewAnalysis reviewAnalysis,
            @Schema(description = "感情推移") List<SentimentTrend> sentimentTrends,
            @Schema(description = "キーワード分析") KeywordAnalysis keywordAnalysis,
            @Schema(description = "分析メタデータ") AnalysisMetadata metadata
    ) {
        /**
         * 感情スコア
         */
        public record SentimentScore(
                @Schema(description = "ポジティブ比率") double positive,
                @Schema(description = "ネガティブ比率") double negative,
                @Schema(description = "ニュートラル比率") double neutral,
                @Schema(description = "総合スコア(-1.0 to 1.0)") double overallScore,
                @Schema(description = "信頼度") double confidence
        ) {}
        
        /**
         * レビュー分析
         */
        public record ReviewAnalysis(
                @Schema(description = "総レビュー数") int totalReviews,
                @Schema(description = "平均評価") double averageRating,
                @Schema(description = "評価項目別スコア") Map<String, Double> categoryScores,
                @Schema(description = "ポジティブキーワード") List<String> positiveKeywords,
                @Schema(description = "ネガティブキーワード") List<String> negativeKeywords
        ) {}
        
        /**
         * 感情推移
         */
        public record SentimentTrend(
                @Schema(description = "日付") @JsonFormat(pattern = "yyyy-MM-dd") LocalDate date,
                @Schema(description = "感情スコア") double sentimentScore,
                @Schema(description = "レビュー数") int reviewCount
        ) {}
        
        /**
         * キーワード分析
         */
        public record KeywordAnalysis(
                @Schema(description = "ポジティブキーワード") List<String> positiveKeywords,
                @Schema(description = "ネガティブキーワード") List<String> negativeKeywords,
                @Schema(description = "キーワード頻度") Map<String, Integer> keywordFrequency
        ) {}
    }
    
    /**
     * トレンド分析レスポンス
     */
    @Schema(description = "トレンド分析結果")
    public record TrendAnalysisResponse(
            @Schema(description = "分析期間") AnalysisPeriod period,
            @Schema(description = "トレンドタイプ") String trendType,
            @Schema(description = "トレンドデータ") List<TrendDataPoint> trendData,
            @Schema(description = "トレンド洞察") List<TrendInsight> insights,
            @Schema(description = "トレンド予測") List<TrendPrediction> predictions,
            @Schema(description = "分析メタデータ") AnalysisMetadata metadata
    ) {
        /**
         * トレンドデータポイント
         */
        public record TrendDataPoint(
                @Schema(description = "カテゴリ/時点") String category,
                @Schema(description = "値") double value,
                @Schema(description = "追加属性") Map<String, Object> attributes
        ) {}
        
        /**
         * トレンド洞察
         */
        public record TrendInsight(
                @Schema(description = "洞察タイプ") String type,
                @Schema(description = "説明") String description,
                @Schema(description = "影響度") String impact,
                @Schema(description = "信頼度") double confidence
        ) {}
        
        /**
         * トレンド予測
         */
        public record TrendPrediction(
                @Schema(description = "予測日付") @JsonFormat(pattern = "yyyy-MM-dd") LocalDate date,
                @Schema(description = "予測値") double predictedValue,
                @Schema(description = "信頼度") double confidence,
                @Schema(description = "下限値") double lowerBound,
                @Schema(description = "上限値") double upperBound
        ) {}
    }
    
    /**
     * 共通レコード定義
     */
    
    /**
     * 分析期間
     */
    public record AnalysisPeriod(
            @Schema(description = "開始日") @JsonFormat(pattern = "yyyy-MM-dd") LocalDateTime startDate,
            @Schema(description = "終了日") @JsonFormat(pattern = "yyyy-MM-dd") LocalDateTime endDate,
            @Schema(description = "期間タイプ") String timeRange,
            @Schema(description = "期間(日数)") int days
    ) {}
    
    /**
     * 推奨事項
     */
    public record Recommendation(
            @Schema(description = "推奨タイトル") String title,
            @Schema(description = "推奨内容") String description,
            @Schema(description = "優先度") String priority,
            @Schema(description = "アクション") List<String> actions
    ) {}
    
    /**
     * 分析メタデータ
     */
    public record AnalysisMetadata(
            @Schema(description = "分析モデル名") String modelName,
            @Schema(description = "信頼度") double confidence,
            @Schema(description = "生成時刻") @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime generatedAt,
            @Schema(description = "追加パラメータ") Map<String, Object> parameters
    ) {}
}
