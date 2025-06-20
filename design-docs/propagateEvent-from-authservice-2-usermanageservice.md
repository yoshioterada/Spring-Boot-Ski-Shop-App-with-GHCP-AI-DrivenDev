# 認証サービスからユーザー管理サービスへのイベント伝播設計

## 概要

このドキュメントでは、`authentication-service`（認証サービス）と`user-management-service`（ユーザー管理サービス）間の堅牢で疎結合なイベント伝播を実現するための設計を詳述します。特にユーザー登録と削除イベントに焦点を当て、Sagaパターンを使用します。この設計は、ローカル開発環境（Redis Streams + REST API）と本番環境（Azure Service Bus + Azure Event Grid）の両方の実装をカバーします。

## システムアーキテクチャ

### アーキテクチャ図

```ascii
+------------------+        イベント        +----------------------+
|                  |     ------------>    |                      |
| 認証サービス      |                      |  ユーザー管理サービス  |
|                  |     <------------    |                      |
+------------------+   ステータス更新      +----------------------+
        |                                           |
        | イベント発行                                | イベント購読/処理
        |                                           |
        v                                           v
+------------------+                      +----------------------+
|                  |                      |                      |
| メッセージブローカー|                      |  メッセージブローカー  |
|                  |                      |                      |
+------------------+                      +----------------------+
 ローカル: Redis Streams                    ローカル: Redisコンシューマー
 本番: Azure Service Bus                   本番: Azure Service Bus
```

### 設計原則

1. **疎結合**: 認証サービスとユーザー管理サービスは独立して動作し、イベントを通じてのみ通信します。
2. **回復性**: 一方のサービスが一時的に利用できなくても、もう一方のサービスは機能し続けることができます。
3. **一貫性**: Sagaパターンにより、分散操作の最終的な一貫性を確保します。
4. **観測可能性**: すべてのイベントと操作のための包括的なメトリクスとモニタリングを提供します。
5. **冪等性**: 重複処理を防ぐために操作は冪等性を持ちます。

## イベントスキーマ

### ユーザー登録イベント

```json
{
  "eventId": "uuid-文字列",
  "eventType": "USER_REGISTERED",
  "timestamp": "ISO-8601-タイムスタンプ",
  "version": "1.0",
  "producer": "authentication-service",
  "payload": {
    "userId": "uuid-文字列",
    "email": "user@example.com",
    "firstName": "太郎",
    "lastName": "山田",
    "phoneNumber": "+81123456789",
    "status": "PENDING_VERIFICATION",
    "createdAt": "ISO-8601-タイムスタンプ",
    "additionalAttributes": {
      "key1": "value1",
      "key2": "value2"
    }
  },
  "correlationId": "uuid-文字列",
  "sagaId": "uuid-文字列",
  "retry": 0
}
```

### ユーザー削除イベント

```json
{
  "eventId": "uuid-文字列",
  "eventType": "USER_DELETED",
  "timestamp": "ISO-8601-タイムスタンプ",
  "version": "1.0",
  "producer": "authentication-service",
  "payload": {
    "userId": "uuid-文字列",
    "reason": "USER_REQUESTED",
    "deletedAt": "ISO-8601-タイムスタンプ"
  },
  "correlationId": "uuid-文字列",
  "sagaId": "uuid-文字列",
  "retry": 0
}
```

### ステータス更新イベント（ユーザー管理サービスからのフィードバック）

```json
{
  "eventId": "uuid-文字列",
  "eventType": "USER_MANAGEMENT_STATUS",
  "timestamp": "ISO-8601-タイムスタンプ",
  "version": "1.0",
  "producer": "user-management-service",
  "payload": {
    "userId": "uuid-文字列",
    "originalEventId": "uuid-文字列",
    "status": "SUCCESS|FAILED",
    "reason": "失敗した場合のオプションエラーメッセージ",
    "processingTime": 123 // ミリ秒
  },
  "correlationId": "uuid-文字列",
  "sagaId": "uuid-文字列"
}
```

## ステータス定義

### ユーザー登録ステータス

ユーザー登録プロセスにおける各段階のステータスとその意味を以下に定義します。

#### 登録時の認証サービス側ステータス

| ステータス | 説明 | 発生タイミング | 次のアクション |
|-----------|------|--------------|---------------|
| `PENDING_REGISTRATION` | ユーザー登録要求を受信、認証サービスでの処理開始 | ユーザー登録API呼び出し時 | ユーザー情報の検証とアカウント作成 |
| `ACCOUNT_CREATED` | 認証サービスでユーザーアカウント作成完了 | ユーザーアカウントDB保存後 | ユーザー管理サービスへのイベント発行 |
| `EVENT_PUBLISHED` | ユーザー登録イベントの発行成功 | メッセージブローカーへの発行完了後 | ユーザー管理サービスからの応答待機 |
| `EVENT_PUBLISH_FAILED` | ユーザー登録イベントの発行失敗 | メッセージブローカーへの発行失敗時 | リトライまたは補償処理 |
| `PENDING_USER_MANAGEMENT` | ユーザー管理サービスでの処理待機中 | イベント発行後、応答受信前 | タイムアウト監視 |
| `REGISTRATION_COMPLETED` | ユーザー登録プロセス全体の完了 | ユーザー管理サービスからSUCCESS応答受信時 | 登録完了通知（メール等） |
| `REGISTRATION_FAILED` | ユーザー登録プロセスの失敗 | ユーザー管理サービスからFAILED応答受信時 | エラー処理と補償トランザクション |
| `COMPENSATION_REQUIRED` | 補償処理が必要な状態 | 下流サービスでの失敗検出時 | 作成済みアカウントの削除または無効化 |
| `COMPENSATED` | 補償処理完了 | 補償トランザクション実行後 | 失敗通知とクリーンアップ |

#### 登録時のユーザー管理サービス側ステータス

| ステータス | 説明 | 発生タイミング | 次のアクション |
|-----------|------|--------------|---------------|
| `EVENT_RECEIVED` | ユーザー登録イベントを受信 | メッセージブローカーからのイベント受信時 | イベント内容の検証 |
| `VALIDATION_IN_PROGRESS` | イベントデータの検証中 | ユーザーデータの妥当性チェック開始時 | 必須フィールドとビジネスルールの検証 |
| `VALIDATION_PASSED` | イベントデータ検証成功 | 全ての検証項目通過時 | ユーザープロファイル作成処理開始 |
| `VALIDATION_FAILED` | イベントデータ検証失敗 | 検証エラー検出時 | FAILED応答の送信 |
| `PROFILE_CREATION_IN_PROGRESS` | ユーザープロファイル作成中 | DB操作開始時 | データベーストランザクション実行 |
| `PROFILE_CREATED` | ユーザープロファイル作成成功 | DB保存完了時 | SUCCESS応答の送信 |
| `PROFILE_CREATION_FAILED` | ユーザープロファイル作成失敗 | DB操作エラー時 | FAILED応答の送信とエラーログ記録 |
| `DUPLICATE_USER_DETECTED` | 重複ユーザーの検出 | 既存ユーザーとの重複チェック時 | FAILED応答の送信（重複エラー） |
| `PROCESSING_TIMEOUT` | 処理タイムアウト | 設定時間内に処理が完了しない場合 | FAILED応答の送信とリソースクリーンアップ |

