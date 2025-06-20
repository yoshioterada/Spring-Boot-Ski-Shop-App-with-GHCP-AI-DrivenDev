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
}
