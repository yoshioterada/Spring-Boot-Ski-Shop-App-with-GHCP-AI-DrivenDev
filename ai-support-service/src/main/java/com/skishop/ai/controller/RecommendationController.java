package com.skishop.ai.controller;

import com.skishop.ai.service.ProductRecommendationAssistant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 商品推奨API コントローラー
 * 
 * <p>LangChain4j 1.1.0 + Azure OpenAI を使用した商品推奨機能</p>
 * <p>Java 21の最新機能を活用</p>
 * 
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/recommendations")
@Tag(name = "Recommendation API", description = "AI商品推奨関連API")
public class RecommendationController {
    
    private static final Logger logger = LoggerFactory.getLogger(RecommendationController.class);
    
    private final ProductRecommendationAssistant recommendationAssistant;
    
    /**
     * コンストラクタ
     * 
     * @param recommendationAssistant 商品推奨アシスタント
     */
    public RecommendationController(ProductRecommendationAssistant recommendationAssistant) {
        this.recommendationAssistant = recommendationAssistant;
    }
    
    /**
     * Java 21のRecordを使用したリクエストパラメータ
     */
    public record RecommendationParams(
        String userId,
        String productId,
        String category,
        int limit,
        String userContext
    ) {
        // コンパクトコンストラクタでバリデーション
        public RecommendationParams {
            if (limit <= 0) limit = 10;
            if (limit > 50) limit = 50;
        }
        
        public static RecommendationParams forPersonalized(String userId, int limit, String category) {
            return new RecommendationParams(userId, null, category, limit, null);
        }
        
        public static RecommendationParams forSimilar(String productId, int limit) {
            return new RecommendationParams(null, productId, null, limit, null);
        }
        
        public static RecommendationParams forTrending(int limit, String category) {
            return new RecommendationParams(null, null, category, limit, null);
        }
    }
    
    /**
     * ユーザー向け商品推奨
     */
    @GetMapping("/{userId}")
    @Operation(summary = "ユーザー向け商品推奨", description = "指定されたユーザーに対する商品推奨")
    public ResponseEntity<String> getPersonalizedRecommendations(
            @PathVariable String userId,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String category) {
        
        var params = RecommendationParams.forPersonalized(userId, limit, category);
        logger.info("Getting personalized recommendations for user: {} (limit: {}, category: {})", 
                 userId, params.limit(), params.category());
        
        try {
            // Java 21のvar型推論とText Blocksを活用
            var userProfile = buildUserProfile(userId);
            var productCatalog = getMockProductCatalog();
            var userQuery = buildPersonalizedQuery(params.category(), params.limit());
            
            var recommendations = recommendationAssistant.generateRecommendations(
                userQuery, userProfile, productCatalog
            );
            
            return ResponseEntity.ok(recommendations);
            
        } catch (Exception e) {
            logger.error("Error generating personalized recommendations: ", e);
            return createErrorResponse("推奨生成中にエラーが発生しました");
        }
    }
    
    /**
     * 類似商品推奨
     */
    @GetMapping("/similar/{productId}")
    @Operation(summary = "類似商品推奨", description = "指定された商品に類似する商品の推奨")
    public ResponseEntity<String> getSimilarProducts(
            @PathVariable String productId,
            @RequestParam(defaultValue = "5") int limit) {
        
        var params = RecommendationParams.forSimilar(productId, limit);
        logger.info("Getting similar products for product: {} (limit: {})", productId, params.limit());
        
        try {
            var targetProduct = getProductInfo(productId);
            var productCatalog = getMockProductCatalog();
            
            var similarProducts = recommendationAssistant.findSimilarProducts(
                targetProduct, productCatalog
            );
            
            return ResponseEntity.ok(similarProducts);
            
        } catch (Exception e) {
            logger.error("Error finding similar products: ", e);
            return createErrorResponse("類似商品検索中にエラーが発生しました");
        }
    }
    
