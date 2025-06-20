package com.skishop.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * AI サポートサービス メインアプリケーション
 * 
 * Spring Boot 3.x + Java 21 + LangChain4j 1.1.0 + Azure OpenAI による
 * スキーショップ向けAIサポートサービス
 * 
 * 主な機能:
 * - チャットボット（商品推奨、技術アドバイス）
 * - 検索拡張（RAG）
 * - カスタマーサポート
 */
@SpringBootApplication
@EnableCaching      // Redis キャッシング有効化
@EnableMongoRepositories  // MongoDB リポジトリ有効化
@EnableAsync        // 非同期処理有効化
@EnableScheduling   // スケジュール処理有効化
public class AiSupportServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiSupportServiceApplication.class, args);
    }
}
