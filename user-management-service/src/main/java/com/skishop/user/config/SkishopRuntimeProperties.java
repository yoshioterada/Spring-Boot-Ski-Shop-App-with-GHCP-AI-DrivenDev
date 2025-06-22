package com.skishop.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Skishopランタイム設定プロパティ
 * 統一された設定プロパティ管理
 */
@Configuration
@ConfigurationProperties(prefix = "skishop.runtime")
@Data
public class SkishopRuntimeProperties {

    /**
     * イベント伝搬機能の有効/無効
     */
    private boolean eventPropagationEnabled = false;

    /**
     * イベントブローカーの種類 (redis, azure-servicebus, kafka, etc.)
     */
    private String eventBrokerType = "redis";

    /**
     * イベント処理の最大リトライ回数
     */
    private int eventMaxRetries = 3;

    /**
     * イベント処理のタイムアウト（ミリ秒）
     */
    private long eventTimeoutMs = 30000;

    /**
     * Redisイベントキーのプレフィックス
     */
    private String eventRedisKeyPrefix = "skishop";

    /**
     * イベント処理の並列度
     */
    private int eventConcurrency = 4;

    /**
     * イベント永続化の有効/無効
     */
    private boolean eventPersistenceEnabled = true;

    /**
     * 処理済みイベントの保持期間（日）
     */
    private int processedEventRetentionDays = 30;

    /**
     * デバッグモードの有効/無効
     */
    private boolean debugMode = false;

    /**
     * 環境設定 (local, development, production)
     */
    private String environment = "local";

    /**
     * Azure Service Bus の設定
     */
    private AzureServiceBus azureServicebus = new AzureServiceBus();

    @Data
    public static class AzureServiceBus {
        /**
         * Azure Service Bus の有効/無効
         */
        private boolean enabled = false;

        /**
         * トピック名
         */
        private String topicName = "skishop-events-prod";

        /**
         * サブスクリプション名
         */
        private String subscriptionName = "user-service-subscription";

        /**
         * ステータスフィードバックトピック
         */
        private String statusFeedbackTopic = "skishop-status-feedback-prod";

        /**
         * 最大同時実行数
         */
        private int maxConcurrentCalls = 4;

        /**
         * 最大リトライ回数
         */
        private int maxRetries = 3;

        /**
         * プリフェッチカウント
         */
        private int prefetchCount = 10;
    }
}