    /**
     * トレンド商品推奨
     */
    @GetMapping("/trending")
    @Operation(summary = "トレンド商品推奨", description = "現在のトレンド商品の推奨")
    public ResponseEntity<String> getTrendingProducts(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String category) {
        
        var params = RecommendationParams.forTrending(limit, category);
        logger.info("Getting trending products (limit: {}, category: {})", params.limit(), params.category());
        
        // Java 21のText Blocksを活用したモックデータ
        var trendingProducts = """
            {
              "recommendations": [
                {
                  "productId": "ski-001",
                  "productName": "Rossignol Experience 88 Ti",
                  "score": 0.95,
                  "reasons": ["今季人気", "高評価", "注目のモデル"],
                  "category": "オールマウンテンスキー",
                  "trendScore": 0.9
                },
                {
                  "productId": "boot-001", 
                  "productName": "Lange RX 130",
                  "score": 0.88,
                  "reasons": ["プロ使用モデル", "高性能", "人気急上昇"],
                  "category": "スキーブーツ",
                  "trendScore": 0.85
                }
              ],
              "explanation": "現在の季節とユーザーの関心に基づいたトレンド商品です"
            }
            """;
        
        return ResponseEntity.ok(trendingProducts);
    }
    
    /**
     * カテゴリベース商品推奨
     */
    @GetMapping("/category/{category}")
    @Operation(summary = "カテゴリベース商品推奨", description = "指定されたカテゴリからの商品推奨")
    public ResponseEntity<String> getCategoryRecommendations(
            @PathVariable String category,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String userContext) {
        
        logger.info("Getting category-based recommendations for {} (limit: {}, context: {})", 
                 category, limit, userContext);
        
        try {
            var productCatalog = getMockProductCatalog();
            var query = String.format("%sカテゴリから%sに適した商品を%d件推奨してください", 
                                     category, 
                                     userContext != null ? userContext : "一般的な用途", 
                                     limit);
            
            var recommendations = recommendationAssistant.generateRecommendations(
                query, null, productCatalog
            );
            
            return ResponseEntity.ok(recommendations);
            
        } catch (Exception e) {
            logger.error("Error generating category recommendations: ", e);
            return createErrorResponse("カテゴリ推奨生成中にエラーが発生しました");
        }
    }
    
    /**
     * 推奨フィードバック記録
     */
    @PostMapping("/feedback")
    @Operation(summary = "推奨フィードバック記録", description = "推奨結果に対するユーザーフィードバックを記録")
    public ResponseEntity<Void> submitRecommendationFeedback(
            @RequestParam String userId,
            @RequestParam String productId,
            @RequestParam String action,
            @RequestParam(required = false) Double rating) {
        
        logger.info("Received recommendation feedback - User: {}, Product: {}, Action: {}", 
                 userId, productId, action);
        
        // TODO: 実際の実装ではフィードバックをDBに保存し、推奨アルゴリズムの改善に使用
        
        return ResponseEntity.ok().build();
    }
    
    /**
     * 推奨理由説明
     */
    @GetMapping("/explain/{userId}/{productId}")
    @Operation(summary = "推奨理由説明", description = "なぜその商品が推奨されたかの説明")
    public ResponseEntity<String> explainRecommendation(
            @PathVariable String userId,
            @PathVariable String productId) {
        
        logger.info("Explaining recommendation for user: {} and product: {}", userId, productId);
        
        // Java 21のText Blocksを活用
        var explanation = """
            {
              "explanation": "この商品があなたに推奨された理由",
              "factors": [
                {
                  "factor": "過去の購入履歴",
                  "description": "あなたは以前に類似のカテゴリの商品を購入されています",
                  "weight": 0.3
                },
                {
                  "factor": "スキルレベルマッチ",
                  "description": "あなたのスキルレベルに適した商品です",
                  "weight": 0.4
                },
                {
                  "factor": "価格帯の適合",
                  "description": "あなたの予算範囲内の商品です",
                  "weight": 0.3
                }
              ],
              "confidence": 0.85
            }
            """;
        
        return ResponseEntity.ok(explanation);
    }
    