### ユーザー削除ステータス

ユーザー削除プロセスにおける各段階のステータスとその意味を以下に定義します。

#### 削除時の認証サービス側ステータス

| ステータス | 説明 | 発生タイミング | 次のアクション |
|-----------|------|--------------|---------------|
| `PENDING_DELETION` | ユーザー削除要求を受信、処理開始 | ユーザー削除API呼び出し時 | ユーザー存在確認と削除権限チェック |
| `DELETION_AUTHORIZED` | 削除権限の確認完了 | 削除権限検証通過時 | 認証サービスでの論理削除実行 |
| `ACCOUNT_SOFT_DELETED` | 認証サービスでアカウント論理削除完了 | ユーザーアカウントの無効化後 | ユーザー管理サービスへの削除イベント発行 |
| `DELETION_EVENT_PUBLISHED` | ユーザー削除イベントの発行成功 | メッセージブローカーへの発行完了後 | ユーザー管理サービスからの応答待機 |
| `DELETION_EVENT_PUBLISH_FAILED` | ユーザー削除イベントの発行失敗 | メッセージブローカーへの発行失敗時 | リトライまたは補償処理 |
| `PENDING_USER_MANAGEMENT_DELETION` | ユーザー管理サービスでの削除処理待機中 | イベント発行後、応答受信前 | タイムアウト監視 |
| `DELETION_COMPLETED` | ユーザー削除プロセス全体の完了 | ユーザー管理サービスからSUCCESS応答受信時 | 削除完了処理とクリーンアップ |
| `DELETION_FAILED` | ユーザー削除プロセスの失敗 | ユーザー管理サービスからFAILED応答受信時 | エラー処理と削除の巻き戻し |
| `DELETION_ROLLBACK_REQUIRED` | 削除の巻き戻しが必要な状態 | 下流サービスでの削除失敗検出時 | アカウントの復旧処理 |
| `DELETION_ROLLED_BACK` | 削除巻き戻し完了 | アカウント復旧処理完了後 | 失敗通知と状態の正常化 |

#### 削除時のユーザー管理サービス側ステータス

| ステータス | 説明 | 発生タイミング | 次のアクション |
|-----------|------|--------------|---------------|
| `DELETION_EVENT_RECEIVED` | ユーザー削除イベントを受信 | メッセージブローカーからのイベント受信時 | イベント内容の検証 |
| `DELETION_VALIDATION_IN_PROGRESS` | 削除イベントデータの検証中 | 削除対象ユーザーの存在確認開始時 | ユーザー存在チェックと関連データの確認 |
| `DELETION_VALIDATION_PASSED` | 削除イベントデータ検証成功 | 削除対象の確認完了時 | ユーザープロファイル削除処理開始 |
| `DELETION_VALIDATION_FAILED` | 削除イベントデータ検証失敗 | ユーザー不存在または検証エラー時 | FAILED応答の送信 |
| `PROFILE_DELETION_IN_PROGRESS` | ユーザープロファイル削除中 | 関連データの削除処理開始時 | データベーストランザクション実行 |
| `RELATED_DATA_CLEANUP_IN_PROGRESS` | 関連データのクリーンアップ中 | プロファイル以外の関連データ削除時 | 履歴、設定、キャッシュ等の削除 |
| `PROFILE_DELETED` | ユーザープロファイル削除成功 | 全ての関連データ削除完了時 | SUCCESS応答の送信 |
| `PROFILE_DELETION_FAILED` | ユーザープロファイル削除失敗 | 削除処理中のエラー時 | FAILED応答の送信とエラーログ記録 |
| `USER_NOT_FOUND` | 削除対象ユーザーが見つからない | ユーザー検索結果が空の場合 | SUCCESS応答の送信（冪等性のため） |
| `DELETION_TIMEOUT` | 削除処理タイムアウト | 設定時間内に削除が完了しない場合 | FAILED応答の送信と処理の中断 |

### Sagaステータス

Sagaパターンにおけるオーケストレーションステータスを以下に定義します。

#### Saga全体のステータス

| ステータス | 説明 | 適用対象 | 遷移条件 |
|-----------|------|---------|---------|
| `SAGA_STARTED` | Sagaトランザクション開始 | 登録・削除共通 | Saga開始時 |
| `SAGA_IN_PROGRESS` | Sagaトランザクション実行中 | 登録・削除共通 | 各ステップの実行中 |
| `SAGA_STEP_COMPLETED` | Sagaステップ完了 | 登録・削除共通 | 個別ステップの成功時 |
| `SAGA_STEP_FAILED` | Sagaステップ失敗 | 登録・削除共通 | 個別ステップの失敗時 |
| `SAGA_COMPENSATING` | 補償処理実行中 | 登録・削除共通 | 失敗時の巻き戻し処理中 |
| `SAGA_COMPENSATED` | 補償処理完了 | 登録・削除共通 | 巻き戻し処理成功時 |
| `SAGA_COMPENSATION_FAILED` | 補償処理失敗 | 登録・削除共通 | 巻き戻し処理失敗時 |
| `SAGA_COMPLETED` | Sagaトランザクション成功完了 | 登録・削除共通 | 全ステップ成功時 |
| `SAGA_FAILED` | Sagaトランザクション失敗 | 登録・削除共通 | 補償処理完了またはタイムアウト時 |
| `SAGA_TIMEOUT` | Sagaトランザクションタイムアウト | 登録・削除共通 | 設定時間内に完了しない場合 |

### ステータス遷移図

#### ユーザー登録のステータス遷移

```ascii
[PENDING_REGISTRATION] → [ACCOUNT_CREATED] → [EVENT_PUBLISHED]
                              ↓                    ↓
                         [REGISTRATION_FAILED] ← [EVENT_PUBLISH_FAILED]
                              ↓                    ↓
                         [COMPENSATION_REQUIRED] → [COMPENSATED]
                              ↑
[EVENT_PUBLISHED] → [PENDING_USER_MANAGEMENT] → [REGISTRATION_COMPLETED]
                                               ↘
                                                [REGISTRATION_FAILED]
```

#### ユーザー削除のステータス遷移

```ascii
[PENDING_DELETION] → [DELETION_AUTHORIZED] → [ACCOUNT_SOFT_DELETED]
                                                      ↓
[DELETION_EVENT_PUBLISHED] → [PENDING_USER_MANAGEMENT_DELETION]
         ↓                                           ↓
[DELETION_EVENT_PUBLISH_FAILED]                [DELETION_COMPLETED]
         ↓                                           ↓
[DELETION_ROLLBACK_REQUIRED] → [DELETION_ROLLED_BACK]
                            ↗
                       [DELETION_FAILED]
```

### エラーハンドリングとステータス管理

#### リトライ可能エラー

以下のステータス時には自動リトライを実行します：

1. **`EVENT_PUBLISH_FAILED`** - メッセージブローカーへの発行失敗
2. **`DELETION_EVENT_PUBLISH_FAILED`** - 削除イベントの発行失敗
3. **`PROFILE_CREATION_FAILED`** - 一時的なDB接続エラー
4. **`PROFILE_DELETION_FAILED`** - 一時的なDB接続エラー

#### 非リトライエラー

以下のステータス時にはリトライせず、即座に失敗処理を行います：

