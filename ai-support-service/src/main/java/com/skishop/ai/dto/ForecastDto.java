package com.skishop.ai.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 予測機能用DTO定義
 * 
 * <p>Java 21のRecord型を活用したAI予測機能のデータ転送オブジェクト</p>
 * <p>需要予測、価格予測、在庫予測などの機能に対応</p>
 * 
 * @since 1.0.0
 */
public class ForecastDto {
    
    /**
     * 需要予測レスポンス
     */
    @Schema(description = "需要予測結果")
    public record DemandForecastResponse(
            @Schema(description = "商品ID") String productId,
            @Schema(description = "予測期間") ForecastPeriod period,
            @Schema(description = "予測モデル") String model,
            @Schema(description = "需要予測データ") List<DemandForecastPoint> forecastData,
            @Schema(description = "統計情報") ForecastStatistics statistics,
            @Schema(description = "信頼区間") ConfidenceInterval confidenceInterval,
            @Schema(description = "アラート") List<ForecastAlert> alerts,
            @Schema(description = "メタデータ") ForecastMetadata metadata
    ) {
        /**
         * 需要予測ポイント
         */
        public record DemandForecastPoint(
                @Schema(description = "日付") @JsonFormat(pattern = "yyyy-MM-dd") LocalDate date,
                @Schema(description = "予測需要数") int predictedDemand,
                @Schema(description = "信頼度") double confidence,
                @Schema(description = "季節性要因") double seasonalityFactor,
                @Schema(description = "トレンド要因") double trendFactor
        ) {}
    }
    
    /**
     * 価格予測レスポンス
     */
    @Schema(description = "価格予測結果")
    public record PriceForecastResponse(
            @Schema(description = "商品ID") String productId,
            @Schema(description = "現在価格") BigDecimal currentPrice,
            @Schema(description = "推奨価格") BigDecimal recommendedPrice,
            @Schema(description = "目標利益率") double targetMargin,
            @Schema(description = "価格シナリオ") List<PriceScenario> priceScenarios,
            @Schema(description = "競合分析") CompetitorAnalysis competitorAnalysis,
            @Schema(description = "価格感度分析") PriceSensitivityAnalysis sensitivity,
            @Schema(description = "予測収益") RevenueForecast revenueForecast,
            @Schema(description = "メタデータ") ForecastMetadata metadata
    ) {
        /**
         * 価格シナリオ
         */
        public record PriceScenario(
                @Schema(description = "シナリオ名") String scenarioName,
                @Schema(description = "価格") BigDecimal price,
                @Schema(description = "予測売上数") int forecastSales,
                @Schema(description = "予測収益") BigDecimal forecastRevenue,
                @Schema(description = "利益率") double marginRate,
                @Schema(description = "成功確率") double successProbability
        ) {}
        
        /**
         * 競合分析
         */
        public record CompetitorAnalysis(
                @Schema(description = "競合平均価格") BigDecimal avgCompetitorPrice,
                @Schema(description = "最低競合価格") BigDecimal minCompetitorPrice,
                @Schema(description = "最高競合価格") BigDecimal maxCompetitorPrice,
                @Schema(description = "市場ポジション") String marketPosition,
                @Schema(description = "価格優位性") double priceAdvantage
        ) {}
        
        /**
         * 価格感度分析
         */
        public record PriceSensitivityAnalysis(
                @Schema(description = "価格弾性") double priceElasticity,
                @Schema(description = "最適価格帯") PriceRange optimalPriceRange,
                @Schema(description = "顧客セグメント別感度") Map<String, Double> segmentSensitivity
        ) {}
        
        /**
         * 価格帯
         */
        public record PriceRange(
                @Schema(description = "最低価格") BigDecimal minPrice,
                @Schema(description = "最高価格") BigDecimal maxPrice,
                @Schema(description = "推奨価格") BigDecimal recommendedPrice
        ) {}
        
        /**
         * 収益予測
         */
        public record RevenueForecast(
                @Schema(description = "30日予測収益") BigDecimal revenue30Days,
                @Schema(description = "90日予測収益") BigDecimal revenue90Days,
                @Schema(description = "年間予測収益") BigDecimal revenueAnnual,
                @Schema(description = "ROI予測") double forecastROI
        ) {}
    }
    
