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

# ユーザー登録テスト
curl -X POST http://localhost:8080/api/auth/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "password123",
    "firstName": "Test",
    "lastName": "User"
  }'
```

### 本番環境の設定

#### Azure リソースの作成

1. **Azure Service Bus の作成**
```bash
# Resource Group作成
az group create --name skishop-prod --location japaneast

# Service Bus Namespace作成
az servicebus namespace create \
  --resource-group skishop-prod \
  --name skishop-servicebus \
  --location japaneast \
  --sku Standard

# Topics作成
az servicebus topic create \
  --resource-group skishop-prod \
  --namespace-name skishop-servicebus \
  --name user-events

# Subscriptions作成
az servicebus topic subscription create \
  --resource-group skishop-prod \
  --namespace-name skishop-servicebus \
  --topic-name user-events \
  --name user-management-subscription
```

2. **Azure Redis Cache の作成**
```bash
az redis create \
  --resource-group skishop-prod \
  --name skishop-redis \
  --location japaneast \
  --sku Standard \
  --vm-size c1
```

3. **Azure Database for PostgreSQL の作成**
```bash
az postgres flexible-server create \
  --resource-group skishop-prod \
  --name skishop-postgres \
  --location japaneast \
  --admin-user skishop \
  --admin-password "SecurePassword123!" \
  --sku-name Standard_B2s \
  --tier Burstable \
  --version 15
```

#### 環境変数の設定（本番環境）

```bash
# .env.production ファイルを作成
cat > .env.production << 'EOF'
# Production Environment
SPRING_PROFILES_ACTIVE=production
SKISHOP_EVENT_PROPAGATION_ENABLED=true
SKISHOP_EVENT_BROKER_TYPE=azure-servicebus
SKISHOP_EVENT_REDIS_KEY_PREFIX=skishop-prod
SKISHOP_DEBUG_MODE=false
SKISHOP_ENVIRONMENT=production

# Azure Service Bus
AZURE_SERVICEBUS_CONNECTION_STRING=Endpoint=sb://skishop-servicebus.servicebus.windows.net/;SharedAccessKeyName=RootManageSharedAccessKey;SharedAccessKey=...

# Database
DATABASE_URL=jdbc:postgresql://skishop-postgres.postgres.database.azure.com:5432/postgres
DATABASE_USERNAME=skishop
DATABASE_PASSWORD=SecurePassword123!

# Redis
REDIS_HOST=skishop-redis.redis.cache.windows.net
REDIS_PORT=6380
REDIS_PASSWORD=...

# Security
JWT_SECRET=production-secret-key-512-bits-long
AZURE_CLIENT_ID=...
AZURE_CLIENT_SECRET=...
AZURE_TENANT_ID=...

# Monitoring
AZURE_APP_INSIGHTS_KEY=...
EOF
```

## 完全な設定プロパティ一覧

### 共通プロパティ（両サービス）

#### Skishop Runtime Properties
```yaml
skishop:
  runtime:
    # Event propagation configuration
    event-propagation-enabled: ${SKISHOP_EVENT_PROPAGATION_ENABLED:false}
    event-broker-type: ${SKISHOP_EVENT_BROKER_TYPE:redis}  # redis, azure-servicebus, kafka
    event-max-retries: ${SKISHOP_EVENT_MAX_RETRIES:3}
    event-timeout-ms: ${SKISHOP_EVENT_TIMEOUT_MS:30000}
    event-redis-key-prefix: ${SKISHOP_EVENT_REDIS_KEY_PREFIX:skishop}
    event-concurrency: ${SKISHOP_EVENT_CONCURRENCY:4}
    event-persistence-enabled: ${SKISHOP_EVENT_PERSISTENCE_ENABLED:true}
    processed-event-retention-days: ${SKISHOP_PROCESSED_EVENT_RETENTION_DAYS:30}
    debug-mode: ${SKISHOP_DEBUG_MODE:false}
    environment: ${SKISHOP_ENVIRONMENT:local}  # local, development, production