1. **`VALIDATION_FAILED`** - データ検証エラー
2. **`DUPLICATE_USER_DETECTED`** - ユーザー重複エラー
3. **`DELETION_VALIDATION_FAILED`** - 削除対象不正エラー

#### タイムアウト設定

| 処理段階 | タイムアウト時間 | タイムアウト時のステータス |
|---------|----------------|-------------------------|
| ユーザー登録Saga全体 | 30秒 | `SAGA_TIMEOUT` |
| ユーザー削除Saga全体 | 60秒 | `SAGA_TIMEOUT` |
| 個別イベント処理 | 10秒 | `PROCESSING_TIMEOUT` |
| DB操作 | 5秒 | `PROFILE_CREATION_FAILED` |

## セキュリティ考慮事項

### 設定値のセキュリティ

#### 機密情報の適切なマスキング

```java
@Component
public class ConfigurationSecurityManager {
    private static final Set<String> SENSITIVE_KEYS = Set.of(
        "password", "secret", "key", "token", "credential"
    );
    
    public String maskSensitiveValue(String key, String value) {
        if (isSensitiveKey(key)) {
            return value.length() > 4 ? 
                value.substring(0, 4) + "*".repeat(value.length() - 4) : 
                "*".repeat(value.length());
        }
        return value;
    }
    
    private boolean isSensitiveKey(String key) {
        String lowerKey = key.toLowerCase();
        return SENSITIVE_KEYS.stream().anyMatch(lowerKey::contains);
    }
    
    public void logConfigurationSafely(Map<String, String> config) {
        config.forEach((key, value) -> {
            String maskedValue = maskSensitiveValue(key, value);
            log.info("設定: {}={}", key, maskedValue);
        });
    }
}
```

#### 暗号化設定の実装

```java
@Configuration
@EnableConfigurationProperties(EncryptionProperties.class)
public class SecurityConfiguration {
    
    @Bean
    public StringEncryptor stringEncryptor(EncryptionProperties properties) {
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        encryptor.setPassword(properties.getPassword());
        encryptor.setAlgorithm("PBEWITHHMACSHA512ANDAES_256");
        encryptor.setKeyObtentionIterations(1000);
        encryptor.setPoolSize(4);
        encryptor.setProviderName("SunJCE");
        encryptor.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");
        encryptor.setStringOutputType("base64");
        return encryptor;
    }
}

@ConfigurationProperties(prefix = "encryption")
@Data
public class EncryptionProperties {
    private String password = System.getenv("ENCRYPTION_PASSWORD");
}
```

#### セキュアな設定例

```yaml
# application-security.yml
security:
  encryption:
    password: ${ENCRYPTION_PASSWORD}
  
azure:
  servicebus:
    connection-string: ENC(encrypted_connection_string)
    
spring:
  datasource:
    password: ENC(encrypted_db_password)
    
logging:
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level [%X{correlationId}] %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level [%X{correlationId}] %logger{36} - %msg%n"
```

### イベントデータの保護

#### ペイロード暗号化

```java
@Service
public class EventEncryptionService {
    private final StringEncryptor encryptor;
    private final ObjectMapper objectMapper;
    
    public String encryptPayload(Object payload) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(payload);
        return encryptor.encrypt(json);
    }
    
    public <T> T decryptPayload(String encryptedPayload, Class<T> clazz) 
            throws JsonProcessingException {
        String decrypted = encryptor.decrypt(encryptedPayload);
        return objectMapper.readValue(decrypted, clazz);
    }
}
```

## 運用考慮事項

### 包括的なヘルスチェック実装

#### 詳細なヘルスチェック

```java
@Component
public class ComprehensiveHealthIndicator implements HealthIndicator {
    private final EventPublisher primaryPublisher;
    private final EventPublisher fallbackPublisher;
    private final DataSource dataSource;
    private final RedisTemplate<String, String> redisTemplate;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    
    @Override
    public Health health() {
        Health.Builder builder = Health.up();
        
        // データベース接続チェック
        checkDatabase(builder);
        
        // Redis接続チェック
        checkRedis(builder);
        
        // イベントパブリッシャーの健全性チェック
        checkEventPublishers(builder);
        
        // サーキットブレーカーの状態チェック
        checkCircuitBreakers(builder);
        
        // メモリ使用量チェック
        checkMemoryUsage(builder);
        
        // ディスク使用量チェック
        checkDiskUsage(builder);
        
        return builder.build();
    }
    
    private void checkDatabase(Health.Builder builder) {
        try (Connection connection = dataSource.getConnection()) {
            boolean isValid = connection.isValid(5);
            builder.withDetail("database.connection", isValid ? "healthy" : "unhealthy");
            builder.withDetail("database.url", maskConnectionString(connection.getMetaData().getURL()));
        } catch (Exception e) {
            builder.withDetail("database.connection", "failed");
            builder.withDetail("database.error", e.getMessage());
        }
    }
    
    private void checkRedis(Health.Builder builder) {
        try {
            String pong = redisTemplate.getConnectionFactory()
                .getConnection().ping();
            builder.withDetail("redis.connection", "PONG".equals(pong) ? "healthy" : "unhealthy");
        } catch (Exception e) {
            builder.withDetail("redis.connection", "failed");
            builder.withDetail("redis.error", e.getMessage());
        }
    }
    
    private void checkEventPublishers(Health.Builder builder) {
        // 主要パブリッシャーの健全性チェック
        boolean primaryHealthy = performPublisherHealthCheck(primaryPublisher);
        builder.withDetail("publisher.primary", primaryHealthy ? "healthy" : "unhealthy");
        
        // フォールバックパブリッシャーの健全性チェック
        boolean fallbackHealthy = performPublisherHealthCheck(fallbackPublisher);
        builder.withDetail("publisher.fallback", fallbackHealthy ? "healthy" : "unhealthy");
    }
    
    private void checkCircuitBreakers(Health.Builder builder) {
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(cb -> {
            CircuitBreaker.State state = cb.getState();
            builder.withDetail("circuitBreaker." + cb.getName(), state.toString());
            
            CircuitBreaker.Metrics metrics = cb.getMetrics();
            builder.withDetail("circuitBreaker." + cb.getName() + ".failureRate", 
                metrics.getFailureRate());
        });
    }
    
    private void checkMemoryUsage(Health.Builder builder) {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        
        long used = heapUsage.getUsed();
        long max = heapUsage.getMax();
        double usagePercent = (double) used / max * 100;
        
        builder.withDetail("memory.heap.used", used);
        builder.withDetail("memory.heap.max", max);
        builder.withDetail("memory.heap.usagePercent", String.format("%.2f%%", usagePercent));
        
        if (usagePercent > 90) {
            builder.down().withDetail("memory.status", "critical");
        } else if (usagePercent > 80) {
            builder.withDetail("memory.status", "warning");
        }
    }
    
    private void checkDiskUsage(Health.Builder builder) {
        File root = new File("/");
        long totalSpace = root.getTotalSpace();
        long freeSpace = root.getFreeSpace();
        long usedSpace = totalSpace - freeSpace;
        double usagePercent = (double) usedSpace / totalSpace * 100;
        
        builder.withDetail("disk.total", totalSpace);
        builder.withDetail("disk.used", usedSpace);
        builder.withDetail("disk.free", freeSpace);
        builder.withDetail("disk.usagePercent", String.format("%.2f%%", usagePercent));
        
        if (usagePercent > 95) {
            builder.down().withDetail("disk.status", "critical");
        } else if (usagePercent > 85) {
            builder.withDetail("disk.status", "warning");
        }
    }
    
    private boolean performPublisherHealthCheck(EventPublisher publisher) {
        try {
            // ヘルスチェック専用の軽量なテストイベントを送信
            Map<String, Object> healthCheckPayload = Map.of(
                "type", "HEALTH_CHECK",
                "timestamp", System.currentTimeMillis()
            );
            
            return publisher.publishEventSync("HEALTH_CHECK", healthCheckPayload, "health-check");
        } catch (Exception e) {
            log.warn("パブリッシャーヘルスチェックに失敗: {}", e.getMessage());
            return false;
        }
    }
    
    private String maskConnectionString(String url) {
        // 接続文字列から機密情報をマスク
        return url.replaceAll("password=[^;]+", "password=****");
    }
}
```