    /**
     * 在庫予測レスポンス
     */
    @Schema(description = "在庫予測結果")
    public record InventoryForecastResponse(
            @Schema(description = "商品ID") String productId,
            @Schema(description = "現在在庫数") int currentStock,
            @Schema(description = "在庫予測データ") List<InventoryForecastPoint> forecastData,
            @Schema(description = "再注文推奨") ReorderRecommendation reorderRecommendation,
            @Schema(description = "在庫最適化") InventoryOptimization optimization,
            @Schema(description = "リスク分析") InventoryRiskAnalysis riskAnalysis,
            @Schema(description = "メタデータ") ForecastMetadata metadata
    ) {
        /**
         * 在庫予測ポイント
         */
        public record InventoryForecastPoint(
                @Schema(description = "日付") @JsonFormat(pattern = "yyyy-MM-dd") LocalDate date,
                @Schema(description = "予測在庫数") int predictedStock,
                @Schema(description = "予測需要") int predictedDemand,
                @Schema(description = "在庫切れリスク") double stockoutRisk,
                @Schema(description = "推奨アクション") String recommendedAction
        ) {}
        
        /**
         * 再注文推奨
         */
        public record ReorderRecommendation(
                @Schema(description = "再注文日") @JsonFormat(pattern = "yyyy-MM-dd") LocalDate reorderDate,
                @Schema(description = "推奨注文数量") int recommendedQuantity,
                @Schema(description = "緊急度") String urgency,
                @Schema(description = "理由") String reason,
                @Schema(description = "コスト予測") BigDecimal estimatedCost
        ) {}
        
        /**
         * 在庫最適化
         */
        public record InventoryOptimization(
                @Schema(description = "最適在庫レベル") int optimalStockLevel,
                @Schema(description = "安全在庫") int safetyStock,
                @Schema(description = "経済的注文量") int economicOrderQuantity,
                @Schema(description = "回転率目標") double targetTurnoverRate,
                @Schema(description = "削減可能コスト") BigDecimal potentialCostSavings
        ) {}
        
        /**
         * 在庫リスク分析
         */
        public record InventoryRiskAnalysis(
                @Schema(description = "在庫切れリスク") double stockoutRisk,
                @Schema(description = "過剰在庫リスク") double overstockRisk,
                @Schema(description = "陳腐化リスク") double obsolescenceRisk,
                @Schema(description = "総合リスクスコア") double overallRiskScore,
                @Schema(description = "緩和策") List<String> mitigationStrategies
        ) {}
    }
    
    /**
     * 季節性予測レスポンス
     */
    @Schema(description = "季節性予測結果")
    public record SeasonalForecastResponse(
            @Schema(description = "カテゴリ") String category,
            @Schema(description = "予測年") int year,
            @Schema(description = "地域") String region,
            @Schema(description = "月別予測") List<MonthlyForecast> monthlyForecasts,
            @Schema(description = "季節パターン") SeasonalPattern seasonalPattern,
            @Schema(description = "ピーク分析") PeakAnalysis peakAnalysis,
            @Schema(description = "イベント影響") List<EventImpact> eventImpacts,
            @Schema(description = "メタデータ") ForecastMetadata metadata
    ) {
        /**
         * 月別予測
         */
        public record MonthlyForecast(
                @Schema(description = "月") int month,
                @Schema(description = "予測需要指数") double demandIndex,
                @Schema(description = "前年同月比") double yearOverYearChange,
                @Schema(description = "信頼度") double confidence,
                @Schema(description = "推奨在庫レベル") String recommendedStockLevel
        ) {}
        
        /**
         * 季節パターン
         */
        public record SeasonalPattern(
                @Schema(description = "パターンタイプ") String patternType,
                @Schema(description = "季節性強度") double seasonalityStrength,
                @Schema(description = "ピーク月") List<Integer> peakMonths,
                @Schema(description = "オフシーズン月") List<Integer> offSeasonMonths,
                @Schema(description = "変動係数") double coefficientOfVariation
        ) {}
        
        /**
         * ピーク分析
         */
        public record PeakAnalysis(
                @Schema(description = "主要ピーク") Peak primaryPeak,
                @Schema(description = "副次ピーク") List<Peak> secondaryPeaks,
                @Schema(description = "ピーク準備期間") int peakPreparationDays,
                @Schema(description = "在庫積み増し推奨") List<StockBuildupRecommendation> stockBuildupRecommendations
        ) {}
        
        /**
         * ピーク
         */
        public record Peak(
                @Schema(description = "開始月") int startMonth,
                @Schema(description = "終了月") int endMonth,
                @Schema(description = "強度") double intensity,
                @Schema(description = "予測需要増加率") double demandIncrease
        ) {}
        
        /**
         * 在庫積み増し推奨
         */
        public record StockBuildupRecommendation(
                @Schema(description = "商品グループ") String productGroup,
                @Schema(description = "積み増し開始日") @JsonFormat(pattern = "yyyy-MM-dd") LocalDate buildupStartDate,
                @Schema(description = "目標在庫レベル") double targetStockLevel,
                @Schema(description = "推奨注文量") int recommendedOrderQuantity
        ) {}
        
        /**
         * イベント影響
         */
        public record EventImpact(
                @Schema(description = "イベント名") String eventName,
                @Schema(description = "影響期間") DateRange impactPeriod,
                @Schema(description = "需要変化率") double demandChangeRate,
                @Schema(description = "影響度") String impactLevel
        ) {}
    }
    
