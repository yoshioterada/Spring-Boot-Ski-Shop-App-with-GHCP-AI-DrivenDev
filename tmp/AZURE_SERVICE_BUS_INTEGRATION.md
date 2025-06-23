# Azure Service Bus統合 - デプロイメントガイド

## 概要

認証サービスとユーザー管理サービス間のイベント伝播において、本番環境でAzure Service Busを使用するための統合が完成しました。このドキュメントでは、デプロイメントと設定について説明します。

## 実装された機能

### 4. 詳細ステータス管理 - 運用監視・デバッグ向上 ✅
- SagaStatusMonitoringService: 包括的なサガ状態監視
- SagaStatusHistory: 状態遷移の履歴追跡
- SagaOverview、SagaPerformanceStats: 運用統計とパフォーマンス分析
- 詳細なステータス列挙（UserRegistrationStatus、UserDeletionStatus、SagaStatus）

### 5. 基本的な監視・ヘルスチェック - 運用安定性向上 ✅
- EventSystemHealthIndicator: システム全体の健全性チェック
- EventSystemMetricsService: Micrometer基盤のメトリクス収集
- Azure Service Bus健全性チェック統合
- Prometheus メトリクス出力対応

### 6. Azure Service Bus統合 - 本番環境対応 ✅
- AzureServiceBusEventPublisher: セキュアなイベント発行
- AzureServiceBusEventReceiver: 高可用性イベント受信
- AzureServiceBusStatusFeedbackPublisher/Receiver: 双方向ステータスフィードバック
- 管理されたIDによる認証（推奨）と接続文字列サポート
- デッドレターキュー対応
- 自動リトライとエラーハンドリング

## Azure Service Bus リソース要件

### 必要なリソース

1. **Service Bus Namespace**
   - Standard または Premium ティア推奨
   - 地域レプリケーション対応

2. **Topics と Subscriptions**
   ```
   skishop-events-prod (Topic)
   ├── auth-service-subscription
   └── user-service-subscription
   
   skishop-status-feedback-prod (Topic)
   ├── auth-service-subscription
   └── user-service-subscription
   ```

3. **管理されたID (推奨)**
   - Azure Service Bus データ所有者ロール
   - Azure Service Bus データ送信者ロール
   - Azure Service Bus データ受信者ロール

## 環境変数設定

### 認証サービス

```bash
# Azure Service Bus 設定
AZURE_SERVICEBUS_NAMESPACE=your-servicebus-namespace
AZURE_SERVICEBUS_CONNECTION_STRING=optional-connection-string

# Service Bus 有効化
SKISHOP_AZURE_SERVICEBUS_ENABLED=true
SKISHOP_ENVIRONMENT=production

# パフォーマンス調整
AZURE_SERVICEBUS_MAX_CONCURRENT_CALLS=10
AZURE_SERVICEBUS_MAX_RETRIES=5
```

### ユーザー管理サービス

```bash
# Azure Service Bus 設定（認証サービスと同じ）
AZURE_SERVICEBUS_NAMESPACE=your-servicebus-namespace
AZURE_SERVICEBUS_CONNECTION_STRING=optional-connection-string

# Service Bus 有効化
SKISHOP_AZURE_SERVICEBUS_ENABLED=true
SKISHOP_ENVIRONMENT=production
```

## デプロイメント手順

### 1. Azure リソースの準備

```bash
# Service Bus Namespace作成
az servicebus namespace create \
  --resource-group your-resource-group \
  --name your-servicebus-namespace \
  --location eastus \
  --sku Standard

# Topics作成
az servicebus topic create \
  --resource-group your-resource-group \
  --namespace-name your-servicebus-namespace \
  --name skishop-events-prod \
  --max-size 5120

az servicebus topic create \
  --resource-group your-resource-group \
  --namespace-name your-servicebus-namespace \
  --name skishop-status-feedback-prod \
  --max-size 5120

# Subscriptions作成
az servicebus topic subscription create \
  --resource-group your-resource-group \
  --namespace-name your-servicebus-namespace \
  --topic-name skishop-events-prod \
  --name auth-service-subscription

az servicebus topic subscription create \
  --resource-group your-resource-group \
  --namespace-name your-servicebus-namespace \
  --topic-name skishop-events-prod \
  --name user-service-subscription

az servicebus topic subscription create \
  --resource-group your-resource-group \
  --namespace-name your-servicebus-namespace \
  --topic-name skishop-status-feedback-prod \
  --name auth-service-subscription

az servicebus topic subscription create \
  --resource-group your-resource-group \
  --namespace-name your-servicebus-namespace \
  --topic-name skishop-status-feedback-prod \
  --name user-service-subscription
```