### 詳細なメトリクス収集サービス

#### 包括的なメトリクス実装

```java
@Component
public class ComprehensiveMetricsService {
    private final MeterRegistry meterRegistry;
    private final Timer eventPublishingTimer;
    private final Timer eventProcessingTimer;
    private final Counter eventSuccessCounter;
    private final Counter eventFailureCounter;
    private final Gauge sagaActiveGauge;
    private final DistributionSummary payloadSizeDistribution;
    
    // リアルタイムでアクティブなSaga数を追跡
    private final AtomicLong activeSagaCount = new AtomicLong(0);
    
    public ComprehensiveMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // イベント発行時間メトリクス
        this.eventPublishingTimer = Timer.builder("events.publishing.duration")
            .description("イベント発行にかかる時間")
            .register(meterRegistry);
            
        // イベント処理時間メトリクス
        this.eventProcessingTimer = Timer.builder("events.processing.duration")
            .description("イベント処理にかかる時間")
            .register(meterRegistry);
            
        // 成功イベントカウンター
        this.eventSuccessCounter = Counter.builder("events.success.total")
            .description("成功したイベント数")
            .register(meterRegistry);
            
        // 失敗イベントカウンター
        this.eventFailureCounter = Counter.builder("events.failure.total")
            .description("失敗したイベント数")
            .register(meterRegistry);
            
        // アクティブなSaga数ゲージ
        this.sagaActiveGauge = Gauge.builder("saga.active.count")
            .description("アクティブなSaga数")
            .register(meterRegistry, activeSagaCount, AtomicLong::get);
            
        // ペイロードサイズ分布
        this.payloadSizeDistribution = DistributionSummary.builder("events.payload.size")
            .description("イベントペイロードサイズ分布")
            .baseUnit("bytes")
            .register(meterRegistry);
    }
    
    public void recordEventPublished(String eventType, String environment, long durationMs, int payloadSize) {
        eventPublishingTimer.record(durationMs, TimeUnit.MILLISECONDS, 
            Tags.of("eventType", eventType, "environment", environment, "status", "success"));
        eventSuccessCounter.increment(Tags.of("eventType", eventType, "environment", environment));
        payloadSizeDistribution.record(payloadSize);
    }
    
    public void recordEventFailure(String eventType, String environment, long durationMs, String errorType, String errorMessage) {
        eventPublishingTimer.record(durationMs, TimeUnit.MILLISECONDS, 
            Tags.of("eventType", eventType, "environment", environment, "status", "failure"));
        eventFailureCounter.increment(Tags.of("eventType", eventType, "environment", environment, "errorType", errorType));
        
        // エラー詳細をカスタムメトリクスとして記録
        Counter.builder("events.errors.detail")
            .description("エラー詳細")
            .tag("errorType", errorType)
            .tag("errorMessage", truncateErrorMessage(errorMessage))
            .register(meterRegistry)
            .increment();
    }
    
    public void recordEventProcessed(String eventType, String environment, long processingTimeMs) {
        eventProcessingTimer.record(processingTimeMs, TimeUnit.MILLISECONDS,
            Tags.of("eventType", eventType, "environment", environment, "status", "success"));
    }
    
    public void recordSagaStarted(String sagaType) {
        activeSagaCount.incrementAndGet();
        Counter.builder("saga.started.total")
            .description("開始されたSaga数")
            .tag("sagaType", sagaType)
            .register(meterRegistry)
            .increment();
    }
    
    public void recordSagaCompleted(String sagaType, long durationMs, boolean success) {
        activeSagaCount.decrementAndGet();
        
        Timer.builder("saga.duration")
            .description("Saga完了時間")
            .tag("sagaType", sagaType)
            .tag("status", success ? "success" : "failed")
            .register(meterRegistry)
            .record(durationMs, TimeUnit.MILLISECONDS);
    }
    
    public void recordRetryAttempt(String operation, int attemptNumber, String reason) {
        Counter.builder("retry.attempts.total")
            .description("リトライ試行回数")
            .tag("operation", operation)
            .tag("attemptNumber", String.valueOf(attemptNumber))
            .tag("reason", reason)
            .register(meterRegistry)
            .increment();
    }
    
    public void recordCircuitBreakerEvent(String circuitBreakerName, String event) {
        Counter.builder("circuitbreaker.events.total")
            .description("サーキットブレーカーイベント")
            .tag("name", circuitBreakerName)
            .tag("event", event)
            .register(meterRegistry)
            .increment();
    }
    
    // ビジネスメトリクス
    public void recordUserRegistrationMetrics(String status, long processingTimeMs) {
        Timer.builder("business.user.registration.duration")
            .description("ユーザー登録処理時間")
            .tag("status", status)
            .register(meterRegistry)
            .record(processingTimeMs, TimeUnit.MILLISECONDS);
    }
    
    public void recordUserDeletionMetrics(String status, long processingTimeMs) {
        Timer.builder("business.user.deletion.duration")
            .description("ユーザー削除処理時間")
            .tag("status", status)
            .register(meterRegistry)
            .record(processingTimeMs, TimeUnit.MILLISECONDS);
    }
    
    private String truncateErrorMessage(String message) {
        return message != null && message.length() > 100 ? 
            message.substring(0, 100) + "..." : message;
    }
}
```

### 設定値の検証ロジック

#### 包括的な設定検証