    /**
     * 予測ダッシュボードレスポンス
     */
    @Schema(description = "予測ダッシュボード")
    public record ForecastDashboardResponse(
            @Schema(description = "ダッシュボードタイプ") String dashboardType,
            @Schema(description = "生成時刻") @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime generatedAt,
            @Schema(description = "サマリー") ForecastSummary summary,
            @Schema(description = "アラート") List<ForecastAlert> alerts,
            @Schema(description = "主要指標") List<KeyMetric> keyMetrics,
            @Schema(description = "予測精度") ForecastAccuracy accuracy,
            @Schema(description = "推奨アクション") List<RecommendedAction> recommendedActions
    ) {
        /**
         * 予測サマリー
         */
        public record ForecastSummary(
                @Schema(description = "監視商品数") int monitoredProducts,
                @Schema(description = "高精度予測率") double highAccuracyRate,
                @Schema(description = "平均予測誤差") double avgForecastError,
                @Schema(description = "在庫最適化商品数") int optimizedProducts
        ) {}
        
        /**
         * 主要指標
         */
        public record KeyMetric(
                @Schema(description = "指標名") String metricName,
                @Schema(description = "現在値") double currentValue,
                @Schema(description = "予測値") double forecastValue,
                @Schema(description = "変化率") double changeRate,
                @Schema(description = "トレンド") String trend
        ) {}
        
        /**
         * 推奨アクション
         */
        public record RecommendedAction(
                @Schema(description = "アクションタイプ") String actionType,
                @Schema(description = "対象商品") String targetProduct,
                @Schema(description = "説明") String description,
                @Schema(description = "優先度") String priority,
                @Schema(description = "期限") @JsonFormat(pattern = "yyyy-MM-dd") LocalDate deadline
        ) {}
    }
    
    /**
     * 精度評価レスポンス
     */
    @Schema(description = "予測精度評価結果")
    public record AccuracyEvaluationResponse(
            @Schema(description = "モデルID") String modelId,
            @Schema(description = "評価期間") DateRange evaluationPeriod,
            @Schema(description = "精度指標") AccuracyMetrics accuracyMetrics,
            @Schema(description = "詳細評価") List<DetailedEvaluation> detailedEvaluations,
            @Schema(description = "改善提案") List<ImprovementSuggestion> improvementSuggestions,
            @Schema(description = "メタデータ") ForecastMetadata metadata
    ) {
        /**
         * 精度指標
         */
        public record AccuracyMetrics(
                @Schema(description = "MAPE(平均絶対パーセント誤差)") double mape,
                @Schema(description = "RMSE(二乗平均平方根誤差)") double rmse,
                @Schema(description = "MAE(平均絶対誤差)") double mae,
                @Schema(description = "決定係数") double rSquared,
                @Schema(description = "全体評価") String overallRating
        ) {}
        
        /**
         * 詳細評価
         */
        public record DetailedEvaluation(
                @Schema(description = "商品ID") String productId,
                @Schema(description = "予測精度") double accuracy,
                @Schema(description = "誤差分析") ErrorAnalysis errorAnalysis,
                @Schema(description = "改善ポイント") List<String> improvementPoints
        ) {}
        
        /**
         * 誤差分析
         */
        public record ErrorAnalysis(
                @Schema(description = "平均誤差") double meanError,
                @Schema(description = "標準偏差") double standardDeviation,
                @Schema(description = "最大誤差") double maxError,
                @Schema(description = "誤差分布") Map<String, Integer> errorDistribution
        ) {}
        
        /**
         * 改善提案
         */
        public record ImprovementSuggestion(
                @Schema(description = "改善項目") String improvementArea,
                @Schema(description = "提案内容") String suggestion,
                @Schema(description = "期待効果") String expectedBenefit,
                @Schema(description = "実装難易度") String implementationDifficulty
        ) {}
    }
    
