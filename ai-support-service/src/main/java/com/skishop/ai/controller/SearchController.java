package com.skishop.ai.controller;

import com.skishop.ai.service.SearchEnhancementAssistant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 検索拡張API コントローラー
 * 
 * <p>LangChain4j 1.1.0 + Azure OpenAI を使用したセマンティック検索機能</p>
 * 
 * <h3>提供機能：</h3>
 * <ul>
 *   <li>セマンティック検索 - 意味的類似性による検索</li>
 *   <li>オートコンプリート - 検索候補の自動補完</li>
 *   <li>検索拡張 - クエリの自動改善</li>
 * </ul>
 * 
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/search")
@Tag(name = "Search API", description = "AI検索拡張関連API")
public class SearchController {
    
    private static final Logger logger = LoggerFactory.getLogger(SearchController.class);
    
    private final SearchEnhancementAssistant searchAssistant;
    
    /**
     * コンストラクタ
     * 
     * @param searchAssistant 検索拡張アシスタント
     */
    public SearchController(SearchEnhancementAssistant searchAssistant) {
        this.searchAssistant = searchAssistant;
    }
    
    // Java 21 Text Blocks for mock data
    private static final String MOCK_PRODUCT_CATALOG = """
            [
              {
                "productId": "ski-001",
                "name": "Rossignol Experience 88 Ti",
                "category": "オールマウンテンスキー",
                "brand": "Rossignol",
                "price": 89000,
                "description": "中級者から上級者向けの万能スキー"
              },
              {
                "productId": "ski-002", 
                "name": "Salomon XDR 88 Ti",
                "category": "オールマウンテンスキー",
                "brand": "Salomon",
                "price": 95000,
                "description": "優れたエッジグリップと安定性"
              },
              {
                "productId": "boots-001",
                "name": "Lange RX 120",
                "category": "スキーブーツ",
                "brand": "Lange",
                "price": 78000,
                "description": "プレシジョンフィットの競技用ブーツ"
              }
            ]
            """;
    
    private static final String MOCK_POPULAR_QUERIES = """
            [
              {"query": "スキー板 初心者", "popularity": 0.9},
              {"query": "スキーブーツ", "popularity": 0.8},
              {"query": "スキーウェア", "popularity": 0.7},
              {"query": "ゴーグル", "popularity": 0.6},
              {"query": "スキー手袋", "popularity": 0.5}
            ]
            """;
    
    /**
     * セマンティック検索
     */
    @PostMapping("/semantic")
    @Operation(summary = "セマンティック検索", description = "AIを使用した意味的検索")
    public ResponseEntity<String> performSemanticSearch(
            @RequestParam String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String userId) {
        
        logger.info("Performing semantic search for query: {}", query);
        
        try {
            // ユーザーコンテキストの構築
            var userContext = buildUserContext(userId, category);
            
            // AI検索の実行
            var searchResults = searchAssistant.performSemanticSearch(
                query, MOCK_PRODUCT_CATALOG, userContext
            );
            
            return ResponseEntity.ok(searchResults);
            
        } catch (Exception e) {
            logger.error("Error performing semantic search: ", e);
            return ResponseEntity.internalServerError()
                    .body("{\"error\": \"検索中にエラーが発生しました\"}");
        }
    }
    
    /**
     * オートコンプリート
     */
    @GetMapping("/autocomplete")
    @Operation(summary = "検索オートコンプリート", description = "検索クエリの自動補完")
    public ResponseEntity<String> getAutocompleteSuggestions(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int limit) {
        
        logger.info("Getting autocomplete suggestions for: {}", q);
        
        try {
            // オートコンプリート生成
            var suggestions = searchAssistant.generateAutocompleteSuggestions(
                q, MOCK_POPULAR_QUERIES
            );
            
            return ResponseEntity.ok(suggestions);
            
        } catch (Exception e) {
            logger.error("Error generating autocomplete suggestions: ", e);
            return ResponseEntity.internalServerError()
                    .body("{\"suggestions\": []}");
        }
    }
    
    /**
     * 検索サジェスト
     */
    @GetMapping("/suggest")
    @Operation(summary = "検索サジェスト", description = "検索候補の提案")
    public ResponseEntity<String> getSearchSuggestions(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "5") int limit) {
        
        logger.info("Getting search suggestions for category: {}", category);
        
        // カテゴリ別の人気検索クエリを返す
        String suggestions = """
            {
              "suggestions": [
                "初心者向け スキー板",
                "カービングスキー おすすめ",
                "スキーブーツ サイズ",
                "スキーウェア 防水",
                "スキーゴーグル 曇り止め"
              ]
            }
            """;
        
        return ResponseEntity.ok(suggestions);
    }
    
    /**
     * 画像検索（将来実装）
     */
    @PostMapping("/visual")
    @Operation(summary = "画像検索", description = "画像を使用した商品検索")
    public ResponseEntity<String> performVisualSearch() {
        
        logger.info("Visual search requested");
        
        return ResponseEntity.ok("{\"message\": \"画像検索機能は今後実装予定です\"}");
    }
    
    /**
     * ユーザーコンテキストの構築
     * Java 21のString formatted() メソッドを使用
     */
    private String buildUserContext(String userId, String category) {
        return """
            {
              "userId": "%s",
              "preferredCategory": "%s",
              "searchHistory": ["スキー板", "ブーツ", "ウェア"],
              "skillLevel": "中級者",
              "budget": {"min": 30000, "max": 100000}
            }
            """.formatted(
                userId != null ? userId : "anonymous",
                category != null ? category : "all"
            );
    }
}
