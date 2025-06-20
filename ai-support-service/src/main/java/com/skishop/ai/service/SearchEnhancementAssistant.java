package com.skishop.ai.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 検索拡張AI - セマンティック検索とオートコンプリート
 * 
 * <p>LangChain4j 1.1.0 + Azure OpenAI を使用して、
 * 高度な検索機能を提供します。</p>
 * 
 * <h3>主な機能：</h3>
 * <ul>
 *   <li>セマンティック検索 - 意味的に関連する商品を検索</li>
 *   <li>検索クエリの拡張と改善</li>
 *   <li>検索結果のランキング最適化</li>
 *   <li>検索意図の理解と分析</li>
 *   <li>オートコンプリート機能</li>
 * </ul>
 * 
 * @since 1.0.0
 * @see CustomerSupportAssistant
 * @see ProductRecommendationAssistant
 */
public interface SearchEnhancementAssistant {
    
    /**
     * セマンティック検索を実行
     * 
     * @param query 検索クエリ
     * @param productCatalog 商品カタログ情報
     * @param userContext ユーザーコンテキスト
     * @return JSON形式の検索結果
     */
    @SystemMessage("""
        あなたは高度な検索システムです。
        ユーザーの検索クエリを分析し、最適な検索結果を提供してください。
        
        機能：
        1. セマンティック検索 - 意味的に関連する商品を検索
        2. 検索クエリの拡張と改善
        3. 検索結果のランキング最適化
        4. 検索意図の理解と分析
        
        検索結果はJSON形式で以下の構造で返してください：
        {
          "results": [
            {
              "productId": "商品ID",
              "title": "商品名",
              "relevanceScore": 0.95,
              "matchType": "semantic|exact|partial",
              "highlights": ["検索にマッチした部分"]
            }
          ],
          "suggestedQueries": ["関連検索クエリ"],
          "filters": {"category": "カテゴリ", "priceRange": {"min": 0, "max": 100000}}
        }
        """)
    String performSemanticSearch(
        @UserMessage @V("query") String query,
        @V("productCatalog") String productCatalog,
        @V("userContext") String userContext
    );
    
    /**
     * オートコンプリート候補を生成
     * 
     * @param partialQuery 部分的な検索クエリ
     * @param popularQueries 人気検索クエリ
     * @return JSON形式の補完候補
     */
    @SystemMessage("""
        検索のオートコンプリート機能を提供してください。
        ユーザーの入力途中のクエリに対して、適切な補完候補を生成してください。
        
        補完の基準：
        - 人気の検索クエリ
        - 季節性
        - ユーザーの履歴
        - 商品カテゴリ
        
        JSON形式で以下の構造で返してください：
        {
          "suggestions": [
            {
              "completion": "補完後のクエリ",
              "type": "product|category|brand",
              "popularity": 0.8
            }
          ]
        }
        """)
    String generateAutocompleteSuggestions(
        @UserMessage @V("partialQuery") String partialQuery,
        @V("popularQueries") String popularQueries
    );
}