```

#### Database Properties
```yaml
spring:
  datasource:
    url: ${DATABASE_URL:jdbc:postgresql://localhost:5432/skishop_auth}
    username: ${DATABASE_USERNAME:skishop_user}
    password: ${DATABASE_PASSWORD:password}
    hikari:
      maximum-pool-size: ${DB_POOL_MAX_SIZE:20}
      minimum-idle: ${DB_POOL_MIN_IDLE:5}
      connection-timeout: ${DB_CONNECTION_TIMEOUT:30000}
      idle-timeout: ${DB_IDLE_TIMEOUT:600000}
      max-lifetime: ${DB_MAX_LIFETIME:1800000}
```

#### Redis Properties
```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      timeout: ${REDIS_TIMEOUT:2000ms}
      ssl:
        enabled: ${REDIS_SSL_ENABLED:false}
      lettuce:
        pool:
          max-active: ${REDIS_POOL_MAX_ACTIVE:8}
          max-wait: ${REDIS_POOL_MAX_WAIT:-1ms}
          max-idle: ${REDIS_POOL_MAX_IDLE:8}
          min-idle: ${REDIS_POOL_MIN_IDLE:0}
```

#### Resilience4j Properties
```yaml
resilience4j:
  retry:
    instances:
      event-publishing:
        max-attempts: ${RESILIENCE4J_RETRY_MAX_ATTEMPTS:3}
        wait-duration: ${RESILIENCE4J_RETRY_WAIT_DURATION:1000ms}
        exponential-backoff-multiplier: ${RESILIENCE4J_RETRY_BACKOFF_MULTIPLIER:2}
        retry-exceptions:
          - java.lang.RuntimeException
          - org.springframework.dao.DataAccessException
      event-processing:
        max-attempts: ${RESILIENCE4J_RETRY_MAX_ATTEMPTS:3}
        wait-duration: ${RESILIENCE4J_RETRY_WAIT_DURATION:1000ms}
        exponential-backoff-multiplier: ${RESILIENCE4J_RETRY_BACKOFF_MULTIPLIER:2}
        retry-exceptions:
          - java.lang.RuntimeException
          - org.springframework.dao.DataAccessException
```

### Authentication Service 固有プロパティ

```yaml
# JWT Configuration
jwt:
  secret: ${JWT_SECRET:test-secret-key-for-jwt-that-is-long-enough}
  issuer: ${JWT_ISSUER:SkiShop-Auth}
  access-token-expiration: ${JWT_ACCESS_EXPIRATION:3600}
  refresh-token-expiration: ${JWT_REFRESH_EXPIRATION:604800}

# Azure Active Directory Configuration
spring:
  cloud:
    azure:
      active-directory:
        enabled: ${AZURE_AD_ENABLED:false}
        profile:
          tenant-id: ${AZURE_TENANT_ID:}
        credential:
          client-id: ${AZURE_CLIENT_ID:}
          client-secret: ${AZURE_CLIENT_SECRET:}
        app-id-uri: ${AZURE_APP_ID_URI:}

# OAuth2 Configuration
  security:
    oauth2:
      client:
        registration:
          azure:
            client-id: ${AZURE_CLIENT_ID:}
            client-secret: ${AZURE_CLIENT_SECRET:}
            scope: openid,profile,email
            redirect-uri: ${OAUTH2_REDIRECT_URI:http://localhost:8080/login/oauth2/code/azure}
```

### User Management Service 固有プロパティ

```yaml
# User-specific configuration
skishop:
  user:
    password:
      min-length: ${USER_PASSWORD_MIN_LENGTH:8}
      max-length: ${USER_PASSWORD_MAX_LENGTH:100}
      require-uppercase: ${USER_PASSWORD_REQUIRE_UPPERCASE:true}
      require-lowercase: ${USER_PASSWORD_REQUIRE_LOWERCASE:true}
      require-digits: ${USER_PASSWORD_REQUIRE_DIGITS:true}
      require-special-chars: ${USER_PASSWORD_REQUIRE_SPECIAL_CHARS:true}
    
    email-verification:
      token-validity: ${EMAIL_VERIFICATION_TOKEN_VALIDITY:24}  # hours
      base-url: ${FRONTEND_BASE_URL:http://localhost:3000}
      
    account-lock:
      max-failed-attempts: ${ACCOUNT_LOCK_MAX_FAILED_ATTEMPTS:5}
      lock-duration: ${ACCOUNT_LOCK_DURATION:30}  # minutes
      
    notification:
      email:
        enabled: ${EMAIL_NOTIFICATION_ENABLED:true}
        from: ${EMAIL_FROM:noreply@skishop.com}
        smtp:
          host: ${SMTP_HOST:localhost}
          port: ${SMTP_PORT:587}
          username: ${SMTP_USERNAME:}
          password: ${SMTP_PASSWORD:}
          auth: ${SMTP_AUTH:true}
          starttls: ${SMTP_STARTTLS:true}
```

### Azure Service Bus Properties (本番環境)

```yaml
# Azure Service Bus Configuration
azure:
  servicebus:
    connection-string: ${AZURE_SERVICEBUS_CONNECTION_STRING:}
    pricing-tier: ${AZURE_SERVICEBUS_PRICING_TIER:standard}
  
  # Application Insights
  application-insights:
    enabled: ${AZURE_APP_INSIGHTS_ENABLED:false}
    instrumentation-key: ${AZURE_APP_INSIGHTS_KEY:}
    
  # Key Vault
  keyvault:
    enabled: ${AZURE_KEYVAULT_ENABLED:false}
    uri: ${AZURE_KEYVAULT_URI:}
```

### 環境別設定ファイル

#### ローカル環境 (application-local.yml)
```yaml
# Local Development Overrides
skishop:
  runtime:
    event-propagation-enabled: true
    event-broker-type: redis
    event-redis-key-prefix: skishop-local
    debug-mode: true
    environment: local
    
logging:
  level:
    '[com.skishop]': DEBUG
    '[org.springframework.data.redis]': DEBUG
    '[io.github.resilience4j]': DEBUG
```

#### 本番環境 (application-production.yml)
```yaml
# Production Overrides
skishop:
  runtime:
    event-propagation-enabled: true
    event-broker-type: azure-servicebus
    event-redis-key-prefix: skishop-prod
    debug-mode: false
    environment: production
    event-max-retries: 5
    event-timeout-ms: 60000
    event-concurrency: 8
    processed-event-retention-days: 90

logging:
  level:
    '[com.skishop]': INFO
    '[io.github.resilience4j]': WARN
    root: WARN

management:
  endpoint:
    health:
      show-details: never
```

## デプロイメントチェックリスト

### ローカル環境

- [ ] Docker & Docker Compose インストール済み
- [ ] PostgreSQL, Redis コンテナが起動中
- [ ] データベースとユーザーが作成済み
- [ ] 環境変数 (.env.local) が設定済み
- [ ] 両サービスが正常に起動
- [ ] Health checkエンドポイントが応答
- [ ] イベント発行/受信のテストが成功

### 本番環境

- [ ] Azure リソースグループ作成済み
- [ ] Azure Service Bus 設定済み
- [ ] Azure Redis Cache 設定済み
- [ ] Azure Database for PostgreSQL 設定済み
- [ ] Azure Key Vault 設定済み（オプション）
- [ ] Azure Application Insights 設定済み（オプション）
- [ ] 本番環境変数 (.env.production) 設定済み
- [ ] SSL/TLS 証明書設定済み
- [ ] ネットワークセキュリティグループ設定済み
- [ ] ロードバランサー設定済み
- [ ] バックアップとディザスタリカバリ設定済み
- [ ] 監視とアラート設定済み

## 実装完了状況

### ✅ 完了済み

1. **Event Publishing Service**: 認証サービスでのイベント発行ロジック実装
2. **Event Consumer Service**: ユーザー管理サービスでのイベント受信ロジック実装
3. **User Registration Service**: ユーザー登録エンドポイントとイベント連携
4. **Configuration Classes**: 統一された設定プロパティ管理
5. **Entity Classes**: Saga状態と処理済みイベントのJPAエンティティ
6. **Repository Classes**: データアクセス層の実装
7. **Redis Configuration**: イベント発行・受信のためのRedis設定
8. **Environment Configuration**: ローカル・本番環境設定ファイル
9. **依存関係**: pom.xmlへの必要なライブラリ追加
10. **ドキュメント**: 環境構築手順と設定プロパティ一覧

### 🔄 次のステップ（必要に応じて）

1. **統合テスト**: エンドツーエンドの動作確認
2. **Docker Compose**: ローカル開発環境のコンテナ化
3. **Azure Service Bus**: 本番環境向けの実装
4. **監視・メトリクス**: Prometheus/Grafanaとの統合
5. **エラーハンドリング**: 詳細なエラー処理とアラート
6. **性能最適化**: スループットとレイテンシの改善
