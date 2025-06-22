package com.skishop.auth.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Skishop実行環境設定プロパティ
 */
@ConfigurationProperties(prefix = "skishop.runtime")
@Component
@Data
public class SkishopRuntimeProperties {
    
    /**
     * 実行環境の設定
     * local: ローカル開発環境（Redis + REST）
     * production: 本番環境（Azure Service Bus + Event Grid）
     */
    private Runtime environment = Runtime.LOCAL;
    
    /**
     * イベント伝播機能の有効/無効
     */
    private boolean eventPropagationEnabled = false;
    
    /**
     * ユーザー登録イベントの発行制御
     */
    private boolean userRegistrationEventEnabled = false;
    
    /**
     * ユーザー削除イベントの発行制御
     */
    private boolean userDeletionEventEnabled = false;
    
    /**
     * フォールバック動作の制御
     */
    private boolean fallbackToSyncProcessing = true;
    
    /**
     * デバッグモード
     */
    private boolean debugMode = false;
    
    /**
     * イベントブローカータイプ
     */
    private String eventBrokerType = "redis";
    
    /**
     * イベントタイムアウト（ミリ秒）
     */
    private long eventTimeoutMs = 30000;
    
    /**
     * Redis キープレフィックス
     */
    private String eventRedisKeyPrefix = "skishop";
    
    /**
     * イベント最大リトライ回数
     */
    private int eventMaxRetries = 3;
    
    /**
     * イベント並行実行数
     */
    private int eventConcurrency = 10;
    

    
    /**
     * Sagaタイムアウト設定
     */
    private SagaConfig saga = new SagaConfig();
    
    /**
     * リトライ設定
     */
    private RetryConfig retry = new RetryConfig();
    
    public enum Runtime {
        LOCAL, PRODUCTION
    }
    
    @Data
    public static class SagaConfig {
        private Duration timeout = Duration.ofSeconds(30);
        private int maxActiveSagas = 1000;
    }
    
    @Data
    public static class RetryConfig {
        private int maxAttempts = 3;
        private Duration initialDelay = Duration.ofMillis(1000);
        private double multiplier = 2.0;
        private Duration maxDelay = Duration.ofMillis(10000);
    }
    
    // メソッド
    public boolean isEventPropagationEnabled() {
        return eventPropagationEnabled;
    }
    
    public String getEventBrokerType() {
        return eventBrokerType;
    }
    
    public long getEventTimeoutMs() {
        return eventTimeoutMs;
    }
    
    public String getEventRedisKeyPrefix() {
        return eventRedisKeyPrefix;
    }
    
    public String getEnvironment() {
        return environment.name().toLowerCase();
    }
    
    public int getEventMaxRetries() {
        return eventMaxRetries;
    }
    
    public int getEventConcurrency() {
        return eventConcurrency;
    }
}