    /**
     * バッチ予測リクエスト
     */
    @Schema(description = "バッチ予測リクエスト")
    public record BatchForecastRequest(
            @Schema(description = "商品IDリスト") List<String> productIds,
            @Schema(description = "予測タイプ") String forecastType,
            @Schema(description = "予測期間(日数)") int forecastDays,
            @Schema(description = "予測モデル") String model,
            @Schema(description = "コールバックURL") String callbackUrl,
            @Schema(description = "追加パラメータ") Map<String, Object> additionalParams
    ) {}
    
    /**
     * バッチ予測レスポンス
     */
    @Schema(description = "バッチ予測レスポンス")
    public record BatchForecastResponse(
            @Schema(description = "バッチID") String batchId,
            @Schema(description = "ステータス") String status,
            @Schema(description = "開始時刻") @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @Schema(description = "予測完了時刻") @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime estimatedCompletionTime,
            @Schema(description = "処理対象数") int totalProducts,
            @Schema(description = "進捗URL") String progressUrl
    ) {}
    
    /**
     * 共通レコード定義
     */
    
    /**
     * 予測期間
     */
    public record ForecastPeriod(
            @Schema(description = "開始日") @JsonFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Schema(description = "終了日") @JsonFormat(pattern = "yyyy-MM-dd") LocalDate endDate,
            @Schema(description = "期間(日数)") int days
    ) {}
    
    /**
     * 日付範囲
     */
    public record DateRange(
            @Schema(description = "開始日") @JsonFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @Schema(description = "終了日") @JsonFormat(pattern = "yyyy-MM-dd") LocalDate endDate
    ) {}
    
    /**
     * 予測統計
     */
    public record ForecastStatistics(
            @Schema(description = "平均値") double mean,
            @Schema(description = "中央値") double median,
            @Schema(description = "標準偏差") double standardDeviation,
            @Schema(description = "最小値") double min,
            @Schema(description = "最大値") double max,
            @Schema(description = "合計") double total
    ) {}
    
    /**
     * 信頼区間
     */
    public record ConfidenceInterval(
            @Schema(description = "下限") List<Double> lowerBound,
            @Schema(description = "上限") List<Double> upperBound,
            @Schema(description = "信頼水準") double confidenceLevel
    ) {}
    
    /**
     * 予測アラート
     */
    public record ForecastAlert(
            @Schema(description = "アラートタイプ") String alertType,
            @Schema(description = "重要度") String severity,
            @Schema(description = "メッセージ") String message,
            @Schema(description = "推奨アクション") String recommendedAction,
            @Schema(description = "期限") @JsonFormat(pattern = "yyyy-MM-dd") LocalDate deadline
    ) {}
    
    /**
     * 予測精度
     */
    public record ForecastAccuracy(
            @Schema(description = "短期精度(7日)") double shortTermAccuracy,
            @Schema(description = "中期精度(30日)") double mediumTermAccuracy,
            @Schema(description = "長期精度(90日)") double longTermAccuracy,
            @Schema(description = "全体精度") double overallAccuracy
    ) {}
    
    /**
     * 予測メタデータ
     */
    public record ForecastMetadata(
            @Schema(description = "モデルバージョン") String modelVersion,
            @Schema(description = "アルゴリズム") String algorithm,
            @Schema(description = "処理時間(ミリ秒)") long processingTimeMs,
            @Schema(description = "データ品質スコア") double dataQualityScore,
            @Schema(description = "最終更新時刻") @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime lastUpdated
    ) {}
}