```java
@Component
@Validated
public class ConfigurationValidator implements ApplicationListener<ApplicationReadyEvent> {
    
    @Value("${azure.servicebus.connection-string:}")
    private String serviceBusConnectionString;
    
    @Value("${azure.eventgrid.topic-endpoint:}")
    private String eventGridEndpoint;
    
    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;
    
    @Value("${spring.data.redis.port:6379}")
    private int redisPort;
    
    @Value("${saga.retry.max-attempts:3}")
    private int maxRetryAttempts;
    
    @Value("${saga.timeout:30000}")
    private int sagaTimeout;
    
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        validateConfiguration();
    }
    
    private void validateConfiguration() {
        List<String> errors = new ArrayList<>();
        
        // 環境別設定の検証
        String runtime = System.getProperty("skishop.runtime", "local");
        
        if ("production".equals(runtime)) {
            validateProductionConfiguration(errors);
        } else {
            validateLocalConfiguration(errors);
        }
        
        // 共通設定の検証
        validateCommonConfiguration(errors);
        
        if (!errors.isEmpty()) {
            String errorMessage = "設定検証エラー:\n" + String.join("\n", errors);
            throw new IllegalStateException(errorMessage);
        }
        
        log.info("設定検証が正常に完了しました");
    }
    
    private void validateProductionConfiguration(List<String> errors) {
        if (StringUtils.isBlank(serviceBusConnectionString)) {
            errors.add("本番環境ではazure.servicebus.connection-stringが必須です");
        } else if (!isValidServiceBusConnectionString(serviceBusConnectionString)) {
            errors.add("azure.servicebus.connection-stringの形式が不正です");
        }
        
        if (StringUtils.isBlank(eventGridEndpoint)) {
            errors.add("本番環境ではazure.eventgrid.topic-endpointが必須です");
        } else if (!isValidUrl(eventGridEndpoint)) {
            errors.add("azure.eventgrid.topic-endpointの形式が不正です");
        }
    }
    
    private void validateLocalConfiguration(List<String> errors) {
        if (StringUtils.isBlank(redisHost)) {
            errors.add("ローカル環境ではspring.data.redis.hostが必須です");
        }
        
        if (redisPort <= 0 || redisPort > 65535) {
            errors.add("spring.data.redis.portは1-65535の範囲で設定してください");
        }
    }
    
    private void validateCommonConfiguration(List<String> errors) {
        if (maxRetryAttempts < 1 || maxRetryAttempts > 10) {
            errors.add("saga.retry.max-attemptsは1-10の範囲で設定してください");
        }
        
        if (sagaTimeout < 1000 || sagaTimeout > 300000) {
            errors.add("saga.timeoutは1000-300000ms（1秒-5分）の範囲で設定してください");
        }
    }
    
    private boolean isValidServiceBusConnectionString(String connectionString) {
        return connectionString.contains("Endpoint=") && 
               connectionString.contains("SharedAccessKeyName=") && 
               connectionString.contains("SharedAccessKey=");
    }
    
    private boolean isValidUrl(String url) {
        try {
            new URL(url);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }
}
```

## 環境構築手順

### ローカル開発環境の構築

#### 前提条件

- Java 17以上
- Maven 3.8以上またはGradle 8.0以上
- Docker Desktop（Redis用）
- IDE（IntelliJ IDEA、Eclipse、VS Codeなど）

#### 1. Redisサーバーの起動

**Docker Composeを使用した方法:**

```yaml
# docker-compose.yml
version: '3.8'
services:
  redis:
    image: redis:7.2-alpine
    container_name: skishop-redis
    ports:
      - "6379:6379"
    command: redis-server --appendonly yes
    volumes:
      - redis-data:/data
    environment:
      - REDIS_PASSWORD=skishop-redis-password
    networks:
      - skishop-network

  redis-commander:
    image: rediscommander/redis-commander:latest
    container_name: redis-commander
    hostname: redis-commander
    ports:
      - "8081:8081"
    environment:
      - REDIS_HOSTS=local:redis:6379:0:skishop-redis-password
    networks:
      - skishop-network
    depends_on:
      - redis

volumes:
  redis-data:

networks:
  skishop-network:
    driver: bridge
```

**起動コマンド:**

```bash
# Docker Composeでサービス起動
docker-compose up -d

# Redis接続確認
docker exec -it skishop-redis redis-cli
127.0.0.1:6379> AUTH skishop-redis-password
127.0.0.1:6379> ping
PONG
```

#### 2. データベースの準備

**PostgreSQL（Docker使用）:**

```yaml
# docker-compose.ymlに追加
  postgres:
    image: postgres:15-alpine
    container_name: skishop-postgres
    environment:
      POSTGRES_DB: skishop
      POSTGRES_USER: skishop
      POSTGRES_PASSWORD: skishop-password
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data
    networks:
      - skishop-network

volumes:
  postgres-data:
```

**データベーススキーマ作成:**

```sql
-- Authentication Service用テーブル
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE saga_states (
    saga_id VARCHAR(255) PRIMARY KEY,
    saga_type VARCHAR(100) NOT NULL,
    state VARCHAR(50) NOT NULL,
    entity_id VARCHAR(255),
    start_time BIGINT NOT NULL,
    data JSONB,
    error_reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- User Management Service用テーブル
CREATE TABLE user_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone_number VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE processed_events (
    event_id VARCHAR(255) PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    processed BOOLEAN DEFAULT TRUE,
    success BOOLEAN DEFAULT FALSE,
    processing_time BIGINT,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### 3. アプリケーションの設定

**Authentication Service設定:**

```bash
# application-local.yml作成
mkdir -p src/main/resources
cat > src/main/resources/application-local.yml << 'EOF'
# 詳細設定は後述の設定プロパティ一覧を参照
EOF
```

**User Management Service設定:**

```bash
# 同様にapplication-local.yml作成
```

#### 4. 依存関係の追加

**Maven依存関係追加:**

```xml
<!-- pom.xmlに追加 -->
<dependencies>
    <!-- 既存の依存関係... -->
    
    <!-- Spring Data Redis Streams -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
    
    <!-- 詳細な依存関係は前述のライブラリ一覧を参照 -->
</dependencies>
```

#### 5. 開発環境での動作確認

```bash
# Authentication Serviceの起動
cd authentication-service
mvn spring-boot:run -Dspring-boot.run.profiles=local

# User Management Serviceの起動（別ターミナル）
cd user-management-service
mvn spring-boot:run -Dspring-boot.run.profiles=local

# ヘルスチェック
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health

# Redis Streams確認
curl http://localhost:8081:8081  # Redis Commander
```

### 本番環境の構築

#### 前提条件

- Azure サブスクリプション
- Azure CLI 2.50以上
- Terraform 1.5以上（インフラ構築用）
- kubectl（AKS使用時）

#### 1. Azureリソースの作成

**Service Busの作成:**

```bash
# リソースグループ作成
az group create --name skishop-prod-rg --location japaneast

# Service Bus Namespace作成
az servicebus namespace create \
  --resource-group skishop-prod-rg \
  --name skishop-servicebus \
  --location japaneast \
  --sku Standard

# キュー作成
az servicebus queue create \
  --resource-group skishop-prod-rg \
  --namespace-name skishop-servicebus \
  --name auth-user-events \
  --max-size 1024

# 接続文字列取得
az servicebus namespace authorization-rule keys list \
  --resource-group skishop-prod-rg \
  --namespace-name skishop-servicebus \
  --name RootManageSharedAccessKey \
  --query primaryConnectionString --output tsv
```

**Event Gridの作成:**

```bash
# Event Grid Topic作成
az eventgrid topic create \
  --resource-group skishop-prod-rg \
  --name skishop-events \
  --location japaneast

# Topic Endpoint取得
az eventgrid topic show \
  --resource-group skishop-prod-rg \
  --name skishop-events \
  --query endpoint --output tsv

# Access Key取得
az eventgrid topic key list \
  --resource-group skishop-prod-rg \
  --name skishop-events \
  --query key1 --output tsv
```

**Azure Database for PostgreSQLの作成:**

```bash
# PostgreSQL Serverの作成
az postgres flexible-server create \
  --resource-group skishop-prod-rg \
  --name skishop-postgres \
  --location japaneast \
  --admin-user skishop \
  --admin-password 'SecurePassword123!' \
  --sku-name Standard_B1ms \
  --tier Burstable \
  --storage-size 32