    /**
     * ユーザープロファイルの構築（Java 21のText Blocks with formatted）
     */
    private String buildUserProfile(String userId) {
        // 実際の実装ではユーザー管理サービスから取得
        return """
            {
              "userId": "%s",
              "skillLevel": "中級者",
              "preferences": {
                "budget": {"min": 50000, "max": 150000},
                "brands": ["Rossignol", "Salomon", "Atomic"],
                "usage": "レジャー",
                "categories": ["スキー板", "ブーツ"]
              },
              "physicalAttributes": {
                "height": "170cm",
                "weight": "65kg",
                "footSize": "26.5cm"
              },
              "purchaseHistory": [
                {"productId": "ski-old-001", "category": "スキー板", "purchaseDate": "2023-01-15"},
                {"productId": "boot-old-001", "category": "ブーツ", "purchaseDate": "2023-01-15"}
              ],
              "searchHistory": ["スキー板 中級者", "カービングスキー", "スキーブーツ"],
              "seasonalPreferences": {
                "season": "winter",
                "preferredSlopes": ["ゲレンデ", "オンピステ"]
              }
            }
            """.formatted(userId);
    }
    
    /**
     * パーソナライズドクエリの構築
     */
    private String buildPersonalizedQuery(String category, int limit) {
        var baseQuery = new StringBuilder("私に最適な商品を推奨してください");
        if (category != null) {
            baseQuery.append("。カテゴリ: ").append(category);
        }
        baseQuery.append("。最大").append(limit).append("件まで。");
        return baseQuery.toString();
    }
    
    /**
     * 商品情報の取得
     */
    private String getProductInfo(String productId) {
        // 実際の実装では商品管理サービスから取得
        return """
            {
              "productId": "%s",
              "name": "Sample Product",
              "category": "スキー板",
              "features": ["高性能", "軽量", "耐久性"],
              "price": 89000,
              "specifications": {
                "length": "170cm",
                "width": "88mm",
                "technology": ["チタン強化", "ウッドコア"]
              }
            }
            """.formatted(productId);
    }
    
    /**
     * モック商品カタログの取得（Java 21のText Blocksを活用）
     */
    private String getMockProductCatalog() {
        return """
            [
              {
                "productId": "ski-001",
                "name": "Rossignol Experience 88 Ti",
                "category": "オールマウンテンスキー",
                "price": 89000,
                "skillLevel": "中級者-上級者",
                "features": ["チタン強化", "安定性", "カービング性能"],
                "specifications": {
                  "length": ["160cm", "170cm", "180cm"],
                  "width": "88mm",
                  "technology": ["チタン強化", "ウッドコア", "オートターン"]
                }
              },
              {
                "productId": "ski-002", 
                "name": "Salomon QST 92",
                "category": "フリーライドスキー",
                "price": 76000,
                "skillLevel": "中級者",
                "features": ["軽量", "パウダー対応", "オールラウンド"],
                "specifications": {
                  "length": ["165cm", "175cm", "185cm"],
                  "width": "92mm",
                  "technology": ["カーボン", "軽量コア"]
                }
              },
              {
                "productId": "boot-001",
                "name": "Lange RX 130",
                "category": "スキーブーツ",
                "price": 98000,
                "skillLevel": "上級者",
                "features": ["高剛性", "精密フィット", "競技対応"],
                "specifications": {
                  "flex": "130",
                  "lastWidth": "98mm",
                  "features": ["ヒートモルディング", "レーシング仕様"]
                }
              }
            ]
            """;
    }
    
    /**
     * エラーレスポンスの作成（Java 21のText Blocksを活用）
     */
    private ResponseEntity<String> createErrorResponse(String message) {
        var errorResponse = """
            {
              "error": "%s",
              "timestamp": "%s",
              "status": 500
            }
            """.formatted(message, java.time.LocalDateTime.now());
        
        return ResponseEntity.internalServerError().body(errorResponse);
    }
}
