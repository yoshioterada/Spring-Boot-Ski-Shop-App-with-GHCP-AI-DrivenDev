package com.skishop.ai.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * 商品推奨AI - 高度なセマンティック検索と推奨エンジン
 * 
 * <p>LangChain4j 1.1.0 + Azure OpenAIを使用して、
 * パーソナライズされた商品推奨を提供します。</p>
 * 
 * <h3>主な機能：</h3>
 * <ul>
 *   <li>ユーザープロファイルベースの推奨</li>
 *   <li>類似商品の検索と推奨</li>
 *   <li>季節・シーン別推奨</li>
 *   <li>予算最適化推奨</li>
 * </ul>
 * 
 * <p>このインターフェースは機械学習とセマンティック分析を活用して、
 * 個々のユーザーに最適化された商品推奨を提供します。</p>
 * 
 * @since 1.0.0
 * @see <a href="https://github.com/langchain4j/langchain4j-examples">LangChain4j Examples</a>
 */
public interface ProductRecommendationAssistant {
    
    /**
     * パーソナライズされた商品推奨
     * 
     * @param userQuery ユーザーのクエリ・要望
     * @param userProfile ユーザープロファイル
     * @param productCatalog 商品カタログ情報
     * @return JSON形式の推奨結果
     */
    @SystemMessage("""
        あなたは最先端のスキー用品推奨エンジンです。
        機械学習とセマンティック分析を活用して、ユーザーに最適な商品を推奨してください。
        
        【分析対象データ】
        - ユーザープロファイル：スキーレベル、経験年数、好み
        - 身体特性：身長、体重、足のサイズ、体型
        - 予算・価格感度：予算範囲、価格重視度
        - 使用条件：頻度、シーン、地域、季節
        - 嗜好情報：ブランド好み、デザイン傾向
        - 行動履歴：過去の購入、検索、閲覧パターン
        
        【推奨アルゴリズム】
        1. コンテンツベースフィルタリング
        2. 協調フィルタリング
        3. ハイブリッド推奨
        4. シーズナル調整
        5. 在庫・価格最適化
        
        【出力形式】
        以下のJSON構造で回答してください：
        ```json
        {
          "recommendations": [
            {
              "productId": "商品ID",
              "productName": "商品名",
              "brand": "ブランド名",
              "score": 0.95,
              "confidenceLevel": "high|medium|low",
              "reasons": [
                "ユーザーのスキーレベルに最適",
                "予算範囲内で最高品質",
                "好みのブランドの最新モデル"
              ],
              "category": "カテゴリ",
              "price": "価格",
              "suitabilityScore": {
                "level": 0.9,
                "bodyType": 0.8,
                "purpose": 0.95,
                "budget": 0.85
              }
            }
          ],
          "totalRecommendations": 5,
          "searchStrategy": "使用した推奨戦略",
          "explanation": "推奨全体の説明と根拠"
        }
        ```
        
        【重要事項】
        - 安全性を最優先に考慮
        - 正確な商品情報のみ使用
        - ユーザーの予算を尊重
        - 過度な推奨は避ける
        """)
    String generateRecommendations(
        @UserMessage @V("userQuery") String userQuery,
        @V("userProfile") String userProfile,
        @V("productCatalog") String productCatalog
    );
    
    /**
     * 類似商品検索と推奨
     * 
     * @param targetProduct 基準となる商品
     * @param productCatalog 商品カタログ
     * @return JSON形式の類似商品リスト
     */
    @SystemMessage("""
        指定された商品の特徴を詳細に分析し、類似性の高い商品を発見・推奨してください。
        
        【類似性判定基準】
        - 商品カテゴリ（スキー板、ブーツ、ウェアなど）
        - 価格帯・グレードレベル
        - ブランドポジショニング
        - 技術仕様・機能特性
        - 対象スキーレベル・技術水準
        - 使用目的・シーン適性
        - デザイン・スタイル傾向
        - 素材・製造技術
        
        【分析手法】
        1. 特徴ベクトル比較
        2. セマンティック類似性分析
        3. ユーザーレビュー・評価パターン分析
        4. 購買行動パターン分析
        5. 専門家評価との相関分析
        
        【出力形式】
        ```json
        {
          "targetProduct": {
            "id": "基準商品ID",
            "name": "基準商品名",
            "analysis": "商品特徴分析"
          },
          "similarProducts": [
            {
              "productId": "類似商品ID",
              "productName": "類似商品名",
              "similarityScore": 0.88,
              "similarityReasons": [
                "同じカテゴリ・価格帯",
                "類似の技術仕様",
                "同等の対象レベル"
              ],
              "differences": [
                "ブランドが異なる",
                "カラーバリエーション"
              ],
              "recommendationReason": "なぜ代替品として適しているか"
            }
          ],
          "totalSimilarProducts": 8,
          "analysisMethod": "使用した分析手法"
        }
        ```
        
        正確性と実用性を重視し、実際の購買決定に役立つ情報を提供してください。
        """)
    String findSimilarProducts(
        @UserMessage @V("targetProduct") String targetProduct,
        @V("productCatalog") String productCatalog
    );
    
    /**
     * 季節・シーン別商品推奨
     * 
     * @param season 季節情報
     * @param scene 使用シーン
     * @param userProfile ユーザープロファイル
     * @return 季節・シーン最適化推奨
     */
    @SystemMessage("""
        季節とシーンに特化した商品推奨を行ってください。
        
        【季節考慮要素】
        - 早シーズン・ハイシーズン・春シーズン
        - 雪質・気候条件
        - ゲレンデ状況
        - 気温・天候パターン
        
        【シーン考慮要素】
        - ゲレンデスキー・バックカントリー・競技
        - 日帰り・宿泊・長期滞在
        - 家族・友人・単独
        - 初回・リピート・特別なイベント
        
        季節とシーンに最適化された実用的な推奨を提供してください。
        """)
    String recommendBySeasonAndScene(
        @UserMessage @V("season") String season,
        @V("scene") String scene,
        @V("userProfile") String userProfile
    );
}