# データベース作成
az postgres flexible-server db create \
  --resource-group skishop-prod-rg \
  --server-name skishop-postgres \
  --database-name skishop
```

#### 2. Kubernetesクラスターの準備（AKS使用時）

```bash
# AKSクラスター作成
az aks create \
  --resource-group skishop-prod-rg \
  --name skishop-aks \
  --node-count 3 \
  --node-vm-size Standard_D2s_v3 \
  --enable-addons monitoring \
  --generate-ssh-keys

# kubectl認証設定
az aks get-credentials \
  --resource-group skishop-prod-rg \
  --name skishop-aks
```

#### 3. シークレット管理の設定

**Azure Key Vaultの作成:**

```bash
# Key Vault作成
az keyvault create \
  --resource-group skishop-prod-rg \
  --name skishop-keyvault \
  --location japaneast

# シークレット保存
az keyvault secret set \
  --vault-name skishop-keyvault \
  --name servicebus-connection-string \
  --value "Endpoint=sb://..."

az keyvault secret set \
  --vault-name skishop-keyvault \
  --name eventgrid-topic-key \
  --value "..."
```

#### 4. モニタリングの設定

**Application Insightsの作成:**

```bash
# Application Insights作成
az monitor app-insights component create \
  --app skishop-insights \
  --location japaneast \
  --resource-group skishop-prod-rg \
  --application-type web

# Instrumentation Key取得
az monitor app-insights component show \
  --app skishop-insights \
  --resource-group skishop-prod-rg \
  --query instrumentationKey --output tsv
```

## 設定プロパティ一覧

### プロパティ命名規則

すべての設定プロパティは以下の命名規則に従います：

- **プレフィックス**: `skishop.runtime.`
- **形式**: kebab-case（ハイフン区切り）
- **環境変数**: SCREAMING_SNAKE_CASE（アンダースコア区切り、大文字）

### ローカル環境設定（application-local.yml）

```yaml
# ===== SKI SHOP ランタイム設定 =====
skishop:
  runtime:
    # ===== イベント伝播機能の制御 =====
    event-propagation-enabled: true           # イベント機能の有効/無効
    user-registration-event-enabled: true     # ユーザー登録イベント発行
    user-deletion-event-enabled: true         # ユーザー削除イベント発行
    fallback-to-sync-processing: true         # 同期処理へのフォールバック
    debug-mode: true                          # デバッグログ出力
    environment: "local"                      # 実行環境識別
    rollout-percentage: 100                   # 機能適用割合
    
    # ===== メッセージング設定 =====
    messaging:
      provider: "redis-streams"               # ローカル環境はRedis Streams
      retry-enabled: true                     # リトライ機能有効
      dead-letter-queue-enabled: true         # デッドレターキュー有効
      batch-processing-enabled: true          # バッチ処理有効
      
    # ===== パフォーマンス設定 =====
    performance:
      async-processing-enabled: true          # 非同期処理有効
      thread-pool-size: 10                    # スレッドプール サイズ
      queue-capacity: 1000                    # キューキャパシティ
      
    # ===== セキュリティ設定 =====
    security:
      encryption-enabled: false               # ローカルでは暗号化無効
      audit-logging-enabled: true             # 監査ログ有効
      
    # ===== 監視設定 =====
    monitoring:
      metrics-enabled: true                   # メトリクス収集有効
      tracing-enabled: true                   # 分散トレーシング有効
      health-check-enabled: true              # ヘルスチェック有効

# ===== Spring Framework設定 =====
spring:
  profiles:
    active: local
  
  # データソース設定
  datasource:
    url: jdbc:postgresql://localhost:5432/skishop
    username: skishop
    password: skishop-password
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 10                   # 最大プールサイズ
      minimum-idle: 2                         # 最小アイドル接続数
      connection-timeout: 30000               # 接続タイムアウト（30秒）
      idle-timeout: 600000                    # アイドルタイムアウト（10分）
      max-lifetime: 1800000                   # 最大生存時間（30分）
      leak-detection-threshold: 60000         # リーク検出しきい値（60秒）
  
  # JPA設定
  jpa:
    hibernate:
      ddl-auto: validate                      # スキーマ検証のみ
    show-sql: true                            # SQL出力（デバッグ用）
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
        use_sql_comments: true
  
  # Redis設定
  data:
    redis:
      host: localhost
      port: 6379
      password: skishop-redis-password
      timeout: 3000ms
      database: 0
      lettuce:
        pool:
          max-active: 8                       # 最大アクティブ接続数
          max-idle: 4                         # 最大アイドル接続数
          min-idle: 1                         # 最小アイドル接続数
          max-wait: 5000ms                    # 接続取得待機時間
          time-between-eviction-runs: 30s     # 不要接続の削除間隔
        timeout: 2000ms                       # コマンドタイムアウト
        shutdown-timeout: 1000ms              # シャットダウンタイムアウト
  
  # キャッシュ設定
  cache:
    type: redis
    redis:
      time-to-live: 300000                    # 5分

# ===== Redis Streams設定 =====
redis:
  streams:
    auth-events:
      stream-name: "skishop-auth-user-events" # 統一されたストリーム名
      consumer-group: "user-management-service" # コンシューマーグループ
      consumer-name: "${spring.application.name}-${random.uuid}" # ユニークなコンシューマー名
      batch-size: 10                          # バッチサイズ
      poll-timeout: 1000ms                    # ポーリングタイムアウト
      block-timeout: 5000ms                   # ブロックタイムアウト
      retry-count: 3                          # リトライ回数
      error-handler: "dead-letter-queue"      # エラーハンドラー
      auto-ack: false                         # 手動ACK
      
    # ===== Dead Letter Queue設定 =====
    dead-letter:
      stream-name: "skishop-auth-user-events-dlq" # DLQストリーム名
      max-len: 10000                          # DLQ最大長
      retention-period: 7d                    # 保持期間
      
    # ===== ストリーム管理設定 =====
    management:
      auto-create: true                       # ストリーム自動作成
      max-len: 100000                         # ストリーム最大長
      maxlen-policy: "approximate"            # 長さ制限ポリシー

# ===== Saga設定 =====
saga:
  # ===== 基本設定 =====
  enabled: true                               # Saga機能有効
  persistence-enabled: true                   # Saga状態永続化
  
  # ===== リトライ設定 =====
  retry:
    max-attempts: 3                           # 最大リトライ回数
    initial-delay: 1000                       # 初期遅延（ミリ秒）
    multiplier: 2.0                           # 遅延倍率
    max-delay: 10000                          # 最大遅延（ミリ秒）
    jitter: true                              # ジッター有効
    
  # ===== タイムアウト設定 =====
  timeout: 30000                              # Sagaタイムアウト（30秒）
  step-timeout: 10000                         # ステップタイムアウト（10秒）
  
  # ===== 管理設定 =====
  cleanup-interval: 300000                    # クリーンアップ間隔（5分）
  max-concurrent-sagas: 100                   # 最大同時実行Saga数
  persistence-batch-size: 50                  # 永続化バッチサイズ
  
  # ===== 補償処理設定 =====
  compensation:
    enabled: true                             # 補償処理有効
    timeout: 20000                            # 補償処理タイムアウト（20秒）
    max-attempts: 5                           # 補償処理最大試行回数

