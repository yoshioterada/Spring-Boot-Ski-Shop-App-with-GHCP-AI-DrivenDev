package com.skishop.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * ビジネス設定プロパティ
 * 
 * <p>Java 21のrecord機能を使用した設定クラス</p>
 * <p>application.ymlの business.* プロパティをバインド</p>
 * 
 * @since 1.0.0
 */
@Component
@ConfigurationProperties(prefix = "business")
public record BusinessConfigProperties(
    RecommendationConfig recommendation,
    ChatbotConfig chatbot,
    SearchConfig search
) {
    
    /**
     * デフォルト値を持つ設定を作成
     */
    public BusinessConfigProperties() {
        this(
            new RecommendationConfig(10, 3600, List.of("collaborative-filtering", "content-based", "hybrid")),
            new ChatbotConfig(50, 1800, "ja"),
            new SearchConfig(50, 10)
        );
    }
    
    /**
     * 推奨エンジン設定
     * 
     * @param maxResults 最大推奨数
     * @param cacheTtl キャッシュTTL（秒）
     * @param algorithms 利用可能なアルゴリズム
     */
    public record RecommendationConfig(
        int maxResults,
        int cacheTtl,
        List<String> algorithms
    ) {
        public RecommendationConfig {
            // Java 21のコンパクトコンストラクタでバリデーション
            if (maxResults <= 0) {
                throw new IllegalArgumentException("maxResults must be positive");
            }
            if (cacheTtl < 0) {
                throw new IllegalArgumentException("cacheTtl must be non-negative");
            }
            if (algorithms.isEmpty()) {
                throw new IllegalArgumentException("algorithms must not be empty");
            }
        }
    }
    
    /**
     * チャットボット設定
     * 
     * @param maxConversationLength 最大会話長
     * @param sessionTimeout セッションタイムアウト（秒）
     * @param defaultLanguage デフォルト言語
     */
    public record ChatbotConfig(
        int maxConversationLength,
        int sessionTimeout,
        String defaultLanguage
    ) {
        public ChatbotConfig {
            if (maxConversationLength <= 0) {
                throw new IllegalArgumentException("maxConversationLength must be positive");
            }
            if (sessionTimeout <= 0) {
                throw new IllegalArgumentException("sessionTimeout must be positive");
            }
            if (defaultLanguage == null || defaultLanguage.trim().isEmpty()) {
                throw new IllegalArgumentException("defaultLanguage must not be null or empty");
            }
        }
    }
    
    /**
     * 検索設定
     * 
     * @param maxResults 最大検索結果数
     * @param autocompleteMaxResults オートコンプリート最大結果数
     */
    public record SearchConfig(
        int maxResults,
        int autocompleteMaxResults
    ) {
        public SearchConfig {
            if (maxResults <= 0) {
                throw new IllegalArgumentException("maxResults must be positive");
            }
            if (autocompleteMaxResults <= 0) {
                throw new IllegalArgumentException("autocompleteMaxResults must be positive");
            }
        }
    }
}
