package com.skishop.auth.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

/**
 * アプリケーション設定の包括的な検証
 */
@Component
@Validated
@RequiredArgsConstructor
@Slf4j
public class ConfigurationValidator implements ApplicationListener<ApplicationReadyEvent> {

    private final SkishopRuntimeProperties runtimeProperties;
    
    @Value("${azure.servicebus.connection-string:}")
    private String serviceBusConnectionString;
    
    @Value("${azure.eventgrid.topic-endpoint:}")
    private String eventGridEndpoint;
    
    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;
    
    @Value("${spring.data.redis.port:6379}")
    private int redisPort;
    
    @Value("${spring.data.redis.password:}")
    private String redisPassword;
    
    @Value("${saga.retry.max-attempts:3}")
    private int maxRetryAttempts;
    
    @Value("${saga.timeout:30000}")
    private int sagaTimeout;
    
    @Value("${spring.datasource.url:}")
    private String datasourceUrl;
    
    @Value("${spring.datasource.username:}")
    private String datasourceUsername;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        try {
            validateConfiguration();
            log.info("Configuration validation completed successfully");
        } catch (Exception e) {
            log.error("Configuration validation failed: {}", e.getMessage());
            // 本番環境では起動を停止する場合もあり得る
            // System.exit(1);
        }
    }
    
    private void validateConfiguration() {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // 環境別設定の検証
        String environment = runtimeProperties.getEnvironment();
        log.info("Validating configuration for environment: {}", environment);
        
        if ("production".equals(environment)) {
            validateProductionConfiguration(errors, warnings);
        } else if ("staging".equals(environment)) {
            validateStagingConfiguration(errors, warnings);
        } else {
            validateLocalConfiguration(errors, warnings);
        }
        
        // 共通設定の検証
        validateCommonConfiguration(errors, warnings);
        
        // 警告をログ出力
        if (!warnings.isEmpty()) {
            log.warn("Configuration warnings found:\n{}", String.join("\n", warnings));
        }
        
        // エラーがある場合は例外をスロー
        if (!errors.isEmpty()) {
            String errorMessage = "設定検証エラー:\n" + String.join("\n", errors);
            throw new IllegalStateException(errorMessage);
        }
    }
    
    private void validateProductionConfiguration(List<String> errors, List<String> warnings) {
        // 本番環境必須設定
        if (!runtimeProperties.isEventPropagationEnabled()) {
            errors.add("本番環境ではイベント伝播を有効にする必要があります");
        }
        
        // Azure Service Bus設定チェック
        if ("azure-servicebus".equals(runtimeProperties.getEventBrokerType())) {
            if (serviceBusConnectionString == null || serviceBusConnectionString.trim().isEmpty()) {
                errors.add("本番環境のAzure Service Bus接続文字列が設定されていません");
            }
        }
        
        // セキュリティ設定チェック
        if (runtimeProperties.isDebugMode()) {
            warnings.add("本番環境でデバッグモードが有効になっています");
        }
        
        // データベース設定チェック
        if (datasourceUrl.contains("localhost") || datasourceUrl.contains("127.0.0.1")) {
            warnings.add("本番環境でローカルデータベースを使用している可能性があります");
        }
        
        // Redis設定チェック
        if ("localhost".equals(redisHost) || "127.0.0.1".equals(redisHost)) {
            warnings.add("本番環境でローカルRedisを使用している可能性があります");
        }
        
        if (redisPassword.isEmpty()) {
            warnings.add("本番環境でRedisパスワードが設定されていません");
        }
    }
    
    private void validateStagingConfiguration(List<String> errors, List<String> warnings) {
        // ステージング環境設定
        if (!runtimeProperties.isEventPropagationEnabled()) {
            warnings.add("ステージング環境でイベント伝播が無効になっています");
        }
        
        // Azure Service Bus設定チェック（ステージングでも推奨）
        if ("azure-servicebus".equals(runtimeProperties.getEventBrokerType())) {
            if (serviceBusConnectionString == null || serviceBusConnectionString.trim().isEmpty()) {
                warnings.add("ステージング環境でAzure Service Bus接続文字列が設定されていません");
            }
        }
    }
    
    private void validateLocalConfiguration(List<String> errors, List<String> warnings) {
        // ローカル環境設定
        if ("azure-servicebus".equals(runtimeProperties.getEventBrokerType()) && 
            (serviceBusConnectionString == null || serviceBusConnectionString.trim().isEmpty())) {
            warnings.add("ローカル環境でAzure Service Busが選択されていますが、接続文字列が設定されていません。Redisにフォールバックします。");
        }
        
        // Redis設定チェック
        if (redisHost == null || redisHost.trim().isEmpty()) {
            errors.add("Redisホストが設定されていません");
        }
        
        if (redisPort <= 0 || redisPort > 65535) {
            errors.add("無効なRedisポート番号です: " + redisPort);
        }
    }
    
    private void validateCommonConfiguration(List<String> errors, List<String> warnings) {
        // 基本設定チェック
        if (maxRetryAttempts < 0 || maxRetryAttempts > 10) {
            warnings.add("リトライ試行回数が推奨範囲外です (推奨: 0-10): " + maxRetryAttempts);
        }
        
        if (sagaTimeout < 1000 || sagaTimeout > 300000) { // 1秒 - 5分
            warnings.add("Sagaタイムアウトが推奨範囲外です (推奨: 1000-300000ms): " + sagaTimeout);
        }
        
        // データソース設定チェック
        if (datasourceUrl == null || datasourceUrl.trim().isEmpty()) {
            errors.add("データソースURLが設定されていません");
        }
        
        if (datasourceUsername == null || datasourceUsername.trim().isEmpty()) {
            errors.add("データソースユーザー名が設定されていません");
        }
        
        // イベント伝播設定チェック
        String eventBrokerType = runtimeProperties.getEventBrokerType();
        if (eventBrokerType == null || 
            (!eventBrokerType.equals("redis") && !eventBrokerType.equals("azure-servicebus"))) {
            errors.add("無効なイベントブローカータイプです: " + eventBrokerType);
        }
        
        // Runtime Propertiesの検証
        validateRuntimeProperties(errors, warnings);
    }
    
    private void validateRuntimeProperties(List<String> errors, List<String> warnings) {
        if (runtimeProperties.getEventTimeoutMs() < 5000 || runtimeProperties.getEventTimeoutMs() > 600000) {
            warnings.add("イベントタイムアウトが推奨範囲外です (推奨: 5000-600000ms): " + runtimeProperties.getEventTimeoutMs());
        }
        
        if (runtimeProperties.getEventMaxRetries() < 0 || runtimeProperties.getEventMaxRetries() > 5) {
            warnings.add("イベント最大リトライ回数が推奨範囲外です (推奨: 0-5): " + runtimeProperties.getEventMaxRetries());
        }
        
        if (runtimeProperties.getEventConcurrency() < 1 || runtimeProperties.getEventConcurrency() > 20) {
            warnings.add("イベント並行処理数が推奨範囲外です (推奨: 1-20): " + runtimeProperties.getEventConcurrency());
        }
        
        String redisKeyPrefix = runtimeProperties.getEventRedisKeyPrefix();
        if (redisKeyPrefix == null || redisKeyPrefix.trim().isEmpty()) {
            warnings.add("Redisキープレフィックスが設定されていません");
        } else if (redisKeyPrefix.contains(" ") || redisKeyPrefix.contains(":")) {
            warnings.add("Redisキープレフィックスに無効な文字が含まれています: " + redisKeyPrefix);
        }
    }
    
    /**
     * 機密情報をマスクする
     */
    private String maskConnectionString(String connectionString) {
        if (connectionString == null) {
            return null;
        }
        return connectionString.replaceAll("password=[^;]+", "password=****")
                            .replaceAll("Password=[^;]+", "Password=****")
                            .replaceAll("SharedAccessKey=[^;]+", "SharedAccessKey=****");
    }
}
