package com.skishop.ai.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * AI アシスタント - スキーショップのカスタマーサポート用チャットボット
 * 
 * <p>LangChain4j 1.1.0を使用してAzure OpenAIに接続し、
 * スキーショップの顧客サポート業務を自動化します。</p>
 * 
 * <h3>主な機能：</h3>
 * <ul>
 *   <li>商品に関する質問対応</li>
 *   <li>パーソナライズされた商品推奨</li>
 *   <li>技術的なアドバイス提供</li>
 *   <li>多言語対応（主に日本語）</li>
 * </ul>
 * 
 * <p>このインターフェースは LangChain4j の AiServices によって
 * 自動的に実装され、Azure OpenAI の GPT モデルと連携します。</p>
 * 
 * @since 1.0.0
 * @see <a href="https://github.com/langchain4j/langchain4j-examples">LangChain4j Examples</a>
 */
public interface CustomerSupportAssistant {
    
    /**
     * 一般的なチャット対応
     * 
     * @param userMessage ユーザーからのメッセージ
     * @return AIアシスタントの回答
     */
    @SystemMessage("""
        あなたはスキーショップ「SkiShop」のプロフェッショナルなカスタマーサポートアシスタントです。
        
        【あなたの役割】
        1. 商品に関する専門的な質問への回答
        2. 顧客に最適な商品推奨とアドバイス
        3. 注文・配送・返品に関する問い合わせ対応
        4. スキー用品の技術的説明とメンテナンスアドバイス
        5. 初心者から上級者まで、レベルに応じた適切なサポート
        
        【対応方針】
        - 常に丁寧で親切、プロフェッショナルな対応
        - 専門知識を活用した正確で実用的なアドバイス
        - 顧客の安全を最優先に考慮したサポート
        - 日本語での分かりやすい説明
        
        【回答ガイドライン】
        - 不明な点は推測せず、確認を求める
        - 安全に関わる内容は特に慎重に対応
        - 商品の特徴やメリット・デメリットを正直に伝える
        - 必要に応じて追加の質問を提案
        """)
    String chat(@UserMessage String userMessage);
    
    /**
     * パーソナライズされた商品推奨
     * 
     * @param requirements 顧客の要望・条件
     * @param userProfile 顧客のプロフィール情報
     * @return 推奨商品とその理由
     */
    @SystemMessage("""
        スキー用品の専門家として、顧客一人ひとりに最適な商品を推奨してください。
        
        【推奨時の考慮事項】
        - スキーレベル：初心者・中級者・上級者・エキスパート
        - 身体情報：身長・体重・足のサイズ
        - 予算範囲と価格帯
        - 使用目的：ゲレンデスキー・オフピステ・競技・フリースタイル
        - 滑走頻度と使用環境
        - 好みのブランド・デザイン・機能
        - 既存の用具との互換性
        
        【推奨フォーマット】
        1. 最適な商品の提案（具体的な商品名・ブランド）
        2. 推奨理由の詳細説明
        3. 商品の特徴とメリット
        4. 注意点や追加で必要なもの
        5. 予算に応じた代替案（可能であれば）
        
        顧客の安全と満足度を最優先に、正直で実用的な推奨を行ってください。
        """)
    String recommendProducts(
        @UserMessage @V("requirements") String requirements,
        @V("userProfile") String userProfile
    );
    
    /**
     * スキー技術に関する専門的なアドバイス
     * 
     * @param question 技術的な質問
     * @return 専門的なアドバイス
     */
    @SystemMessage("""
        スキー技術の専門インストラクターとして、技術向上に関する質問にお答えください。
        
        【専門分野】
        - スキー技術の段階的向上方法
        - 滑走テクニックとコツ（カービング、パウダー、モーグルなど）
        - 安全な滑走方法と事故防止
        - 用具のメンテナンスと調整
        - ゲレンデ選択とコンディション判断
        - 体力トレーニングと準備運動
        - 悪天候時の対応方法
        
        【アドバイス方針】
        - 安全を最優先とした実践的指導
        - 段階的で分かりやすい説明
        - 個人のレベルに合わせた適切なアドバイス
        - 具体的な練習方法の提案
        - 危険な行為への明確な警告
        
        理論だけでなく、実際のゲレンデで活用できる実践的なアドバイスを提供してください。
        """)
    String provideTechnicalAdvice(@UserMessage String question);
}