### 2. 管理されたIDの設定（推奨）

```bash
# アプリケーション用管理されたIDを作成（各サービス用）
az identity create \
  --resource-group your-resource-group \
  --name auth-service-identity

az identity create \
  --resource-group your-resource-group \
  --name user-service-identity

# Service Bus権限を付与
az role assignment create \
  --assignee $(az identity show --resource-group your-resource-group --name auth-service-identity --query principalId -o tsv) \
  --role "Azure Service Bus Data Owner" \
  --scope /subscriptions/your-subscription-id/resourceGroups/your-resource-group/providers/Microsoft.ServiceBus/namespaces/your-servicebus-namespace
```

### 3. アプリケーションデプロイメント

#### Docker Compose 例

```yaml
version: '3.8'
services:
  authentication-service:
    image: your-registry/authentication-service:latest
    environment:
      - SPRING_PROFILES_ACTIVE=production
      - AZURE_SERVICEBUS_NAMESPACE=your-servicebus-namespace
      - SKISHOP_ENVIRONMENT=production
      - SKISHOP_AZURE_SERVICEBUS_ENABLED=true
    ports:
      - "8080:8080"

  user-management-service:
    image: your-registry/user-management-service:latest
    environment:
      - SPRING_PROFILES_ACTIVE=production
      - AZURE_SERVICEBUS_NAMESPACE=your-servicebus-namespace
      - SKISHOP_ENVIRONMENT=production
      - SKISHOP_AZURE_SERVICEBUS_ENABLED=true
    ports:
      - "8081:8081"
```

## 監視・運用

### 健全性チェック

各サービスは以下のエンドポイントで健全性を確認できます：

- `GET /actuator/health` - システム全体の健全性
- `GET /actuator/health/eventSystem` - イベントシステム専用健全性
- `GET /actuator/metrics` - Micrometer メトリクス
- `GET /actuator/prometheus` - Prometheus メトリクス

### ログ監視

重要なログパターン：
- `Azure Service Bus health check failed` - Service Bus接続問題
- `Failed to publish event to Azure Service Bus` - イベント発行失敗
- `Maximum retry attempts exceeded` - リトライ上限到達

### メトリクス

重要なメトリクス：
- `saga.status.count` - サガ状態別カウント
- `azure.servicebus.publish.success` - 発行成功率
- `azure.servicebus.receive.success` - 受信成功率
- `event.processing.time` - イベント処理時間

## トラブルシューティング

### よくある問題

1. **Azure Service Bus 接続エラー**
   - 管理されたIDの権限確認
   - ネットワーク接続確認
   - Service Bus Namespace存在確認

2. **メッセージがデッドレターキューに送信される**
   - メッセージ形式確認
   - 処理ロジックのエラー確認
   - タイムアウト設定確認

3. **パフォーマンス問題**
   - 同時実行数調整（max-concurrent-calls）
   - リトライ設定調整
   - リソース配分確認

## 次のステップ

- Azure Monitor統合
- Application Insights連携
- 自動スケーリング設定
- 災害復旧計画
- セキュリティ監査

## 関連ドキュメント

- [Azure Service Bus ドキュメント](https://docs.microsoft.com/en-us/azure/service-bus-messaging/)
- [Spring Cloud Azure Service Bus](https://docs.microsoft.com/en-us/azure/developer/java/spring-framework/configure-spring-boot-starter-java-app-with-azure-service-bus)
- [イベント駆動アーキテクチャ ベストプラクティス](https://docs.microsoft.com/en-us/azure/architecture/guide/architecture-styles/event-driven)