# ===== タイムアウト設定 =====
timeout:
  http:
    connect: 5s                               # HTTP接続タイムアウト
    read: 10s                                 # HTTP読み取りタイムアウト
    write: 10s                                # HTTP書き込みタイムアウト
  database:
    query: 5s                                 # クエリタイムアウト
    transaction: 30s                          # トランザクションタイムアウト
    connection-validation: 3s                 # 接続検証タイムアウト
  messaging:
    publish: 10s                              # メッセージ発行タイムアウト
    consume: 5s                               # メッセージ消費タイムアウト
    saga-complete: 30s                        # Saga完了タイムアウト
  circuit-breaker:
    call: 5s                                  # 回路呼び出しタイムアウト
    wait-duration-in-open-state: 60s          # オープン状態での待機時間

# ===== Resilience4j設定 =====
resilience4j:
  circuitbreaker:
    instances:
      eventPublisher:
        failure-rate-threshold: 50            # 失敗率しきい値（50%）
        slow-call-rate-threshold: 50          # 遅い呼び出し率しきい値
        slow-call-duration-threshold: 2s      # 遅い呼び出しの判定時間
        wait-duration-in-open-state: 30s      # オープン状態での待機時間
        sliding-window-size: 10               # スライディングウィンドウサイズ
        minimum-number-of-calls: 5            # 最小呼び出し回数
        permitted-number-of-calls-in-half-open-state: 3 # ハーフオープン状態での許可回数
  
  retry:
    instances:
      eventPublisher:
        max-attempts: 3
        wait-duration: 1s
        exponential-backoff-multiplier: 2
        retry-exceptions:
          - java.net.ConnectException
          - java.net.SocketTimeoutException

# ===== アクチュエーター設定 =====
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,env
  endpoint:
    health:
      show-details: always
  metrics:
    export:
      prometheus:
        enabled: true
    distribution:
      percentiles-histogram:
        "[http.server.requests]": true
      sla:
        "[http.server.requests]": 100ms,500ms,1s,2s

# ===== ログ設定 =====
logging:
  level:
    com.skishop: DEBUG                        # アプリケーションログレベル
    org.springframework.data.redis: INFO     # Redis関連ログ
    org.springframework.transaction: DEBUG   # トランザクションログ
    org.springframework.web: INFO            # Webログ
    org.hibernate.SQL: DEBUG                 # SQLログ
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE # SQLパラメータ
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%X{correlationId}] %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%X{correlationId}] %logger{36} - %msg%n"
  file:
    name: logs/skishop-local.log

# ===== セキュリティ設定 =====
security:
  encryption:
    enabled: false                            # ローカルでは暗号化無効
    algorithm: "AES"
  
# ===== ビジネス設定 =====
business:
  user:
    max-registration-attempts: 3              # 最大登録試行回数
    registration-timeout: 300s                # 登録タイムアウト（5分）
    cleanup-failed-registrations: true       # 失敗した登録のクリーンアップ
```

### 本番環境設定（application-production.yml）

```yaml
# ===== SKI SHOP ランタイム設定 =====
skishop:
  runtime:
    # ===== イベント伝播機能の制御 =====
    event-propagation-enabled: true
    user-registration-event-enabled: true
    user-deletion-event-enabled: true
    fallback-to-sync-processing: false       # 本番環境では無効
    debug-mode: false                         # 本番環境では無効
    environment: "production"
    rollout-percentage: 100
    
    # ===== メッセージング設定 =====
    messaging:
      provider: "azure-servicebus"            # 本番環境はAzure Service Bus
      retry-enabled: true
      dead-letter-queue-enabled: true
      batch-processing-enabled: true
      priority-processing-enabled: true       # 優先度処理有効
      
    # ===== パフォーマンス設定 =====
    performance:
      async-processing-enabled: true
      thread-pool-size: 20                    # 本番環境は大きめ
      queue-capacity: 5000                    # 本番環境は大きめ
      bulk-operations-enabled: true           # バルク操作有効
      
    # ===== セキュリティ設定 =====
    security:
      encryption-enabled: true                # 本番環境では暗号化有効
      audit-logging-enabled: true
      data-masking-enabled: true              # データマスキング有効
      
    # ===== 監視設定 =====
    monitoring:
      metrics-enabled: true
      tracing-enabled: true
      health-check-enabled: true
      performance-profiling-enabled: true     # パフォーマンスプロファイリング有効
      alert-thresholds:
        error-rate: 5                         # エラー率閾値（%）
        response-time: 2000                   # 応答時間閾値（ms）
        queue-depth: 1000                     # キュー深度閾値

# ===== Spring Framework設定 =====
spring:
  profiles:
    active: production
  
  # データソース設定
  datasource:
    url: jdbc:postgresql://${POSTGRES_HOST}:5432/${POSTGRES_DB}
    username: ${POSTGRES_USERNAME}
    password: ${POSTGRES_PASSWORD}
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 20                   # 本番環境では大きめ
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      leak-detection-threshold: 60000         # リーク検出しきい値（60秒）
  
  # JPA設定
  jpa:
    hibernate:
      ddl-auto: validate                      # 本番環境では検証のみ
    show-sql: false                           # SQLログ無効
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        jdbc:
          batch_size: 20                      # バッチサイズ最適化
        order_inserts: true
        order_updates: true
        format_sql: false

# ===== Azure設定 =====
azure:
  # ===== Service Bus設定 =====
  servicebus:
    connection-string: ${AZURE_SERVICEBUS_CONNECTION_STRING}
    namespace: ${AZURE_SERVICEBUS_NAMESPACE:skishop-servicebus}
    
    # ===== メッセージエンティティ設定 =====
    entities:
      user-events:
        queue-name: "skishop-user-events"
        topic-name: "skishop-user-events-topic"
        subscription-name: "user-management-subscription"
        session-enabled: false
        partitioning-enabled: true            # パーティション有効
        
    # ===== 受信設定 =====
    receiver:
      auto-complete: true                     # 自動完了
      prefetch-count: 10                      # プリフェッチ数
      max-concurrent-calls: 5                 # 最大同時呼び出し数
      max-auto-renew-duration: 300s           # 最大自動更新期間
      receive-mode: "peek-lock"               # 受信モード
      
    # ===== 送信設定 =====
    sender:
      via-partition-key: true                 # パーティションキー使用
      retry-options:
        max-retries: 3
        delay: 1s
        max-delay: 30s
        try-timeout: 10s
        
    # ===== デッドレター設定 =====
    dead-letter:
      enabled: true
      max-delivery-count: 5                   # 最大配信回数
      ttl: 7d                                 # Time To Live
  # ===== Event Grid設定 =====
  eventgrid:
    topic-endpoint: ${AZURE_EVENTGRID_TOPIC_ENDPOINT}
    topic-key: ${AZURE_EVENTGRID_TOPIC_KEY}
    
    # ===== 発行設定 =====
    publisher:
      batch-size: 100                         # バッチサイズ
      max-wait-time: 5s                       # 最大待機時間
      retry-policy:
        max-attempts: 3
        initial-delay: 2s
        max-delay: 30s
        exponential-backoff: true
        
    # ===== イベント設定 =====
    events:
      user-registration:
        event-type: "skishop.user.registered"
        subject-prefix: "/users/registration/"
        schema-version: "1.0"
        
      user-deletion:
        event-type: "skishop.user.deleted"
        subject-prefix: "/users/deletion/"
        schema-version: "1.0"
  
  # ===== Key Vault設定 =====
  keyvault:
    uri: ${AZURE_KEYVAULT_URI}
    enabled: true
    secret-refresh-interval: 300s             # シークレット更新間隔
    retry-policy:
      max-attempts: 3
      initial-delay: 1s
      max-delay: 10s
  
  # ===== Application Insights設定 =====
  application-insights:
    instrumentation-key: ${AZURE_APPINSIGHTS_INSTRUMENTATION_KEY}
    enabled: true
    
    # ===== テレメトリ設定 =====
    telemetry:
      sampling-percentage: 100                # サンプリング率
      flush-interval: 5s                      # フラッシュ間隔
      max-batch-size: 512                     # 最大バッチサイズ
      
    # ===== カスタムメトリクス設定 =====
    custom-metrics:
      saga-duration: true                     # Saga実行時間
      event-processing-time: true             # イベント処理時間
      error-rate: true                        # エラー率
      throughput: true                        # スループット

