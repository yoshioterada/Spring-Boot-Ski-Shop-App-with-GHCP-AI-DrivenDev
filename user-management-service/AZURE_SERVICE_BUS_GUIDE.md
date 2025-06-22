# Azure Service Bus 統合ガイド

## 概要

このドキュメントでは、ユーザー管理サービスでのAzure Service Bus統合のセットアップと運用に関する手順を説明します。Azure Service Busは、認証サービスとユーザー管理サービス間の堅牢で信頼性の高いイベント伝播を実現します。

## 前提条件

- Azure アカウントとサブスクリプション
- Azure Service Bus名前空間（Standard以上のティア）
- Azure CLIまたはAzureポータルへのアクセス

## 1. Azure Service Busリソースの作成

以下のコマンドを使用して、必要なAzure Service Busリソースを作成します。

```bash
# リソースグループを作成（既存のものを使用する場合はスキップ）
az group create --name your-resource-group --location eastus

# Service Bus名前空間を作成
az servicebus namespace create \
  --resource-group your-resource-group \
  --name your-servicebus-namespace \
  --location eastus \
  --sku Standard

# トピックの作成
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

# サブスクリプションの作成
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
```

## 2. 管理IDの設定（推奨）

アプリケーションの管理IDを作成し、Service Busへのアクセス権を付与します。

```bash
# アプリケーション用の管理IDを作成
az identity create \
  --resource-group your-resource-group \
  --name user-service-identity

# ServiceBusに対する権限を付与
PRINCIPAL_ID=$(az identity show --resource-group your-resource-group --name user-service-identity --query principalId -o tsv)
NAMESPACE_ID=$(az servicebus namespace show --resource-group your-resource-group --name your-servicebus-namespace --query id -o tsv)

az role assignment create \
  --assignee $PRINCIPAL_ID \
  --role "Azure Service Bus Data Owner" \
  --scope $NAMESPACE_ID
```

## 3. 環境変数の設定

アプリケーションの起動時に以下の環境変数を設定します。

```bash
# 必須設定
export AZURE_SERVICEBUS_NAMESPACE=your-servicebus-namespace
export SKISHOP_AZURE_SERVICEBUS_ENABLED=true
export SPRING_PROFILES_ACTIVE=production

# オプション設定（必要に応じて変更）
export SKISHOP_AZURE_SERVICEBUS_TOPIC_NAME=skishop-events-prod
export SKISHOP_AZURE_SERVICEBUS_SUBSCRIPTION_NAME=user-service-subscription
export SKISHOP_AZURE_SERVICEBUS_STATUS_TOPIC=skishop-status-feedback-prod
export AZURE_SERVICEBUS_MAX_CONCURRENT_CALLS=4
export AZURE_SERVICEBUS_MAX_RETRIES=3
export AZURE_SERVICEBUS_PREFETCH_COUNT=10

# 管理IDではなく接続文字列を使用する場合（非推奨）
# export AZURE_SERVICEBUS_CONNECTION_STRING=your-connection-string
```

## 4. アプリケーション設定

`application-production.yml`ファイルを設定するか、環境変数を使用して設定します。サンプル設定は`application-production-sample.yml`を参照してください。

## 5. デプロイとテスト

1. アプリケーションをデプロイします
2. 以下のエンドポイントでイベントシステムの健全性を確認します：
   - `GET /actuator/health/eventSystem`

## 6. モニタリング

- メトリクスは`/actuator/prometheus`エンドポイントで確認できます
- 重要なメトリクス：
  - `events.success.total`
  - `events.failure.total`
  - `messagebroker.error.total`

## 7. トラブルシューティング

### よくある問題

1. **接続エラー**:
   - 管理IDの権限が正しく設定されているか確認
   - ネットワーク接続を確認
   - 環境変数が正しく設定されているか確認

2. **メッセージがデッドレターキューに送信される**:
   - ログでエラーメッセージを確認
   - イベント処理ロジックのエラーを修正

3. **パフォーマンス問題**:
   - `max-concurrent-calls`を調整
   - `prefetch-count`を調整

## 参考リンク

- [Azure Service Bus公式ドキュメント](https://docs.microsoft.com/azure/service-bus-messaging/)
- [Spring Cloud Azure Service Bus](https://docs.microsoft.com/azure/developer/java/spring-framework/configure-spring-boot-starter-java-app-with-azure-service-bus)
