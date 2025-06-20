# Spring Boot AI Support Service - LangChain4j 1.1.0 Implementation

## 🎯 Project Overview

スキーショップ向けAIサポートサービス - LangChain4j 1.1.0 + Azure OpenAI による高度なAI機能を実装。

## ✅ 完成済み機能

### 1. 依存関係とビルド設定
- **LangChain4j 1.1.0** (最新利用可能版: 1.1.0-beta7)を使用
- **Azure OpenAI統合**: `langchain4j-azure-open-ai:1.1.0-rc1`
- **Spring Boot統合**: `langchain4j-spring-boot-starter:1.1.0-beta7`
- **埋め込みモデル**: `langchain4j-embeddings-all-minilm-l6-v2:1.1.0-beta7`
- Maven依存関係の解決完了、ビルド成功確認済み

### 2. Azure OpenAI 設定 (application.yml)
```yaml
azure:
  openai:
    api-key: ${AZURE_OPENAI_API_KEY}
    endpoint: ${AZURE_OPENAI_ENDPOINT}
    chat-deployment-name: ${AZURE_OPENAI_CHAT_DEPLOYMENT_NAME:gpt-4o}
    embedding-deployment-name: ${AZURE_OPENAI_EMBEDDING_DEPLOYMENT_NAME:text-embedding-3-small}
    service-version: ${AZURE_OPENAI_SERVICE_VERSION:2024-02-01}
    temperature: 0.7
    max-tokens: 2000
    timeout: ${AZURE_OPENAI_TIMEOUT:60s}
    max-retries: 3
```

### 3. LangChain4j 設定クラス (LangChain4jConfig.java)
- **ChatModel Bean**: Azure OpenAI Chat Model設定
- **EmbeddingModel Bean**: Azure OpenAI Embedding Model設定  
- **ChatMemory Bean**: メッセージ履歴管理
- **AIサービス Bean**: 3つのAIアシスタント自動生成

### 4. AIサービスインターフェース

#### CustomerSupportAssistant
- 一般的なチャット対応
- パーソナライズされた顧客サポート
- 技術的アドバイス提供
- 多言語対応

#### ProductRecommendationAssistant  
- セマンティック商品推奨
- パーソナライズされた推奨エンジン
- 類似商品検索
- JSON形式の詳細推奨結果

#### SearchEnhancementAssistant
- セマンティック検索
- 検索クエリ拡張
- オートコンプリート機能

### 5. 環境設定ファイル
- **application-example.yml**: Azure OpenAI設定例とベストプラクティス
- 環境変数による設定管理
- セキュリティ考慮済み設定

### 6. 統合テスト
- Spring Context読み込み確認
- AIサービスBean生成確認
- Azure OpenAI接続テスト (環境変数設定時のみ)

## 🔧 技術仕様

### LangChain4j 1.1.0 API準拠
- **`AiServices.builder()`**: アノテーション不要のサービス生成
- **`ChatModel`**: ChatLanguageModelから変更
- **`serviceVersion`**: apiVersionから変更
- **最新Builder Pattern**: 設定の流暢なAPI

### Azure OpenAI統合
- **GPT-4o**: チャットモデル
- **text-embedding-3-small**: 埋め込みモデル  
- **API Version**: 2024-02-01 (最新安定版)
- **セキュリティ**: ログ無効化、API Key保護

### Spring Boot 3.2.3対応
- **依存注入**: Bean自動設定
- **設定プロパティ**: YAML設定バインディング
- **条件付きBean**: 環境に応じた設定

## 📁 プロジェクト構造

```
ai-support-service/
├── pom.xml                           # Maven依存関係設定
├── src/main/java/com/skishop/ai/
│   ├── config/
│   │   ├── LangChain4jConfig.java    # AI設定クラス
│   │   └── BusinessConfigProperties.java
│   ├── service/
│   │   ├── CustomerSupportAssistant.java
│   │   ├── ProductRecommendationAssistant.java
│   │   └── SearchEnhancementAssistant.java
│   └── controller/                   # REST API コントローラー
└── src/main/resources/
    ├── application.yml               # メイン設定
    └── application-example.yml       # 設定例とガイド
```

## ✅ 動作確認済み

1. **Maven Build**: `mvn clean compile` ✅
2. **Package Build**: `mvn clean package -DskipTests` ✅
3. **統合テスト**: Spring Context 読み込み ✅
4. **AIサービス Bean生成**: 3つのサービス正常作成 ✅

## 🚀 次のステップ

### 1. Azure OpenAI 本接続テスト
```bash
# 環境変数設定後にテスト実行
export AZURE_OPENAI_API_KEY="your-api-key"
export AZURE_OPENAI_ENDPOINT="https://your-resource.openai.azure.com/"
mvn test -Dtest=LangChain4jIntegrationTest
```

### 2. RESTコントローラーとの統合
- CustomerSupportAssistantとChatController連携確認
- ProductRecommendationAssistantとRecommendationController連携
- SearchEnhancementAssistantとSearchController連携

### 3. エラーハンドリング強化
- Azure OpenAI接続エラー処理
- レート制限とリトライ機能
- フォールバック機能

### 4. パフォーマンス最適化
- ChatMemory設定調整
- 埋め込みモデルキャッシュ
- 非同期処理対応

### 5. セキュリティ強化
- API Keyローテーション
- Managed Identity対応
- プロンプトインジェクション対策

## 📋 設定例

### 最小限の環境変数設定
```bash
AZURE_OPENAI_API_KEY=your-azure-openai-api-key
AZURE_OPENAI_ENDPOINT=https://your-resource.openai.azure.com/
AZURE_OPENAI_CHAT_DEPLOYMENT_NAME=gpt-4o
AZURE_OPENAI_EMBEDDING_DEPLOYMENT_NAME=text-embedding-3-small
```

### 推奨本番環境設定
- サービス専用のAzure OpenAIリソース
- Managed Identityによる認証
- Application Insightsによる監視
- Key Vaultによるシークレット管理

## 🔗 参考資料

- [LangChain4j 公式ドキュメント](https://docs.langchain4j.dev/)
- [Azure OpenAI Service](https://azure.microsoft.com/en-us/products/ai-services/openai-service)
- [LangChain4j Examples](https://github.com/langchain4j/langchain4j-examples)

---

**実装完了日**: 2025-06-20  
**LangChain4j Version**: 1.1.0-beta7 / 1.1.0-rc1  
**Spring Boot Version**: 3.2.3  
**Status**: ✅ Ready for Azure OpenAI Integration