# ===== Saga設定（本番環境用） =====
saga:
  # ===== 基本設定 =====
  enabled: true
  persistence-enabled: true
  
  # ===== リトライ設定 =====
  retry:
    max-attempts: 5                           # 本番環境では多めに
    initial-delay: 2000
    multiplier: 1.5
    max-delay: 30000
    jitter: true                              # ジッター有効でスパイク軽減
    
  # ===== タイムアウト設定 =====
  timeout: 60000                              # 60秒
  step-timeout: 20000                         # ステップタイムアウト（20秒）
  
  # ===== 管理設定 =====
  cleanup-interval: 600000                    # 10分
  max-concurrent-sagas: 500                   # 本番環境では大きめ
  persistence-batch-size: 100                 # バッチサイズ最適化
  
  # ===== 補償処理設定 =====
  compensation:
    enabled: true
    timeout: 40000                            # 補償処理タイムアウト（40秒）
    max-attempts: 7                           # 補償処理最大試行回数
    
  # ===== パフォーマンス設定 =====
  performance:
    parallel-execution-enabled: true          # 並列実行有効
    lazy-loading-enabled: true                # 遅延ロード有効
    caching-enabled: true                     # キャッシュ有効
  
# ===== タイムアウト設定（本番環境用） =====
timeout:
  http:
    connect: 10s                              # 本番環境では長め
    read: 30s
    write: 30s
  database:
    query: 10s
    transaction: 60s
    connection-validation: 5s
  messaging:
    publish: 30s
    consume: 10s
    saga-complete: 60s
  circuit-breaker:
    call: 10s
    wait-duration-in-open-state: 120s

# ===== Resilience4j設定（本番環境用） =====
resilience4j:
  circuitbreaker:
    instances:
      azureServiceBus:
        failure-rate-threshold: 30            # より厳しい設定
        slow-call-rate-threshold: 40
        slow-call-duration-threshold: 5s
        wait-duration-in-open-state: 60s
        sliding-window-size: 20
        minimum-number-of-calls: 10
        permitted-number-of-calls-in-half-open-state: 5
      
      azureEventGrid:
        failure-rate-threshold: 30
        slow-call-rate-threshold: 40
        slow-call-duration-threshold: 5s
        wait-duration-in-open-state: 60s
        sliding-window-size: 20
        minimum-number-of-calls: 10
  
  retry:
    instances:
      azureServices:
        max-attempts: 5
        wait-duration: 2s
        exponential-backoff-multiplier: 1.5
        retry-exceptions:
          - com.azure.core.exception.AzureException
          - java.net.ConnectException

# ===== アクチュエーター設定（本番環境用） =====
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus  # 本番では限定
  endpoint:
    health:
      show-details: when-authorized            # 認証時のみ詳細表示
  metrics:
    export:
      prometheus:
        enabled: true
        step: 10s                              # メトリクス収集間隔
      azure-monitor:
        enabled: true                          # Azure Monitor連携
        instrumentation-key: ${AZURE_APPINSIGHTS_INSTRUMENTATION_KEY}

# ===== ログ設定（本番環境用） =====
logging:
  level:
    com.skishop: INFO                          # 本番環境では INFO レベル
    org.springframework: WARN
    org.hibernate: WARN
    com.azure: WARN
    root: WARN
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level [%X{correlationId}] %logger{36} - %msg%n"
  appender:
    azure:
      enabled: true                            # Azure Log Analytics連携
      workspace-id: ${AZURE_LOG_ANALYTICS_WORKSPACE_ID}
      shared-key: ${AZURE_LOG_ANALYTICS_SHARED_KEY}

# ===== セキュリティ設定（本番環境用） =====
security:
  encryption:
    enabled: true                              # 本番環境では暗号化有効
    algorithm: "AES/GCM/NoPadding"
    key-source: "azure-keyvault"
    password: ${ENCRYPTION_PASSWORD}
  
  oauth2:
    resource-server:
      jwt:
        issuer-uri: ${JWT_ISSUER_URI}
        jwk-set-uri: ${JWT_JWK_SET_URI}

# ===== ビジネス設定（本番環境用） =====
business:
  user:
    max-registration-attempts: 5              # 本番環境では多め
    registration-timeout: 600s                # 10分
    cleanup-failed-registrations: true
    rate-limiting:
      enabled: true
      max-requests-per-minute: 100            # レート制限
```

### 環境変数一覧

#### ローカル環境

```bash
# データベース接続
export POSTGRES_HOST=localhost
export POSTGRES_DB=skishop
export POSTGRES_USERNAME=skishop
export POSTGRES_PASSWORD=skishop-password

# Redis接続
export REDIS_HOST=localhost
export REDIS_PORT=6379
export REDIS_PASSWORD=skishop-redis-password

# アプリケーション設定
export SPRING_PROFILES_ACTIVE=local
export SKISHOP_RUNTIME_ENVIRONMENT=local
```

#### 本番環境

```bash
# Azure Service Bus
export AZURE_SERVICEBUS_CONNECTION_STRING="Endpoint=sb://..."
export AZURE_SERVICEBUS_NAMESPACE="skishop-servicebus"

# Azure Event Grid
export AZURE_EVENTGRID_TOPIC_ENDPOINT="https://..."
export AZURE_EVENTGRID_TOPIC_KEY="..."

# Azure Key Vault
export AZURE_KEYVAULT_URI="https://skishop-keyvault.vault.azure.net/"

# Azure Application Insights
export AZURE_APPINSIGHTS_INSTRUMENTATION_KEY="..."

# Azure Log Analytics
export AZURE_LOG_ANALYTICS_WORKSPACE_ID="..."
export AZURE_LOG_ANALYTICS_SHARED_KEY="..."

# データベース接続
export POSTGRES_HOST="skishop-postgres.postgres.database.azure.com"
export POSTGRES_DB="skishop"
export POSTGRES_USERNAME="skishop"
export POSTGRES_PASSWORD="SecurePassword123!"

# セキュリティ
export ENCRYPTION_PASSWORD="..."
export JWT_ISSUER_URI="https://..."
export JWT_JWK_SET_URI="https://..."

# アプリケーション設定
export SPRING_PROFILES_ACTIVE=production
export SKISHOP_RUNTIME_ENVIRONMENT=production
```
