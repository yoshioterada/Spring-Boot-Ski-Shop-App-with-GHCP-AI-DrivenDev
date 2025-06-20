# スキー用品販売ショップサイト設計仕様書

## 目次

1. [システム概要](#システム概要)
2. [アーキテクチャ設計](#アーキテクチャ設計)
3. [マイクロサービス構成](#マイクロサービス構成)
4. [技術スタック](#技術スタック)
5. [データモデル](#データモデル)
6. [API設計](#api設計)
7. [認証・認可](#認証認可)
8. [インフラストラクチャ設計](#インフラストラクチャ設計)
9. [非機能要件](#非機能要件)
10. [開発・運用プロセス](#開発運用プロセス)
11. [移行戦略](#移行戦略)
12. [リスク管理](#リスク管理)

## システム概要

### プロジェクト目的

本プロジェクトは、スキー用品を専門に扱うEコマースプラットフォームをマイクロサービスアーキテクチャで構築することを目的としています。従来のモノリシックなアプローチから脱却し、スケーラビリティ、柔軟性、回復力に優れたシステムを実現します。季節性の高い需要に対応できるよう柔軟なスケーリング機能を備え、オンライン/オフラインの統合購買体験を提供します。

### ビジネス要件

- スキー用品（スキー板、ブーツ、ウェア、アクセサリー等）のオンライン販売
- 季節変動に対応できる柔軟な在庫管理と価格設定
- ユーザー個別の推奨商品提案（AI活用）とパーソナライズされた買い物体験
- OAuth 2.0/OpenID Connectを活用した複数の認証方法と支払い方法のサポート
- ポイント制度とクーポン機能による顧客ロイヤルティ強化と維持戦略
- 日本語と英語の多言語対応とグローバル展開の可能性
- モバイルデバイスを含む複数デバイス対応とレスポンシブデザイン
- リアルタイムの在庫確認と配送追跡
- シーズン前予約や限定商品の販売キャンペーン対応
- AIを活用したチャットボットによるカスタマーサポート

### ユーザー層

- 初心者から上級者までのスキーヤー、スノーボーダー
- スキーインストラクターやプロスキーヤー
- スキーリゾート関係者とスキー学校
- スキーチームや学校など団体顧客
- スキー用品メーカーとディストリビューター
- シーズナルバイヤーと年間を通したスキー愛好家

### 主要機能

- 詳細な商品カタログと高度な検索・フィルタリング機能
- パーソナライズされた商品レコメンデーション
- シームレスなチェックアウトプロセスと複数の支払いオプション
- 会員登録とプロフィール管理
- 注文履歴と注文追跡
- ポイント獲得と利用、クーポン適用
- リアルタイムの在庫確認と入荷通知
- 商品レビューとレーティング
- モバイルアプリとWebサイトの連携
- AIチャットボットによるカスタマーサポート

## アーキテクチャ設計

### 全体アーキテクチャ

本システムはマイクロサービスアーキテクチャを採用し、以下の特徴を持ちます：

- サービス間の疎結合と独立したデプロイサイクル
- APIゲートウェイを介した統一的なアクセスポイント
- イベント駆動型アーキテクチャによる非同期通信
- 各サービスの独立したデータストア（ポリグロット永続化）
- コンテナ化とオーケストレーションによる柔軟な運用
- クラウドネイティブ設計原則の適用

### アーキテクチャ図

```
+----------------------------------------------------------------------------------------+
|                                    クライアント層                                        |
|  +----------------+  +------------------+  +-----------------+  +-------------------+  |
|  | Webブラウザ     |  | モバイルアプリ     |  | 店舗端末        |  | パートナーシステム   |  |
|  +----------------+  +------------------+  +-----------------+  +-------------------+  |
+----------------------------------------------------------------------------------------+
                                        |
                                        | HTTPS
                                        v
+----------------------------------------------------------------------------------------+
|                                   アクセス層                                            |
|  +------------------------------+            +--------------------------------+        |
|  | API Gateway                  |<---------->| OAuth認証サービス                |        |
|  | (Spring Cloud Gateway/Azure) |            | (ユーザー認証・認可)              |        |
|  +------------------------------+            +--------------------------------+        |
+----------------------------------------------------------------------------------------+
                                        |
                                        | REST/gRPC
                                        v
+----------------------------------------------------------------------------------------+
|                                 アプリケーション層                                       |
|  +------------------+  +----------------+  +-------------------+  +----------------+   |
|  | 製品Webサイト     |  | ユーザー管理    |  | 販売管理           |  | 在庫管理        |   |
|  | サービス          |  | サービス        |  | サービス           |  | サービス        |   |
|  +------------------+  +----------------+  +-------------------+  +----------------+   |
|                                                                                        |
|  +------------------+  +----------------+  +-------------------+  +----------------+   |
|  | AIサポート        |  | 支払い・カート   |  | クーポン管理       |  | ポイント管理     |   |
|  | サービス          |  | サービス        |  | サービス           |  | サービス        |   |
|  +------------------+  +----------------+  +-------------------+  +----------------+   |
+----------------------------------------------------------------------------------------+
                                        |
                                        | JMS/Kafka
                                        v
+----------------------------------------------------------------------------------------+
|                                   データ層                                             |
|  +-----------------+  +---------------+  +----------------+  +------------------+      |
|  | 商品DB          |  | ユーザーDB     |  | 注文・販売DB    |  | インベントリDB    |      |
|  | (PostgreSQL)    |  | (MongoDB)     |  | (PostgreSQL)   |  | (PostgreSQL)     |      |
|  +-----------------+  +---------------+  +----------------+  +------------------+      |
|                                                                                        |
|  +-----------------+  +---------------+  +----------------+  +------------------+      |
|  | AI分析データ     |  | 決済データ     |  | クーポンDB     |  | ポイントDB        |      |
|  | (MongoDB)       |  | (PostgreSQL)  |  | (Redis)        |  | (Redis)          |      |
|  +-----------------+  +---------------+  +----------------+  +------------------+      |
+----------------------------------------------------------------------------------------+
```

### システム境界

- **内部システム**：上記アーキテクチャ図に示す全マイクロサービスとデータストア
  
- **外部システム**：
  - 決済ゲートウェイ（Stripe, PayPal等）
  - 配送業者API（ヤマト運輸, 佐川急便等）
  - ソーシャルメディア認証プロバイダ（Google, Facebook, Apple等）
  - メール送信サービス（SendGrid等）
  - 分析プラットフォーム（Google Analytics, Azure Application Insights等）

### アーキテクチャパターン

- **APIゲートウェイパターン**：全てのクライアント要求が単一エントリポイントを通過
- **サーキットブレーカーパターン**：障害の伝播防止と復旧メカニズム
- **CQRS（コマンドクエリ責務分離）**：読み取りと書き込みの分離による最適化
- **サガパターン**：分散トランザクション管理
- **バックエンド・フォー・フロントエンド（BFF）**：クライアント特化のAPIレイヤー
- **ストラングラーフィグパターン**：段階的なマイクロサービス移行
- **イベントソーシングパターン**：状態変更の記録とイベントストリーム

### 通信パターン

- **同期通信**：
  - RESTful API（OpenAPI/Swagger仕様）
  - GraphQL（クライアント最適化クエリ）
  - gRPC（効率的な内部サービス間通信）

- **非同期通信**：
  - イベント駆動（Apache Kafka）
  - メッセージキュー（RabbitMQ）
  - Webhookコールバック（外部連携）

- **イベントメッセージ例**：
  - OrderCreated：注文作成時
  - PaymentProcessed：支払い処理完了時
  - InventoryUpdated：在庫更新時
  - UserRegistered：ユーザー登録時
  - ProductViewed：商品閲覧時（分析用）

## マイクロサービス構成

### 1. APIゲートウェイ

**責務**:
- 外部リクエストのルーティングと負荷分散
- 認証・認可の統一的な処理
- レート制限とスロットリング
- リクエスト/レスポンスの変換
- APIドキュメンテーションの集約
- モニタリングとロギング

**技術スタック**:
- 開発環境：Spring Cloud Gateway
- 本番環境：Azure API Management
- Spring Security
- Spring Cloud Circuit Breaker
- JWT認証

**コンポーネント構成図**:

```mermaid
graph TB
    subgraph "APIゲートウェイ"
        direction TB
        API[API Gateway Controller] --> AUTH[認証フィルター]
        API --> ROUTE[ルーティングハンドラー]
        API --> THROTTLE[レート制限コンポーネント]
        API --> CIRCUIT[サーキットブレーカー]
        API --> TRANSFORM[リクエスト/レスポンス変換]
        API --> LOG[ロギングコンポーネント]
        API --> METRIC[メトリクス収集]
        
        ROUTE --> ROUTE_CONFIG[ルート設定]
        AUTH --> JWT[JWT検証]
        AUTH --> POLICY[認可ポリシー]
        CIRCUIT --> HEALTH[ヘルスチェック]
    end
    
    %% 外部システムとの接続
    API --> MS1[ユーザー管理サービス]
    API --> MS2[在庫管理サービス]
    API --> MS3[販売管理サービス]
    API --> MS4[その他マイクロサービス]
    JWT --> OAUTH[認証サービス]
```

**シーケンス図（認証とルーティング）**:

```mermaid
sequenceDiagram
    participant Client as クライアント
    participant Gateway as APIゲートウェイ
    participant Auth as 認証サービス
    participant Service as マイクロサービス
    
    Client->>Gateway: リクエスト送信
    Gateway->>Gateway: リクエスト検証
    Gateway->>Gateway: レート制限チェック
    
    alt 認証が必要なエンドポイント
        Gateway->>Auth: トークン検証リクエスト
        Auth->>Auth: JWT検証
        Auth-->>Gateway: 検証結果
        
        alt 認証失敗
            Gateway-->>Client: 401/403エラーレスポンス
        end
    end
    
    Gateway->>Gateway: ルート決定
    Gateway->>Service: リクエスト転送
    
    alt サーキットブレーカー開
        Gateway-->>Client: サービス一時利用不可レスポンス
    else サーキットブレーカー閉
        Service-->>Gateway: サービスレスポンス
        Gateway->>Gateway: レスポンス変換
        Gateway-->>Client: 最終レスポンス
    end
    
    Gateway->>Gateway: メトリクス記録
    Gateway->>Gateway: ログ記録
```

### 2. ユーザー管理サービス

**責務**:
- ユーザー登録・認証・プロファイル管理
- ユーザーロールと権限管理
- アカウント設定と嗜好設定
- パーソナライゼーション情報の管理
- ユーザーアクティビティ追跡

**主要エンティティ**:
- User
- Role
- Permission
- UserPreference
- UserActivity

**データストア**:
- PostgreSQL（ユーザーデータ）
- Redis（セッション情報、一時データ）

**コンポーネント構成図**:

```mermaid
graph TB
    subgraph "ユーザー管理サービス"
        direction TB
        API[REST API Controller] --> SERV[User Service]
        SERV --> REPO[User Repository]
        SERV --> ROLE_SERV[Role Service]
        SERV --> PREF_SERV[Preference Service]
        SERV --> ACT_SERV[Activity Service]
        
        ROLE_SERV --> ROLE_REPO[Role Repository]
        PREF_SERV --> PREF_REPO[Preference Repository]
        ACT_SERV --> ACT_REPO[Activity Repository]
        
        REPO --> DB[(PostgreSQL)]
        ROLE_REPO --> DB
        PREF_REPO --> DB
        ACT_REPO --> DB
        
        CACHE[Cache Service] --> REDIS[(Redis)]
        SERV --> CACHE
        
        EVENT[Event Publisher] --> KAFKA[Kafka]
        SERV --> EVENT
        
        VALID[Validation Component]
        SERV --> VALID
    end
    
    %% 外部システムとの接続
    API --> AUTH[認証サービス]
    API --> GATEWAY[APIゲートウェイ]
```

**シーケンス図（ユーザー登録プロセス）**:

```mermaid
sequenceDiagram
    participant Client as クライアント
    participant Gateway as APIゲートウェイ
    participant UserAPI as ユーザーAPI
    participant UserService as ユーザーサービス
    participant Validation as バリデーション
    participant DB as データベース
    participant EventBus as イベントバス
    
    Client->>Gateway: ユーザー登録リクエスト
    Gateway->>UserAPI: リクエスト転送
    UserAPI->>Validation: 入力検証
    
    alt 検証エラー
        Validation-->>UserAPI: バリデーションエラー
        UserAPI-->>Gateway: 400 エラーレスポンス
        Gateway-->>Client: エラーレスポンス
    else 検証成功
        Validation-->>UserAPI: 検証成功
        UserAPI->>UserService: ユーザー作成
        UserService->>DB: ユーザー情報保存
        DB-->>UserService: 保存成功
        
        UserService->>EventBus: ユーザー作成イベント発行
        UserService-->>UserAPI: ユーザー作成成功
        
        UserAPI->>UserAPI: JWTトークン生成
        UserAPI-->>Gateway: 201 Created レスポンス
        Gateway-->>Client: 登録成功レスポンス
    end
    
    opt オプショナル:メール確認
        UserService->>UserService: 確認メール送信
        Client->>Gateway: メール確認リンククリック
        Gateway->>UserAPI: 確認リクエスト
        UserAPI->>UserService: メール確認処理
        UserService->>DB: ユーザーステータス更新
        DB-->>UserService: 更新成功
        UserService-->>UserAPI: 確認成功
        UserAPI-->>Gateway: 成功レスポンス
        Gateway-->>Client: 確認完了ページ
    end
```

### 3. 在庫管理サービス

**責務**:
- 商品カタログ管理
- 在庫状況の追跡と更新
- 商品属性とカテゴリ管理
- 価格設定と割引管理
- 入荷・出荷処理
- 商品画像・メディア管理

**主要エンティティ**:
- Product
- Category
- Inventory
- Supplier
- PriceHistory
- ProductImage

**データストア**:
- MongoDB（商品カタログ、属性）
- PostgreSQL（在庫情報、トランザクション）
- Blob Storage（商品画像）

**コンポーネント構成図**:

```mermaid
graph TB
    subgraph "在庫管理サービス"
        direction TB
        API[REST API Controller] --> PROD_SERV[Product Service]
        API --> INV_SERV[Inventory Service]
        API --> CAT_SERV[Category Service]
        API --> PRICE_SERV[Pricing Service]
        API --> IMG_SERV[Media Service]
        
        PROD_SERV --> PROD_REPO[Product Repository]
        INV_SERV --> INV_REPO[Inventory Repository]
        CAT_SERV --> CAT_REPO[Category Repository]
        PRICE_SERV --> PRICE_REPO[Price Repository]
        IMG_SERV --> IMG_REPO[Image Repository]
        
        PROD_REPO --> MONGO[(MongoDB)]
        CAT_REPO --> MONGO
        
        INV_REPO --> PSQL[(PostgreSQL)]
        PRICE_REPO --> PSQL
        
        IMG_REPO --> BLOB[(Azure Blob Storage)]
        
        EVENT[Event Publisher] --> KAFKA[Kafka]
        INV_SERV --> EVENT
        PRICE_SERV --> EVENT
        
        CACHE[Cache Service] --> REDIS[(Redis)]
        PROD_SERV --> CACHE
        CAT_SERV --> CACHE
        
        SEARCH[Search Index Service] --> ES[(Elasticsearch)]
        PROD_SERV --> SEARCH
    end
    
    %% 外部システムとの接続
    API --> GATEWAY[APIゲートウェイ]
    EVENT --> SALES[販売管理サービス]
    EVENT --> AI[AI対応サポートサービス]
```

**シーケンス図（在庫更新プロセス）**:

```mermaid
sequenceDiagram
    participant Gateway as APIゲートウェイ
    participant InvAPI as 在庫API
    participant InvService as 在庫サービス
    participant ProdService as 商品サービス
    participant DB as データベース
    participant Cache as キャッシュ
    participant EventBus as イベントバス
    participant Sales as 販売管理サービス
    
    alt 入荷処理
        Gateway->>InvAPI: 入荷情報送信
        InvAPI->>InvService: 入荷処理要求
        InvService->>DB: 現在の在庫確認
        DB-->>InvService: 在庫情報
        InvService->>DB: 在庫数更新
        DB-->>InvService: 更新成功
        InvService->>EventBus: 在庫更新イベント発行
        EventBus->>Sales: 在庫変更通知
    end
    
    alt 販売による在庫減少
        Sales->>EventBus: 注文確定イベント
        EventBus->>InvService: 在庫減少要求
        InvService->>DB: 在庫確認
        
        alt 在庫十分
            InvService->>DB: 在庫数減少
            DB-->>InvService: 更新成功
            InvService->>EventBus: 在庫更新完了イベント
            EventBus->>Sales: 在庫確保完了通知
        else 在庫不足
            InvService->>EventBus: 在庫不足イベント
            EventBus->>Sales: 在庫不足通知
        end
    end
    
    InvAPI->>InvService: 在庫状況取得
    InvService->>Cache: キャッシュ確認
    
    alt キャッシュヒット
        Cache-->>InvService: キャッシュデータ
    else キャッシュミス
        InvService->>DB: 在庫データ取得
        DB-->>InvService: 在庫データ
        InvService->>Cache: キャッシュ更新
    end
    
    InvService-->>InvAPI: 在庫情報
    InvAPI-->>Gateway: レスポンス返却
```

### 4. 販売管理サービス

**責務**:
- 注文処理と管理
- 販売分析とレポート
- 返品・交換処理
- 配送手配と追跡
- 販売履歴

**主要エンティティ**:
- Order
- OrderItem
- Shipment
- Return
- Invoice

**データストア**:
- PostgreSQL（注文、配送情報）
- Elasticsearch（検索、分析）

**コンポーネント構成図**:

```mermaid
graph TB
    subgraph "販売管理サービス"
        direction TB
        API[REST API Controller] --> ORDER_SERV[Order Service]
        API --> SHIP_SERV[Shipment Service]
        API --> RET_SERV[Return Service]
        API --> REP_SERV[Report Service]
        
        ORDER_SERV --> ORDER_REPO[Order Repository]
        SHIP_SERV --> SHIP_REPO[Shipment Repository]
        RET_SERV --> RET_REPO[Return Repository]
        
        ORDER_REPO --> DB[(PostgreSQL)]
        SHIP_REPO --> DB
        RET_REPO --> DB
        
        REP_SERV --> ES[(Elasticsearch)]
        
        EVENT[Event Publisher] --> KAFKA[Kafka]
        ORDER_SERV --> EVENT
        SHIP_SERV --> EVENT
        
        ORDER_SERV --> SAGA[Saga Coordinator]
        
        ANALYSIS[Analytics Engine] --> ES
        REP_SERV --> ANALYSIS
        
        EXPORT[Report Exporter]
        REP_SERV --> EXPORT
    end
    
    %% 外部システムとの接続
    API --> GATEWAY[APIゲートウェイ]
    EVENT --> INV[在庫管理サービス]
    EVENT --> PAY[支払い・カート処理サービス]
    EVENT --> POINT[ポイント管理サービス]
    SAGA --> INV
    SAGA --> PAY
    SAGA --> POINT
```

**シーケンス図（注文処理フロー）**:

```mermaid
sequenceDiagram
    participant Client as クライアント
    participant Gateway as APIゲートウェイ
    participant SalesAPI as 販売API
    participant OrderService as 注文サービス
    participant SagaCoord as Sagaコーディネーター
    participant PaymentService as 支払いサービス
    participant InventoryService as 在庫サービス
    participant ShipmentService as 配送サービス
    participant PointService as ポイントサービス
    participant DB as データベース
    
    Client->>Gateway: 注文確定リクエスト
    Gateway->>SalesAPI: リクエスト転送
    SalesAPI->>OrderService: 注文処理要求
    OrderService->>SagaCoord: トランザクション開始
    
    %% 在庫確認ステップ
    SagaCoord->>InventoryService: 在庫確認・確保
    
    alt 在庫不足
        InventoryService-->>SagaCoord: 在庫不足エラー
        SagaCoord-->>OrderService: トランザクション失敗
        OrderService-->>SalesAPI: 処理失敗
        SalesAPI-->>Gateway: エラーレスポンス
        Gateway-->>Client: 在庫不足エラー
    else 在庫確保成功
        InventoryService-->>SagaCoord: 在庫確保完了
        
        %% 支払い処理ステップ
        SagaCoord->>PaymentService: 支払い処理
        
        alt 支払い失敗
            PaymentService-->>SagaCoord: 支払い失敗
            SagaCoord->>InventoryService: 在庫確保取消
            InventoryService-->>SagaCoord: 在庫解放完了
            SagaCoord-->>OrderService: トランザクション失敗
            OrderService-->>SalesAPI: 処理失敗
            SalesAPI-->>Gateway: エラーレスポンス
            Gateway-->>Client: 支払い失敗エラー
        else 支払い成功
            PaymentService-->>SagaCoord: 支払い成功
            
            %% 注文登録
            SagaCoord->>OrderService: 注文確定
            OrderService->>DB: 注文情報保存
            DB-->>OrderService: 保存成功
            
            %% 配送手配
            SagaCoord->>ShipmentService: 配送手配
            ShipmentService->>DB: 配送情報登録
            DB-->>ShipmentService: 登録成功
            ShipmentService-->>SagaCoord: 配送手配完了
            
            %% ポイント付与
            SagaCoord->>PointService: ポイント付与
            PointService-->>SagaCoord: ポイント処理完了
            
            %% トランザクション完了
            SagaCoord-->>OrderService: トランザクション成功
            OrderService-->>SalesAPI: 処理成功
            SalesAPI-->>Gateway: 成功レスポンス
            Gateway-->>Client: 注文完了確認
        end
    end
```

### 5. AI対応サポート機能サービス

**責務**:
- パーソナライズド商品推奨
- 検索最適化とオートコンプリート
- チャットボットによる顧客サポート
- 需要予測
- ユーザー行動分析

**技術スタック**:
- Azure AI Services
- Azure OpenAI Service
- Spring AI
- Vector Database（Pinecone, Qdrant等）

**データフロー**:
- ユーザー行動データの収集
- 商品推奨モデルのトレーニングとデプロイ
- リアルタイム推奨と検索拡張

**コンポーネント構成図**:

```mermaid
graph TB
    subgraph "AI対応サポート機能サービス"
        direction TB
        API[REST/GraphQL API] --> REC_SERV[Recommendation Service]
        API --> SEARCH_SERV[Search Service]
        API --> CHAT_SERV[Chatbot Service]
        API --> FORECAST_SERV[Forecast Service]
        API --> ANALYTICS_SERV[Analytics Service]
        
        REC_SERV --> VECTOR_DB[(Vector Database)]
        SEARCH_SERV --> VECTOR_DB
        SEARCH_SERV --> ES[(Elasticsearch)]
        
        CHAT_SERV --> OPENAI[Azure OpenAI]
        CHAT_SERV --> KNOWLEDGE[Knowledge Base]
        
        FORECAST_SERV --> ML_MODELS[ML Models]
        ANALYTICS_SERV --> DATA_LAKE[(Data Lake)]
        
        ETL[ETL Pipeline] --> DATA_LAKE
        ML_PIPELINE[ML Training Pipeline] --> ML_MODELS
        ETL --> ML_PIPELINE
        
        REC_ENGINE[Recommendation Engine] --> ML_MODELS
        REC_ENGINE --> VECTOR_DB
        REC_SERV --> REC_ENGINE
        
        EVENT_CONSUMER[Event Consumer] --> KAFKA[Kafka]
        EVENT_CONSUMER --> ETL
    end
    
    %% 外部システムとの接続
    API --> GATEWAY[APIゲートウェイ]
    EVENT_CONSUMER --> USER[ユーザー管理サービス]
    EVENT_CONSUMER --> INVENTORY[在庫管理サービス]
    EVENT_CONSUMER --> SALES[販売管理サービス]
```

**シーケンス図（商品レコメンデーション）**:

```mermaid
sequenceDiagram
    participant Client as クライアント
    participant Gateway as APIゲートウェイ
    participant AIAPI as AI API
    participant RecService as レコメンデーションサービス
    participant UserService as ユーザー管理サービス
    participant VectorDB as ベクトルDB
    participant MLModel as 機械学習モデル
    
    Client->>Gateway: 商品ページ閲覧
    Gateway->>AIAPI: レコメンデーション要求
    
    %% ユーザー情報取得
    AIAPI->>UserService: ユーザープロフィール取得
    UserService-->>AIAPI: ユーザー情報
    
    %% 商品ベクトル検索
    AIAPI->>RecService: ユーザー向けレコメンデーション
    RecService->>VectorDB: 現在の商品ベクトル取得
    VectorDB-->>RecService: 商品ベクトル
    
    %% パーソナライズドレコメンデーション
    RecService->>MLModel: レコメンデーション計算
    MLModel->>MLModel: ユーザー嗜好との類似性計算
    MLModel-->>RecService: レコメンデーション結果
    
    RecService->>RecService: コンテキスト考慮フィルタリング
    RecService-->>AIAPI: レコメンド商品リスト
    AIAPI-->>Gateway: レコメンド結果レスポンス
    Gateway-->>Client: パーソナライズド商品表示
    
    %% 行動追跡（非同期）
    Client->>Gateway: 商品インタラクション
    Gateway->>AIAPI: ユーザーアクション記録
    AIAPI->>RecService: フィードバック登録
    RecService->>VectorDB: ユーザー行動プロファイル更新
    
    %% 定期バッチ処理（別シーケンス）
    Note over MLModel: 定期的なモデル再トレーニング（夜間バッチ）
```

### 6. 支払い・カート処理サービス

**責務**:
- ショッピングカート管理
- 支払い処理とゲートウェイ連携
- 価格計算と税金計算
- 注文確認と領収書生成
- 決済セキュリティ（PCI DSS対応）

**主要エンティティ**:
- Cart
- CartItem
- Payment
- PaymentMethod
- Transaction

**データストア**:
- Redis（カート情報）
- PostgreSQL（支払い記録、トランザクション）

**コンポーネント構成図**:

```mermaid
graph TB
    subgraph "支払い・カート処理サービス"
        direction TB
        API[REST API Controller] --> CART_SERV[Cart Service]
        API --> PAY_SERV[Payment Service]
        API --> PRICE_SERV[Pricing Service]
        API --> DOC_SERV[Document Service]
        
        CART_SERV --> CART_REPO[Cart Repository]
        PAY_SERV --> PAY_REPO[Payment Repository]
        PRICE_SERV --> PRICE_REPO[Pricing Repository]
        
        CART_REPO --> REDIS[(Redis)]
        PAY_REPO --> DB[(PostgreSQL)]
        PRICE_REPO --> DB
        
        PAY_SERV --> PAY_GATEWAY[Payment Gateway Adapter]
        PAY_GATEWAY --> EXTERNAL[外部決済サービス]
        
        EVENT[Event Publisher] --> KAFKA[Kafka]
        PAY_SERV --> EVENT
        CART_SERV --> EVENT
        
        DOC_SERV --> TEMPLATE[Template Engine]
        DOC_SERV --> PDF[PDF Generator]
        
        SECURITY[Security Component]
        PAY_SERV --> SECURITY
        PAY_GATEWAY --> SECURITY
    end
    
    %% 外部システムとの接続
    API --> GATEWAY[APIゲートウェイ]
    EVENT --> SALES[販売管理サービス]
    EVENT --> INVENTORY[在庫管理サービス]
    EVENT --> COUPON[クーポンサービス]
    EVENT --> POINT[ポイントサービス]
```

**シーケンス図（カート追加と支払いプロセス）**:

```mermaid
sequenceDiagram
    participant Client as クライアント
    participant Gateway as APIゲートウェイ
    participant CartAPI as カートAPI
    participant CartService as カートサービス
    participant PriceService as 価格計算サービス
    participant PayService as 支払いサービス
    participant PayGateway as 決済ゲートウェイ
    participant Redis as Redisキャッシュ
    participant DB as データベース
    participant EventBus as イベントバス
    
    %% カートへの商品追加フロー
    Client->>Gateway: 商品をカートに追加
    Gateway->>CartAPI: リクエスト転送
    CartAPI->>CartService: カート更新処理
    CartService->>Redis: 現在のカート取得
    Redis-->>CartService: カート情報
    CartService->>Redis: カート更新
    Redis-->>CartService: 更新成功
    CartService->>PriceService: 小計計算要求
    PriceService->>PriceService: 税金・送料・割引計算
    PriceService-->>CartService: 計算結果
    CartService-->>CartAPI: 更新完了レスポンス
    CartAPI-->>Gateway: カート情報
    Gateway-->>Client: カート更新完了
    
    %% 支払い処理フロー
    Client->>Gateway: 支払い処理開始
    Gateway->>CartAPI: チェックアウト要求
    CartAPI->>PayService: 支払い処理開始
    
    PayService->>CartService: カート情報取得
    CartService->>Redis: カート読み取り
    Redis-->>CartService: カート情報
    CartService-->>PayService: カート情報
    
    PayService->>PayService: 支払い情報検証
    PayService->>PayGateway: 支払い処理要求
    PayGateway->>PayGateway: 決済情報暗号化
    PayGateway->>ExternalPayment: 外部決済サービス呼び出し
    
    alt 支払い成功
        ExternalPayment-->>PayGateway: 支払い成功
        PayGateway-->>PayService: 支払い完了通知
        PayService->>DB: トランザクション記録
        DB-->>PayService: 記録完了
        
        PayService->>EventBus: 支払い完了イベント発行
        EventBus->>SalesService: 注文処理通知
        
        PayService-->>CartAPI: 支払い完了レスポンス
        CartAPI-->>Gateway: 支払い成功レスポンス
        Gateway-->>Client: 支払い成功表示
        
        %% カートクリア（非同期）
        PayService->>CartService: カートクリア要求
        CartService->>Redis: カート削除
    else 支払い失敗
        ExternalPayment-->>PayGateway: 支払い失敗
        PayGateway-->>PayService: エラー通知
        PayService-->>CartAPI: 支払い失敗レスポンス
        CartAPI-->>Gateway: エラーレスポンス
        Gateway-->>Client: 支払い失敗メッセージ
    end
```

### 7. 認証サービス（OAuth）

**責務**:
- OAuth 2.0/OpenID Connectフロー処理
- JWTトークン生成・検証
- ソーシャルログイン連携
- 多要素認証（MFA）
- セッション管理

**技術スタック**:
- Spring Security OAuth2
- Keycloak
- JWT

**セキュリティ機能**:
- トークンベース認証
- アクセストークンとリフレッシュトークン管理
- スコープベースの権限制御
- トークン失効管理

**コンポーネント構成図**:

```mermaid
graph TB
    subgraph "認証サービス（OAuth）"
        direction TB
        API[OAuth API Controller] --> AUTH_SERVER[Authorization Server]
        API --> TOKEN_SERV[Token Service]
        API --> USER_AUTH[User Authentication]
        API --> SOCIAL[Social Login Service]
        API --> MFA[MFA Service]
        
        AUTH_SERVER --> CLIENT_REPO[Client Repository]
        TOKEN_SERV --> TOKEN_REPO[Token Repository]
        USER_AUTH --> CRED_SERV[Credential Service]
        
        TOKEN_SERV --> JWT_GEN[JWT Generator]
        TOKEN_SERV --> TOKEN_VALID[Token Validator]
        
        SOCIAL --> SOCIAL_PROVIDERS[Social Identity Providers]
        MFA --> MFA_PROVIDERS[MFA Providers]
        
        CACHE[Token Cache] --> REDIS[(Redis)]
        TOKEN_SERV --> CACHE
        
        CLIENT_REPO --> DB[(PostgreSQL)]
        TOKEN_REPO --> DB
        
        SECURITY[Security Components]
        AUTH_SERVER --> SECURITY
        TOKEN_SERV --> SECURITY
        
        EVENT[Event Publisher] --> KAFKA[Kafka]
        USER_AUTH --> EVENT
    end
    
    %% 外部システムとの接続
    API --> GATEWAY[APIゲートウェイ]
    USER_AUTH --> USER_SERVICE[ユーザー管理サービス]
    SOCIAL_PROVIDERS --> EXTERNAL[外部IDプロバイダー]
```

**シーケンス図（OAuth認証フロー）**:

```mermaid
sequenceDiagram
    participant Client as クライアント
    participant Frontend as フロントエンド
    participant Gateway as APIゲートウェイ
    participant AuthService as 認証サービス
    participant UserService as ユーザー管理サービス
    participant SocialProvider as 外部IDプロバイダー
    participant TokenService as トークンサービス
    participant Cache as トークンキャッシュ
    
    %% 認証コードフロー
    Client->>Frontend: ログイン要求
    Frontend->>Gateway: 認証開始リクエスト
    Gateway->>AuthService: /oauth/authorize
    AuthService->>AuthService: クライアント検証
    
    alt ソーシャルログイン
        AuthService->>SocialProvider: リダイレクト
        Client->>SocialProvider: 認証
        SocialProvider-->>Client: 認証コード付きリダイレクト
        Client->>Frontend: 認証コード
        Frontend->>Gateway: 認証コード送信
        Gateway->>AuthService: /oauth/token
        AuthService->>SocialProvider: トークン検証
        SocialProvider-->>AuthService: ユーザー情報
        
        alt 初回ログイン
            AuthService->>UserService: ユーザー作成
            UserService-->>AuthService: ユーザー情報
        else 既存ユーザー
            AuthService->>UserService: ユーザー取得
            UserService-->>AuthService: ユーザー情報
        end
    else パスワード認証
        AuthService-->>Client: ログインフォーム
        Client->>AuthService: 認証情報送信
        AuthService->>UserService: 認証情報検証
        UserService-->>AuthService: 認証結果
        
        alt 多要素認証必要
            AuthService-->>Client: MFAチャレンジ
            Client->>AuthService: MFAレスポンス
            AuthService->>AuthService: MFA検証
        end
    end
    
    %% トークン生成
    AuthService->>TokenService: トークン生成要求
    TokenService->>TokenService: JWTトークン生成
    TokenService->>Cache: リフレッシュトークン保存
    Cache-->>TokenService: 保存成功
    TokenService-->>AuthService: トークンペア
    
    AuthService-->>Gateway: アクセス・リフレッシュトークン
    Gateway-->>Frontend: トークン送信
    Frontend->>Frontend: トークン保存
    Frontend-->>Client: ログイン成功
    
    %% トークンリフレッシュ（別シーケンス）
    Note over Client, Cache: トークンリフレッシュフロー
    Frontend->>Gateway: リフレッシュトークン送信
    Gateway->>AuthService: /oauth/token (grant_type=refresh_token)
    AuthService->>TokenService: トークン検証
    TokenService->>Cache: リフレッシュトークン検証
    Cache-->>TokenService: 検証結果
    TokenService->>TokenService: 新JWTトークン生成
    TokenService-->>AuthService: 新トークンペア
    AuthService-->>Gateway: 新アクセス・リフレッシュトークン
    Gateway-->>Frontend: 新トークン
```

### 8. 製品販売Webサイト（フロントエンド）

**責務**:
- レスポンシブWebインターフェース提供
- ユーザーエクスペリエンス最適化
- クライアントサイドのパフォーマンス最適化
- 多言語対応
- SEO対策

**技術スタック**:
- Next.js (React)
- TypeScript
- TailwindCSS
- Redux/Zustand
- GraphQL（Apollo Client）

**アクセシビリティ**:
- WCAG 2.1 AA準拠
- キーボードナビゲーション
- スクリーンリーダー対応

**コンポーネント構成図**:

```mermaid
graph TB
    subgraph "製品販売Webサイト（フロントエンド）"
        direction TB
        PAGES[Next.js Pages] --> LAYOUTS[Layout Components]
        PAGES --> COMP[UI Components]
        PAGES --> HOOKS[Custom Hooks]
        
        LAYOUTS --> HEADER[Header Component]
        LAYOUTS --> FOOTER[Footer Component]
        LAYOUTS --> NAV[Navigation Component]
        
        COMP --> PRODUCT[Product Components]
        COMP --> CART[Cart Components]
        COMP --> CHECKOUT[Checkout Components]
        COMP --> USER[User Components]
        COMP --> SEARCH[Search Components]
        
        STATE[State Management] --> REDUX[Redux Store]
        STATE --> CONTEXT[React Context]
        
        API[API Layer] --> REST[REST Client]
        API --> GRAPHQL[GraphQL Client]
        
        SERVICES[Services] --> AUTH[Auth Service]
        SERVICES --> CART_SERV[Cart Service]
        SERVICES --> PRODUCT_SERV[Product Service]
        SERVICES --> USER_SERV[User Service]
        
        UTILS[Utilities] --> I18N[Internationalization]
        UTILS --> VALIDATORS[Form Validators]
        UTILS --> FORMATTER[Data Formatters]
        
        HOOKS --> API
        COMP --> STATE
        COMP --> SERVICES
        SERVICES --> API
    end
    
    %% 外部システムとの接続
    API --> GATEWAY[APIゲートウェイ]
    AUTH --> AUTH_SERVICE[認証サービス]
```

**シーケンス図（商品閲覧と購入フロー）**:

```mermaid
sequenceDiagram
    participant User as ユーザー
    participant Browser as ブラウザ
    participant FrontEnd as Webフロントエンド
    participant ApiGateway as APIゲートウェイ
    participant AuthService as 認証サービス
    participant ProductService as 在庫管理サービス
    participant CartService as カートサービス
    participant AIService as AIサポートサービス
    participant PaymentService as 支払いサービス
    
    %% ページ初期表示
    User->>Browser: サイトアクセス
    Browser->>FrontEnd: ページリクエスト
    
    alt サーバーサイドレンダリング (SSR)
        FrontEnd->>ApiGateway: データフェッチ
        ApiGateway->>ProductService: 商品データ要求
        ProductService-->>ApiGateway: 商品データ
        ApiGateway-->>FrontEnd: データレスポンス
        FrontEnd->>FrontEnd: ページレンダリング
        FrontEnd-->>Browser: HTML応答
    else 静的ページ生成 (SSG)
        FrontEnd-->>Browser: 事前生成されたHTML
        Browser->>FrontEnd: Hydration
    end
    
    Browser-->>User: ページ表示
    
    %% ユーザー認証
    User->>Browser: ログインクリック
    Browser->>FrontEnd: ログイン画面表示
    FrontEnd-->>Browser: ログインフォーム
    User->>Browser: 認証情報入力
    Browser->>FrontEnd: ログインリクエスト
    FrontEnd->>ApiGateway: 認証リクエスト
    ApiGateway->>AuthService: 認証処理
    AuthService-->>ApiGateway: JWT トークン
    ApiGateway-->>FrontEnd: 認証成功レスポンス
    FrontEnd->>FrontEnd: トークン保存
    FrontEnd-->>Browser: ログイン成功表示
    Browser-->>User: ログイン完了
    
    %% 商品閲覧
    User->>Browser: 商品検索/カテゴリ選択
    Browser->>FrontEnd: 商品一覧リクエスト
    FrontEnd->>ApiGateway: 商品検索API呼び出し
    ApiGateway->>ProductService: 商品検索
    ProductService-->>ApiGateway: 商品リスト
    ApiGateway->>AIService: パーソナライズ推奨
    AIService-->>ApiGateway: 推奨商品
    ApiGateway-->>FrontEnd: 検索結果＋推奨
    FrontEnd-->>Browser: 商品一覧表示
    Browser-->>User: 商品表示
    
    %% カート操作
    User->>Browser: 商品をカートに追加
    Browser->>FrontEnd: カート追加アクション
    FrontEnd->>ApiGateway: カート更新API呼び出し
    ApiGateway->>CartService: カート更新
    CartService-->>ApiGateway: 更新成功
    ApiGateway-->>FrontEnd: カート情報
    FrontEnd-->>Browser: カート表示更新
    Browser-->>User: カート追加完了通知
    
    %% 決済処理
    User->>Browser: チェックアウトボタンクリック
    Browser->>FrontEnd: チェックアウト画面リクエスト
    FrontEnd->>ApiGateway: カート情報取得
    ApiGateway->>CartService: カート取得
    CartService-->>ApiGateway: カート詳細
    ApiGateway-->>FrontEnd: カート情報
    FrontEnd-->>Browser: 注文確認画面
    
    User->>Browser: 支払い情報入力・確定
    Browser->>FrontEnd: 支払い処理リクエスト
    FrontEnd->>ApiGateway: 支払い処理API呼び出し
    ApiGateway->>PaymentService: 支払い処理
    
    alt 支払い成功
        PaymentService-->>ApiGateway: 支払い成功
        ApiGateway-->>FrontEnd: 支払い成功レスポンス
        FrontEnd-->>Browser: 注文完了画面
        Browser-->>User: 注文確認表示
    else 支払い失敗
        PaymentService-->>ApiGateway: 支払い失敗
        ApiGateway-->>FrontEnd: エラーレスポンス
        FrontEnd-->>Browser: エラー表示
        Browser-->>User: 支払い失敗通知
    end
```

### 9. クーポン提供機能サービス

**責務**:
- クーポン作成と管理
- クーポン配布ルール設定
- クーポン適用と検証
- キャンペーン管理
- クーポン使用状況追跡

**主要エンティティ**:
- Coupon
- CouponType
- CouponUsage
- Campaign
- Promotion

**データストア**:
- PostgreSQL（クーポンデータ）
- Redis（アクティブクーポン、キャッシュ）

**コンポーネント構成図**:

```mermaid
graph TB
    subgraph "クーポン提供機能サービス"
        direction TB
        API[REST API Controller] --> COUPON_SERV[Coupon Service]
        API --> CAMPAIGN_SERV[Campaign Service]
        API --> VALID_SERV[Validation Service]
        API --> DIST_SERV[Distribution Service]
        
        COUPON_SERV --> COUPON_REPO[Coupon Repository]
        CAMPAIGN_SERV --> CAMPAIGN_REPO[Campaign Repository]
        VALID_SERV --> USAGE_REPO[Usage Repository]
        DIST_SERV --> DIST_REPO[Distribution Repository]
        
        COUPON_REPO --> DB[(PostgreSQL)]
        CAMPAIGN_REPO --> DB
        USAGE_REPO --> DB
        DIST_REPO --> DB
        
        CACHE[Cache Service] --> REDIS[(Redis)]
        COUPON_SERV --> CACHE
        VALID_SERV --> CACHE
        
        RULE_ENGINE[Rule Engine]
        VALID_SERV --> RULE_ENGINE
        DIST_SERV --> RULE_ENGINE
        
        EVENT[Event Publisher] --> KAFKA[Kafka]
        COUPON_SERV --> EVENT
        VALID_SERV --> EVENT
        
        GENERATOR[Coupon Code Generator]
        COUPON_SERV --> GENERATOR
    end
    
    %% 外部システムとの接続
    API --> GATEWAY[APIゲートウェイ]
    EVENT --> SALES[販売管理サービス]
    EVENT --> USER[ユーザー管理サービス]
    EVENT --> CART[カート処理サービス]
```

**シーケンス図（クーポン適用プロセス）**:

```mermaid
sequenceDiagram
    participant Client as クライアント
    participant Gateway as APIゲートウェイ
    participant CartService as カートサービス
    participant CouponAPI as クーポンAPI
    participant ValidService as 検証サービス
    participant RuleEngine as ルールエンジン
    participant Cache as Redisキャッシュ
    participant DB as データベース
    participant EventBus as イベントバス
    
    %% クーポンコード入力
    Client->>Gateway: クーポンコード送信
    Gateway->>CartService: クーポン適用リクエスト
    CartService->>CouponAPI: クーポン検証要求
    
    %% クーポン検証
    CouponAPI->>ValidService: クーポン検証
    ValidService->>Cache: クーポン情報取得
    
    alt キャッシュヒット
        Cache-->>ValidService: クーポン情報
    else キャッシュミス
        ValidService->>DB: クーポン情報照会
        DB-->>ValidService: クーポン情報
        ValidService->>Cache: クーポン情報キャッシュ
    end
    
    %% ルール検証
    ValidService->>RuleEngine: 適用条件チェック
    RuleEngine->>RuleEngine: ルール評価
    
    alt 無効なクーポン
        RuleEngine-->>ValidService: 無効（期限切れ/使用済み等）
        ValidService-->>CouponAPI: 検証失敗
        CouponAPI-->>CartService: クーポン無効レスポンス
        CartService-->>Gateway: エラーレスポンス
        Gateway-->>Client: クーポン無効メッセージ
    else 有効だが条件不適合
        RuleEngine-->>ValidService: 条件不適合
        ValidService-->>CouponAPI: 条件エラー
        CouponAPI-->>CartService: 条件不適合レスポンス
        CartService-->>Gateway: 条件エラーレスポンス
        Gateway-->>Client: 条件不適合メッセージ
    else 有効なクーポン
        RuleEngine-->>ValidService: 検証成功
        ValidService-->>CouponAPI: クーポン詳細情報
        CouponAPI-->>CartService: 割引情報
        
        %% カートへの適用
        CartService->>CartService: 割引計算
        CartService-->>Gateway: 更新後のカート情報
        Gateway-->>Client: 割引適用表示
        
        %% 使用予約（注文確定時に使用確定）
        ValidService->>Cache: クーポン使用予約
    end
    
    %% 注文確定時のクーポン使用確定
    Note over Client, EventBus: 注文確定時
    CartService->>CouponAPI: クーポン使用確定
    CouponAPI->>ValidService: 使用記録
    ValidService->>DB: 使用情報保存
    DB-->>ValidService: 保存成功
    ValidService->>EventBus: クーポン使用イベント
    EventBus->>Analytics: 使用統計更新
```

### 10. ポイント提供管理機能サービス

**責務**:
- ポイント付与・消費・計算
- ポイントルール管理
- ポイント履歴追跡
- ポイント有効期限管理
- ポイントキャンペーン

**主要エンティティ**:
- PointAccount
- PointTransaction
- PointRule
- PointExpiry
- PointCampaign

**データストア**:
- PostgreSQL（ポイントデータ、トランザクション）
- Redis（リアルタイムポイント情報）

**コンポーネント構成図**:

```mermaid
graph TB
    subgraph "ポイント提供管理機能サービス"
        direction TB
        API[REST API Controller] --> POINT_SERV[Point Service]
        API --> RULE_SERV[Rule Service]
        API --> CAMPAIGN_SERV[Campaign Service]
        API --> HISTORY_SERV[History Service]
        
        POINT_SERV --> ACCOUNT_REPO[Account Repository]
        POINT_SERV --> TRANS_REPO[Transaction Repository]
        RULE_SERV --> RULE_REPO[Rule Repository]
        CAMPAIGN_SERV --> CAMPAIGN_REPO[Campaign Repository]
        HISTORY_SERV --> HISTORY_REPO[History Repository]
        
        ACCOUNT_REPO --> DB[(PostgreSQL)]
        TRANS_REPO --> DB
        RULE_REPO --> DB
        CAMPAIGN_REPO --> DB
        HISTORY_REPO --> DB
        
        CACHE[Cache Service] --> REDIS[(Redis)]
        POINT_SERV --> CACHE
        
        CALC_ENGINE[Calculation Engine]
        POINT_SERV --> CALC_ENGINE
        RULE_SERV --> CALC_ENGINE
        
        EXPIRY[Expiry Manager]
        POINT_SERV --> EXPIRY
        EXPIRY --> BATCH[Batch Processor]
        
        EVENT[Event Publisher] --> KAFKA[Kafka]
        POINT_SERV --> EVENT
        CAMPAIGN_SERV --> EVENT
    end
    
    %% 外部システムとの接続
    API --> GATEWAY[APIゲートウェイ]
    EVENT --> SALES[販売管理サービス]
    EVENT --> USER[ユーザー管理サービス]
    EVENT --> NOTIFICATION[通知サービス]
```

**シーケンス図（ポイント付与プロセス）**:

```mermaid
sequenceDiagram
    participant Sales as 販売管理サービス
    participant EventBus as イベントバス
    participant PointAPI as ポイントAPI
    participant PointService as ポイントサービス
    participant RuleService as ルールサービス
    participant CalcEngine as 計算エンジン
    participant DB as データベース
    participant Cache as Redisキャッシュ
    participant User as ユーザー管理サービス
    participant Notification as 通知サービス
    
    %% 注文完了イベントから始まるポイント付与フロー
    Sales->>EventBus: 注文完了イベント発行
    EventBus->>PointAPI: ポイント付与要求
    
    %% ユーザー情報取得
    PointAPI->>PointService: ポイント計算リクエスト
    PointService->>User: ユーザー情報取得
    User-->>PointService: ユーザー情報（会員ランク等）
    
    %% ポイント計算
    PointService->>RuleService: 適用ルール取得
    RuleService->>DB: ルール情報取得
    DB-->>RuleService: ポイントルール
    
    alt ユーザー特典またはキャンペーン
        RuleService->>DB: キャンペーン情報取得
        DB-->>RuleService: 有効なキャンペーン
    end
    
    RuleService-->>PointService: ルール情報
    PointService->>CalcEngine: ポイント計算
    CalcEngine->>CalcEngine: 基本ポイント計算
    CalcEngine->>CalcEngine: 特典ポイント計算
    CalcEngine-->>PointService: 付与ポイント数
    
    %% ポイント記録
    PointService->>DB: ポイントトランザクション記録
    DB-->>PointService: 記録成功
    PointService->>DB: ポイント残高更新
    DB-->>PointService: 更新成功
    
    %% キャッシュ更新
    PointService->>Cache: 最新ポイント残高更新
    Cache-->>PointService: 更新成功
    
    %% ポイント付与通知
    PointService->>EventBus: ポイント付与イベント発行
    EventBus->>Notification: ポイント付与通知
    Notification->>Notification: 通知生成
    Notification->>User: ユーザーへ通知
    
    %% API応答
    PointService-->>PointAPI: ポイント付与完了
    PointAPI-->>EventBus: 処理完了応答
    
    %% 定期処理（別シーケンス）
    Note over PointService, DB: 定期的なポイント有効期限チェック（バッチ処理）
    PointService->>DB: 期限切れポイント検索
    DB-->>PointService: 期限切れポイント
    PointService->>DB: ポイント期限処理
    DB-->>PointService: 処理完了
    PointService->>EventBus: ポイント期限切れイベント
    EventBus->>Notification: 期限切れ通知
```

## 技術スタック

### バックエンド

- **言語**: Java 21 (LTS)
- **フレームワーク**: Spring Boot 3.2
- **テンプレートエンジン**: Thymeleaf（管理画面・メール通知など）
- **ビルドツール**: Gradle 8.5
- **API文書化**: SpringDoc OpenAPI 2.3
- **テスト**: JUnit 5, Mockito, Testcontainers, Gatling
- **データアクセス**: 
  - Spring Data JPA / Hibernate 6.4
  - Spring Data MongoDB 
  - Spring Data Redis
  - QueryDSL（複雑クエリ）
- **メッセージング**: Apache Kafka, Spring Cloud Stream
- **マイクロサービス連携**: 
  - Spring Cloud 2023.0
  - Spring Cloud Gateway
  - Spring Cloud Config
  - Resilience4j（サーキットブレーカー）
- **モニタリング**: 
  - Micrometer
  - Spring Boot Actuator
  - Azure Application Insights

### フロントエンド

- **Webアプリケーション**:
  - フレームワーク: Next.js 14 (React)
  - 言語: TypeScript 5
  - 状態管理: Redux Toolkit / Zustand
  - UIライブラリ: TailwindCSS, Headless UI
  - API連携: GraphQL (Apollo), Axios, React Query
  - テスト: Jest, React Testing Library, Cypress
  - i18n: next-intl（多言語対応）

- **管理画面**:
  - Spring Boot + Thymeleaf
  - Bootstrap 5
  - htmx（部分的な画面更新）
  - Alpine.js（軽量インタラクション）

### データストア

- **リレーショナルDB**: PostgreSQL 16
- **NoSQL DB**: MongoDB 7
- **キャッシュ**: Redis 7.2
- **検索エンジン**: Elasticsearch 8.12
- **ストレージ**: Azure Blob Storage
- **メッセージブローカー**: Apache Kafka 3.6

### インフラストラクチャ

- **開発環境**:
  - Docker 25
  - Docker Compose v2
  - LocalStack（AWSサービスエミュレーション）
  - Testcontainers（統合テスト）

- **本番環境**:
  - Azure Container Apps
  - Azure Container Registry
  - Azure Database for PostgreSQL
  - Azure Cache for Redis
  - Azure Cosmos DB (MongoDB API)
  - Azure Key Vault（シークレット管理）
  - Azure Monitor & Application Insights
  - Azure Front Door & CDN（グローバル配信）

- **CI/CD**:
  - GitHub Actions
  - Terraform（IaC）
  - SonarQube（コード品質）
  - Trivy（コンテナセキュリティスキャン）
- **モニタリング**: Prometheus, Grafana, Azure Monitor
- **ロギング**: ELK Stack, Azure Log Analytics
- **サービスメッシュ**: Istio

### クラウドサービス

- **クラウドプロバイダー**: Microsoft Azure
- **API管理**: Azure API Management
- **AI/ML**: Azure AI Services, Azure OpenAI Service
- **CDN**: Azure CDN
- **ファイアウォール**: Azure Firewall
- **アイデンティティ管理**: Azure AD B2C

## データモデル

### コアエンティティ関係図

```
┌────────────────┐      ┌────────────────┐      ┌────────────────┐
│     User       │      │     Order      │      │    Product     │
├────────────────┤      ├────────────────┤      ├────────────────┤
│ id             │      │ id             │      │ id             │
│ email          │      │ userId         │◄─────┤ name           │
│ password       │      │ status         │      │ description    │
│ firstName      │      │ totalAmount    │      │ categoryId     │
│ lastName       │      │ createdAt      │      │ price          │
│ phoneNumber    │      │ updatedAt      │      │ stockQuantity  │
│ addresses      │      │ paymentId      │      │ attributes     │
│ pointBalance   │      │ couponId       │      │ images         │
│ createdAt      │      └────────┬───────┘      │ createdAt      │
│ updatedAt      │               │              │ updatedAt      │
└────────────────┘               │              └───────┬────────┘
       ▲                         │                      │
       │                         ▼                      │
┌──────┴───────┐      ┌────────────────┐               │
│  UserAddress  │      │   OrderItem    │◄──────────────┘
├───────────────┤      ├────────────────┤
│ id            │      │ id             │
│ userId        │      │ orderId        │
│ addressType   │      │ productId      │
│ recipient     │      │ quantity       │
│ zipCode       │      │ price          │
│ prefecture    │      │ discount       │
│ city          │      │ subtotal       │
│ streetAddress │      └────────────────┘
│ building      │               ▲
│ isDefault     │               │
└───────────────┘      ┌────────┴───────┐      ┌────────────────┐
                       │    Payment     │      │    Coupon      │
                       ├────────────────┤      ├────────────────┤
                       │ id             │      │ id             │
                       │ orderId        │      │ code           │
                       │ amount         │      │ description    │
                       │ method         │      │ discountType   │
                       │ status         │      │ discountValue  │
                       │ transactionId  │      │ minOrderAmount │
                       │ createdAt      │      │ startDate      │
                       └────────────────┘      │ endDate        │
                                               │ usageLimit     │
                                               │ isActive       │
                                               └────────────────┘
```

### マイクロサービス別データモデル詳細

このセクションでは、各マイクロサービスが管理する主要エンティティの詳細な属性とリレーションシップを定義します。

#### ユーザー管理サービス

| エンティティ | 説明 | 主要属性 |
|------------|------|---------|
| User | ユーザー基本情報 | id, email, passwordHash, firstName, lastName, phoneNumber, birthDate, createdAt, updatedAt, lastLoginAt, status |
| Role | ユーザーロール | id, name, description, permissions |
| Permission | 細粒度の権限 | id, name, description, resource, action |
| UserPreference | ユーザー設定 | id, userId, language, currency, notificationPreferences, displayPreferences |
| Address | 配送・請求先住所 | id, userId, addressType(SHIPPING/BILLING), recipient, zipCode, prefecture, city, streetAddress, building, phoneNumber, isDefault |
| UserActivity | ユーザー行動履歴 | id, userId, activityType, timestamp, details, ipAddress, deviceInfo |

#### 在庫管理サービス

| エンティティ | 説明 | 主要属性 |
|------------|------|---------|
| Product | 商品基本情報 | id, sku, name, description, brand, categoryId, price, cost, weight, dimensions, isActive, createdAt, updatedAt |
| Category | 商品カテゴリ | id, name, description, parentId, level, path, imageUrl, isActive |
| Inventory | 在庫情報 | id, productId, stockQuantity, reservedQuantity, availableQuantity, warehouseId, reorderLevel, updatedAt |
| Supplier | 仕入先情報 | id, name, contactPerson, email, phone, address, rating, activeContractId |
| PriceHistory | 価格履歴 | id, productId, price, effectiveFrom, effectiveTo, promotionId |
| ProductAttribute | 商品属性 | id, productId, attributeName, attributeValue, isFilterable, isSortable |
| ProductImage | 商品画像 | id, productId, imageUrl, altText, sortOrder, isPrimary |

#### 販売管理サービス

| エンティティ | 説明 | 主要属性 |
|------------|------|---------|
| Order | 注文情報 | id, userId, orderDate, status, subtotal, tax, shippingCost, discount, totalAmount, couponId, shippingAddressId, billingAddressId, paymentId, notes |
| OrderItem | 注文商品明細 | id, orderId, productId, productSnapshot, quantity, unitPrice, discount, tax, subtotal |
| Shipment | 配送情報 | id, orderId, trackingNumber, carrier, status, shippingDate, estimatedDeliveryDate, actualDeliveryDate |
| Return | 返品情報 | id, orderId, requestDate, status, reason, approvalDate, refundAmount, returnItemsData |
| Invoice | 請求書情報 | id, orderId, invoiceNumber, issuedDate, dueDate, paidDate, amount, status |
| OrderStatus | 注文状態 | id, name, description, sequenceOrder |
| ShipmentStatus | 配送状態 | id, name, description, sequenceOrder |

#### 支払い・カート処理サービス

| エンティティ | 説明 | 主要属性 |
|------------|------|---------|
| Cart | カート情報 | id, userId, sessionId, createdAt, updatedAt, expiredAt, status |
| CartItem | カート内商品 | id, cartId, productId, quantity, addedAt, unitPrice, selectedAttributes |
| Payment | 支払い情報 | id, orderId, amount, currency, method, status, gatewayReference, transactionDate |
| PaymentMethod | 支払い方法 | id, userId, type, provider, accountReference, isDefault, expiryDate, billingAddressId |
| Transaction | 決済トランザクション | id, paymentId, type, amount, status, gatewayResponse, createdAt, updatedAt |
| PaymentStatus | 支払い状態 | id, name, description, isSuccess, isProcessing, isFailed |

#### AIサポートサービス

| エンティティ | 説明 | 主要属性 |
|------------|------|---------|
| UserInteraction | ユーザーとの対話 | id, userId, sessionId, startTime, endTime, interactionType |
| ChatSession | チャットセッション | id, userId, startTime, endTime, sessionSummary, feedbackRating |
| ChatMessage | チャットメッセージ | id, sessionId, timestamp, sender(USER/BOT), content, messageType |
| ProductRecommendation | 商品推奨履歴 | id, userId, productId, recommendationSource, timestamp, wasClicked, confidence |
| SearchQuery | ユーザー検索クエリ | id, userId, query, timestamp, resultCount, filterParameters |
| BehaviorAnalysis | 行動分析データ | id, userId, behaviorType, timestamp, details, sessionId |

#### 認証サービス（OAuth）

| エンティティ | 説明 | 主要属性 |
|------------|------|---------|
| OAuthClient | OAuth認証クライアント | id, clientId, clientSecret, name, description, redirectUris, allowedGrantTypes, scopes |
| OAuthToken | 認証トークン | id, accessToken, refreshToken, clientId, userId, scopes, issuedAt, expiresAt |
| OAuthScope | 権限スコープ | id, name, description, isDefault, required |
| OAuthConsent | ユーザー同意履歴 | id, userId, clientId, scopes, consentedAt, expiresAt |
| MfaMethod | 多要素認証方法 | id, userId, type(SMS/EMAIL/TOTP), status, createdAt, lastUsedAt |
| LoginAttempt | ログイン試行履歴 | id, userId, timestamp, ipAddress, userAgent, isSuccess, failureReason |

#### クーポン提供機能サービス

| エンティティ | 説明 | 主要属性 |
|------------|------|---------|
| Coupon | クーポン情報 | id, code, description, discountType(PERCENTAGE/FIXED), discountValue, minOrderAmount, startDate, endDate, usageLimit, usageCount, isActive |
| CouponType | クーポン種別 | id, name, description, usageLimitationType |
| CouponUsage | クーポン使用履歴 | id, couponId, userId, orderId, usedAt, discountAmount |
| Campaign | キャンペーン情報 | id, name, description, startDate, endDate, status, budget, targetAudience, associatedCoupons |
| Promotion | プロモーション情報 | id, name, description, startDate, endDate, type, conditions, applicationRules |
| CouponRestriction | クーポン制約 | id, couponId, restrictionType(PRODUCT/CATEGORY/USER), restrictionValue, isExclusion |

#### ポイント提供管理機能サービス

| エンティティ | 説明 | 主要属性 |
|------------|------|---------|
| PointAccount | ポイントアカウント | id, userId, balance, lifetimePoints, lastUpdatedAt |
| PointTransaction | ポイント取引履歴 | id, accountId, amount, type(EARN/REDEEM/EXPIRE/ADJUST), sourceType, sourceId, description, transactionDate, expiryDate |
| PointRule | ポイント付与ルール | id, name, description, conversionRate, minimumAmount, applicableProducts, isActive |
| PointExpiry | ポイント有効期限 | id, accountId, amount, earnedDate, expiryDate, status |
| PointCampaign | ポイントキャンペーン | id, name, description, multiplier, startDate, endDate, targetProducts, isActive |
| PointConversionRate | ポイント交換レート | id, fromCurrency, toCurrency, rate, effectiveDate, expiryDate, isDefault |

#### 製品販売Webサイト（フロントエンド）

| エンティティ | 説明 | 主要属性 |
|------------|------|---------|
| Page | ページ設定 | id, title, slug, metaDescription, metaKeywords, contentType, layout, status |
| ContentBlock | コンテンツブロック | id, pageId, type, content, position, visibility, startDate, endDate |
| Navigation | ナビゲーション構造 | id, name, parentId, url, displayText, sortOrder, isActive, icon |
| SEOSetting | SEO設定 | id, pageId, canonicalUrl, robots, structuredData, altLangLinks |
| MediaAsset | メディアアセット | id, type, url, fileSize, dimensions, format, altText, title, uploadedAt |
| Translation | 多言語翻訳 | id, entityType, entityId, language, field, translatedValue |

## API設計

### API設計原則

- RESTful設計原則の遵守
- 一貫したURI構造
- 適切なHTTPメソッドの使用
- 明確なステータスコードの活用
- HATEOAS（Hypermedia as the Engine of Application State）の実装
- バージョニング戦略（URI, ヘッダー, メディアタイプ）
- 統一エラーレスポンス形式

### 共通APIレスポンスフォーマット

```json
{
  "status": "success | error | warning",
  "code": "RESOURCE_CREATED | RESOURCE_UPDATED | RESOURCE_DELETED | VALIDATION_ERROR | ...",
  "message": "人間可読なメッセージ",
  "data": { /* レスポンスデータ */ },
  "meta": {
    "timestamp": "2025-01-01T12:00:00Z",
    "requestId": "req-123456",
    "pagination": {
      "page": 0,
      "size": 20,
      "totalElements": 100,
      "totalPages": 5
    }
  },
  "_links": {
    "self": { "href": "/api/v1/resource/123" },
    "next": { "href": "/api/v1/resources?page=1&size=20" },
    "prev": null
  }
}
```

### 共通エラーレスポンスフォーマット

```json
{
  "status": "error",
  "code": "VALIDATION_ERROR",
  "message": "入力内容に誤りがあります",
  "errors": [
    {
      "field": "email",
      "message": "有効なメールアドレスを入力してください",
      "rejectedValue": "invalid-email"
    },
    {
      "field": "password",
      "message": "パスワードは8文字以上必要です",
      "rejectedValue": "1234"
    }
  ],
  "meta": {
    "timestamp": "2025-01-01T12:00:00Z",
    "requestId": "req-123456"
  }
}
```

### OpenAPI仕様例（ユーザー管理サービス）

```yaml
openapi: 3.0.3
info:
  title: スキーショップユーザー管理API
  description: スキー用品販売サイトのユーザー管理マイクロサービスAPI
  version: 1.0.0
servers:
  - url: https://api.skieshop.com/users
    description: ユーザー管理サービスベースURL
paths:
  /users:
    get:
      summary: ユーザー一覧取得
      description: システム内のユーザー一覧を取得します
      parameters:
        - name: page
          in: query
          description: ページ番号
          schema:
            type: integer
            default: 0
        - name: size
          in: query
          description: ページサイズ
          schema:
            type: integer
            default: 20
      responses:
        '200':
          description: 成功
          content:
            application/json:
              schema:
                type: object
                properties:
                  content:
                    type: array
                    items:
                      $ref: '#/components/schemas/UserSummary'
                  pageable:
                    $ref: '#/components/schemas/PageInfo'
    post:
      summary: 新規ユーザー作成
      description: 新しいユーザーを作成します
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/UserCreationRequest'
      responses:
        '201':
          description: ユーザー作成成功
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/User'
        '400':
          description: 無効なリクエスト
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'

  /users/{id}:
    get:
      summary: ユーザー詳細取得
      description: 指定IDのユーザー詳細を取得します
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
            format: uuid
      responses:
        '200':
          description: 成功
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/User'
        '404':
          description: ユーザーが見つかりません
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
    put:
      summary: ユーザー情報更新
      description: 指定IDのユーザー情報を更新します
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
            format: uuid
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/UserUpdateRequest'
      responses:
        '200':
          description: 更新成功
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/User'
        '400':
          description: 無効なリクエスト
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
        '404':
          description: ユーザーが見つかりません
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
    delete:
      summary: ユーザー削除
      description: 指定IDのユーザーを削除します
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
            format: uuid
      responses:
        '204':
          description: 削除成功
        '404':
          description: ユーザーが見つかりません
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'

components:
  schemas:
    User:
      type: object
      properties:
        id:
          type: string
          format: uuid
        email:
          type: string
          format: email
        firstName:
          type: string
        lastName:
          type: string
        phoneNumber:
          type: string
        addresses:
          type: array
          items:
            $ref: '#/components/schemas/Address'
        pointBalance:
          type: integer
        roles:
          type: array
          items:
            type: string
        createdAt:
          type: string
          format: date-time
        updatedAt:
          type: string
          format: date-time
      required:
        - id
        - email
        - firstName
        - lastName
        - roles
        - createdAt
        - updatedAt
```

### OpenAPI仕様例（在庫管理サービス）

```yaml
openapi: 3.0.3
info:
  title: スキーショップ在庫管理API
  description: スキー用品販売サイトの在庫管理マイクロサービスAPI
  version: 1.0.0
servers:
  - url: https://api.skieshop.com/inventory
    description: 在庫管理サービスベースURL
paths:
  /products:
    get:
      summary: 商品一覧取得
      description: 商品一覧を取得します（フィルタリング・ソート・ページング対応）
      parameters:
        - name: category
          in: query
          description: カテゴリID
          schema:
            type: string
        - name: query
          in: query
          description: 検索キーワード
          schema:
            type: string
        - name: minPrice
          in: query
          description: 最低価格
          schema:
            type: number
        - name: maxPrice
          in: query
          description: 最高価格
          schema:
            type: number
        - name: inStock
          in: query
          description: 在庫あり商品のみ
          schema:
            type: boolean
        - name: sort
          in: query
          description: ソート項目（price, name, popularity）
          schema:
            type: string
            enum: [price_asc, price_desc, name_asc, name_desc, popularity_desc]
        - name: page
          in: query
          description: ページ番号
          schema:
            type: integer
            default: 0
        - name: size
          in: query
          description: ページサイズ
          schema:
            type: integer
            default: 20
      responses:
        '200':
          description: 成功
          content:
            application/json:
              schema:
                type: object
                properties:
                  content:
                    type: array
                    items:
                      $ref: '#/components/schemas/ProductSummary'
                  pageable:
                    $ref: '#/components/schemas/PageInfo'
    post:
      summary: 新規商品登録
      description: 新しい商品を登録します
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/ProductCreationRequest'
      responses:
        '201':
          description: 商品登録成功
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Product'
        '400':
          description: 無効なリクエスト
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'

  /products/{id}/inventory:
    get:
      summary: 商品在庫情報取得
      description: 指定商品の在庫情報を取得します
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
      responses:
        '200':
          description: 成功
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/InventoryInfo'
        '404':
          description: 商品が見つかりません
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
    put:
      summary: 在庫数更新
      description: 指定商品の在庫数を更新します
      parameters:
        - name: id
          in: path
          required: true
          schema:
            type: string
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/InventoryUpdateRequest'
      responses:
        '200':
          description: 更新成功
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/InventoryInfo'
        '400':
          description: 無効なリクエスト
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
```

### GraphQL API例（検索・フィルタリング向け）

```graphql
type Query {
  # 商品検索
  products(
    query: String
    category: ID
    priceRange: PriceRangeInput
    attributes: [AttributeFilterInput!]
    sort: ProductSortInput
    pagination: PaginationInput
  ): ProductConnection!
  
  # 商品詳細
  product(id: ID!): Product
  
  # カテゴリ一覧
  categories(parent: ID): [Category!]!
}

type Product {
  id: ID!
  name: String!
  description: String
  price: Float!
  discount: Float
  finalPrice: Float!
  brand: String
  category: Category!
  images: [ProductImage!]!
  attributes: [ProductAttribute!]!
  inventory: InventoryInfo!
  reviews: ReviewConnection!
  relatedProducts: [Product!]!
}

type ProductConnection {
  edges: [ProductEdge!]!
  pageInfo: PageInfo!
  totalCount: Int!
  facets: [FacetResult!]!
}

type ProductEdge {
  node: Product!
  cursor: String!
}

type PageInfo {
  hasNextPage: Boolean!
  hasPreviousPage: Boolean!
  startCursor: String
  endCursor: String
}

type FacetResult {
  name: String!
  values: [FacetValue!]!
}

type FacetValue {
  value: String!
  count: Int!
  selected: Boolean!
}

input PriceRangeInput {
  min: Float
  max: Float
}

input AttributeFilterInput {
  name: String!
  values: [String!]!
}

input ProductSortInput {
  field: ProductSortField!
  direction: SortDirection!
}

enum ProductSortField {
  PRICE
  NAME
  POPULARITY
  NEWEST
  RATING
}

enum SortDirection {
  ASC
  DESC
}

input PaginationInput {
  first: Int
  after: String
  last: Int
  before: String
}
```

### API間連携パターン

- **同期通信**:
  - REST API (JSON/HTTP)
  - GraphQL (クライアント用、複合クエリ)
  - gRPC (内部サービス間高速通信)

- **非同期通信**:
  - イベントストリーミング (Apache Kafka)
  - メッセージキュー (RabbitMQ)
  - Webhook (外部システム連携)

### イベントペイロード例（JSON）

```json
{
  "eventId": "e37d0b80-c74a-4da5-b4e5-1b0db6c3f62a",
  "eventType": "OrderCreated",
  "timestamp": "2025-06-19T13:45:30.123Z",
  "version": "1.0",
  "payload": {
    "orderId": "ord-12345",
    "userId": "usr-67890",
    "items": [
      {
        "productId": "prod-12345",
        "quantity": 1,
        "price": 39800
      }
    ],
    "totalAmount": 39800,
    "status": "CREATED"
  }
}
```

### gRPC サービス定義例（在庫確認サービス）

```protobuf
syntax = "proto3";

package com.skieshop.inventory;

service InventoryService {
  // 単一商品の在庫状況確認
  rpc CheckStock (CheckStockRequest) returns (StockResponse);
  
  // 複数商品の在庫状況一括確認
  rpc CheckStockBatch (CheckStockBatchRequest) returns (StockBatchResponse);
  
  // 在庫状況のリアルタイム更新をサブスクライブ
  rpc SubscribeStockUpdates (StockSubscriptionRequest) returns (stream StockUpdate);
}

message CheckStockRequest {
  string product_id = 1;
  int32 required_quantity = 2;
}

message StockResponse {
  string product_id = 1;
  int32 available_quantity = 2;
  bool is_in_stock = 3;
  bool can_fulfill_request = 4;
  string estimated_restock_date = 5; // ISO8601形式、在庫切れの場合のみ
}

message CheckStockBatchRequest {
  repeated CheckStockRequest items = 1;
}

message StockBatchResponse {
  repeated StockResponse items = 1;
  bool all_items_available = 2;
}

message StockSubscriptionRequest {
  repeated string product_ids = 1;
  bool include_price_updates = 2;
}

message StockUpdate {
  string product_id = 1;
  int32 available_quantity = 2;
  float current_price = 3;
  string update_timestamp = 4;
  StockUpdateType update_type = 5;
}

enum StockUpdateType {
  QUANTITY_CHANGED = 0;
  PRICE_CHANGED = 1;
  PRODUCT_UNAVAILABLE = 2;
  PRODUCT_AVAILABLE = 3;
}
```

## 認証・認可

### 認証アーキテクチャ

このシステムでは、OAuth 2.0とOpenID Connectプロトコルをベースとした包括的な認証・認可フレームワークを実装します。Azure AD B2CとKeycloakを組み合わせ、柔軟で安全なIDプラットフォームを構築します。

#### 認証コンポーネント構成

```mermaid
graph TB
    User[ユーザー] --> WebApp[Webアプリケーション]
    User --> MobileApp[モバイルアプリ]
    
    WebApp --> APIGW[APIゲートウェイ]
    MobileApp --> APIGW
    
    subgraph "認証基盤"
        APIGW --> |認証リクエスト| AuthService[認証サービス]
        AuthService <--> |OIDC/OAuth2.0| IdP[ID Provider - Keycloak/Azure AD B2C]
        AuthService --> TokenService[トークン管理サービス]
        IdP <--> UserDB[(ユーザーDB)]
    end
    
    APIGW --> |有効なトークン| MicroService1[マイクロサービス1]
    APIGW --> |有効なトークン| MicroService2[マイクロサービス2]
    
    subgraph "外部認証"
        IdP <--> |OIDC/OAuth| Google[Google]
        IdP <--> |OIDC/OAuth| Facebook[Facebook]
        IdP <--> |OIDC/OAuth| Apple[Apple]
        IdP <--> |OIDC/OAuth| LINE[LINE]
    end
```

### 認証フロー

#### 1. OAuth 2.0/OpenID Connect認証フロー

- **認可コードフロー（Webアプリケーション）**:
  1. ユーザーがログインを要求
  2. アプリケーションが認可エンドポイントにユーザーをリダイレクト
  3. ユーザーが認証し、同意を提供
  4. 認可サーバーが認可コードを発行
  5. アプリケーションがバックチャネルで認可コードをトークンと交換
  6. アプリケーションがIDトークンとアクセストークンを取得

- **PKCE付き認可コードフロー（モバイルアプリ）**:
  - 認可コードフローに、Code Verifierとcode_challengeによる保護を追加
  - クライアントシークレットをモバイルアプリに保存する必要がない

- **クライアントクレデンシャルフロー（サービス間通信）**:
  - クライアントIDとシークレットを使用した直接トークン取得
  - ユーザーコンテキストなしでのシステム間API呼び出しに使用

- **リソースオーナーパスワードクレデンシャルフロー（レガシーシステム連携）**:
  - ユーザー名とパスワードを直接使用したトークン取得
  - レガシーシステム統合のみに限定的に使用

#### 2. ソーシャルログイン統合

- Google
- Facebook
- Apple
- LINE
- Twitter

ソーシャルログインは、標準のOpenID Connect/OAuthフローを使用して実装され、IDプロバイダー（Azure AD B2C/Keycloak）を通じて統合されます。各ソーシャルIDは内部ユーザープロファイルにマッピングされます。

#### 3. 多要素認証（MFA）

- **SMSワンタイムパスワード**:
  - ユーザーの電話番号に送信されるワンタイムコード
  - Twilioなどのサービスと統合

- **Eメール認証コード**:
  - ユーザーのメールアドレスに送信される一時コード
  - SendGridと統合

- **TOTP（Time-based One-Time Password）**:
  - Google Authenticator、Microsoft Authenticatorなどの認証アプリ対応
  - RFC 6238準拠

- **プッシュ通知**:
  - モバイルアプリへのプッシュ通知による承認
  - Firebase Cloud Messaging (FCM)を使用

### 認証情報フロー図

```mermaid
sequenceDiagram
    participant User as ユーザー
    participant WebApp as Webアプリケーション
    participant APIGW as APIゲートウェイ
    participant Auth as 認証サービス
    participant IdP as IDプロバイダ
    participant MS as マイクロサービス
    
    User->>WebApp: 1. ログインリクエスト
    WebApp->>IdP: 2. 認証リクエスト
    IdP->>User: 3. ログイン画面表示
    User->>IdP: 4. 認証情報入力
    
    alt MFA有効
        IdP->>User: 5a. 第2要素認証リクエスト
        User->>IdP: 5b. 第2要素認証応答
    end
    
    IdP->>WebApp: 6. 認可コード返却
    WebApp->>Auth: 7. 認可コードをトークンと交換
    Auth->>WebApp: 8. IDトークン・アクセストークン発行
    
    User->>WebApp: 9. 保護リソースへのアクセス
    WebApp->>APIGW: 10. APIリクエスト + アクセストークン
    APIGW->>APIGW: 11. トークン検証
    APIGW->>MS: 12. 検証済みリクエスト転送
    MS->>APIGW: 13. レスポンス
    APIGW->>WebApp: 14. APIレスポンス
    WebApp->>User: 15. 結果表示
```

### 認可モデル

#### ロールベースアクセス制御（RBAC）

| ロール名 | 説明 | 主な権限 |
|---------|------|---------|
| Customer | 一般顧客 | 商品閲覧、注文作成、自身のプロファイル管理 |
| PremiumCustomer | プレミアム会員 | 一般顧客の権限 + 特別オファー閲覧、早期アクセス |
| StoreAdmin | 店舗管理者 | 注文管理、顧客対応、基本的な商品管理 |
| InventoryManager | 在庫管理者 | 商品・在庫管理、価格設定、仕入れ管理 |
| SalesManager | 販売管理者 | 売上レポート閲覧、キャンペーン管理、割引設定 |
| SystemAdmin | システム管理者 | 全機能へのアクセス、ユーザー管理、システム設定 |

#### 属性ベースアクセス制御（ABAC）

RBAC（ロールベース）に加えて、以下の属性に基づいたきめ細かいアクセス制御を実装します：

1. **ユーザー属性**:
   - 会員ステータス（通常/シルバー/ゴールド/プラチナ）
   - 購入履歴（総購入金額、購入頻度）
   - 地域（都道府県、国）
   - 年齢層

2. **リソース属性**:
   - 商品カテゴリ（一般商品/限定商品/プレミアム商品）
   - コンテンツ種別（一般/会員限定/プロモーション）
   - データ機密レベル

3. **環境属性**:
   - アクセス時間帯
   - アクセス元IPアドレス
   - デバイスタイプ

### JWT構造

#### アクセストークン

```json
{
  "alg": "RS256",
  "typ": "JWT",
  "kid": "key-id-1"
}
{
  "iss": "https://auth.skieshop.com",
  "sub": "user-123456",
  "aud": "ski-shop-api",
  "exp": 1609459200,
  "iat": 1609455600,
  "auth_time": 1609455600,
  "azp": "web-client-123",
  "scope": "openid profile email api:read api:write",
  "roles": ["Customer", "PremiumCustomer"],
  "permissions": ["products:read", "orders:create", "profile:write"],
  "amr": ["pwd", "mfa"],
  "jti": "abc-123-xyz-789"
}
```

#### IDトークン

```json
{
  "alg": "RS256",
  "typ": "JWT",
  "kid": "key-id-1"
}
{
  "iss": "https://auth.skieshop.com",
  "sub": "user-123456",
  "aud": "web-client-123",
  "exp": 1609459200,
  "iat": 1609455600,
  "auth_time": 1609455600,
  "nonce": "n-0S6_WzA2M",
  "name": "山田 太郎",
  "given_name": "太郎",
  "family_name": "山田",
  "email": "taro.yamada@example.com",
  "email_verified": true,
  "picture": "https://profile.skieshop.com/photos/user-123456.jpg",
  "locale": "ja-JP",
  "preferred_username": "taro.yamada",
  "amr": ["pwd", "mfa"]
}
```

### セキュリティ実装

#### トランスポート層セキュリティ

- **TLS 1.3の強制**:
  - 全通信での最新TLSプロトコル使用
  - 古いTLSバージョン（1.0/1.1/1.2）の無効化

- **適切な暗号スイートの設定**:
  ```
  TLS_AES_256_GCM_SHA384
  TLS_AES_128_GCM_SHA256
  TLS_CHACHA20_POLY1305_SHA256
  ```

- **HSTS（HTTP Strict Transport Security）の実装**:
  - `Strict-Transport-Security: max-age=31536000; includeSubDomains; preload`
  - HTTPS強制とダウングレード攻撃防止

#### APIセキュリティ

- **JWT検証**:
  - 署名検証（RS256アルゴリズム）
  - 有効期限チェック
  - 発行者・対象者検証
  - スコープと権限の検証

- **トークンの有効期限と更新メカニズム**:
  - アクセストークン: 短期（1時間）
  - リフレッシュトークン: 長期（14日）
  - トークンローテーション

- **CSRFトークン保護**:
  - ステートフルなセッションでのCSRFトークン要求
  - Double Submit Cookie検証

- **レート制限とスロットリング**:
  - IPアドレスベース: 1分あたり60リクエスト
  - APIキーベース: 1分あたり300リクエスト
  - ユーザーベース: 1分あたり120リクエスト

#### データ保護

- **個人識別情報（PII）の暗号化**:
  - 保存時: AES-256-GCM
  - 転送時: TLS 1.3
  - データベース列レベルの暗号化

- **トークン保護**:
  - ブラウザ: HttpOnly & Secure Cookies
  - モバイル: セキュアストレージ
  - CSRFトークンの実装

- **機密データのマスキング**:
  - クレジットカード: 最初の6桁と最後の4桁のみ表示
  - 電話番号: 一部をアスタリスクに置換
  - ログからの機密情報除外

## インフラストラクチャ設計

### クラウドアーキテクチャ

Microsoft Azureをメインクラウドプロバイダーとして使用し、特にAzure Container Appsを中心に構成します：

- **コンピューティング**:
  - Azure Container Apps: 各マイクロサービスのコンテナホスティング
  - Azure Container Registry: コンテナイメージのプライベートレジストリ
  - Azure Functions: イベント駆動型処理、バッチ処理、スケジュールタスク

- **ネットワーキング**:
  - Azure Virtual Network: プライベートネットワーク構成
  - Azure Front Door + CDN: グローバル配信とキャッシング
  - Azure Application Gateway: Webアプリケーションファイアウォール、TLS終端
  - Azure API Management: API公開と管理

- **データストア**:
  - Azure Database for PostgreSQL: リレーショナルデータ
  - Azure Cosmos DB (MongoDB API): 商品カタログ、非構造化データ
  - Azure Cache for Redis: セッション管理、キャッシュ
  - Azure Storage: 画像、静的コンテンツ、バックアップ

- **セキュリティ**:
  - Azure AD B2C: 顧客ID管理
  - Azure Key Vault: シークレット管理
  - Azure Security Center: セキュリティ監視と対応
  - Azure Defender for Container: コンテナセキュリティ

- **監視と運用**:
  - Azure Monitor: アプリケーション監視
  - Azure Log Analytics: ログ集約と分析
  - Azure Application Insights: アプリケーションパフォーマンス監視
  - Azure Automation: 運用タスクの自動化

### Azure Container Appsデプロイメント構成

```mermaid
graph TB
    subgraph "Azure"
        subgraph "フロントエンド"
            AFD[Azure Front Door/CDN]
            WAF[Application Gateway/WAF]
        end
        
        subgraph "API管理"
            APIM[Azure API Management]
        end
        
        subgraph "Azure Container Apps環境"
            CAENV[Container Apps環境]
            
            subgraph "マイクロサービス"
                CAAPI[API Gateway App]
                CAWEB[Web Frontend App]
                CAINV[Inventory Service App]
                CAUSER[User Management App]
                CASALES[Sales Management App]
                CAAI[AI Support App]
                CAPAY[Payment App]
                CACOUPON[Coupon App]
                CAPOINT[Point App]
                CAAUTH[OAuth Service App]
            end
        end
        
        subgraph "マネージドサービス"
            PSQL[Azure DB for PostgreSQL]
            COSMOS[Azure Cosmos DB]
            REDIS[Azure Cache for Redis]
            BLOB[Azure Blob Storage]
            EVENTHUB[Azure Event Hubs]
        end
        
        subgraph "認証・セキュリティ"
            ADB2C[Azure AD B2C]
            KV[Key Vault]
        end
        
        subgraph "監視"
            MONITOR[Azure Monitor]
            INSIGHTS[Application Insights]
            LOGS[Log Analytics]
        end
    end
    
    %% 接続
    AFD --> WAF
    WAF --> APIM
    APIM --> CAAPI
    
    %% Container Apps接続
    CAAPI --> CAWEB
    CAAPI --> CAINV
    CAAPI --> CAUSER
    CAAPI --> CASALES
    CAAPI --> CAAI
    CAAPI --> CAPAY
    CAAPI --> CACOUPON
    CAAPI --> CAPOINT
    CAAPI --> CAAUTH
    
    %% マネージドサービス接続
    CAINV --> COSMOS
    CAINV --> BLOB
    CAUSER --> PSQL
    CASALES --> PSQL
    CAPAY --> PSQL
    CACOUPON --> REDIS
    CAPOINT --> REDIS
    CAAI --> COSMOS
    CAWEB --> BLOB
    
    %% イベントベース接続
    CAINV -.-> EVENTHUB
    CAUSER -.-> EVENTHUB
    CASALES -.-> EVENTHUB
    CAPAY -.-> EVENTHUB
    CACOUPON -.-> EVENTHUB
    CAPOINT -.-> EVENTHUB
    
    %% 認証接続
    CAAPI --> ADB2C
    CAAUTH --> ADB2C
    CAUSER --> KV
    CAPAY --> KV
    
    %% 監視接続
    CAAPI -.-> INSIGHTS
    CAWEB -.-> INSIGHTS
    CAINV -.-> INSIGHTS
    CAUSER -.-> INSIGHTS
    CASALES -.-> INSIGHTS
    CAAI -.-> INSIGHTS
    CAPAY -.-> INSIGHTS
    CACOUPON -.-> INSIGHTS
    CAPOINT -.-> INSIGHTS
    CAAUTH -.-> INSIGHTS
    
    INSIGHTS --> MONITOR
    INSIGHTS --> LOGS
```

### ローカル開発環境（Docker Compose）

ローカル開発では、Docker Composeを使用して各マイクロサービスとその依存関係を簡単に起動できるようにします。

```mermaid
graph TB
    subgraph "Docker Compose 環境"
        subgraph "フロントエンドサービス"
            NGINX[NGINX]
            WEBCLIENT[Next.js Webフロントエンド]
            ADMIN[管理画面アプリ]
        end
        
        subgraph "バックエンドサービス"
            APIGATEWAY[API Gateway]
            USERSERVICE[ユーザー管理サービス]
            INVENTORYSERVICE[在庫管理サービス]
            SALESSERVICE[販売管理サービス]
            AISERVICE[AIサポートサービス]
            PAYMENTSERVICE[支払い・カートサービス]
            COUPONSERVICE[クーポンサービス]
            POINTSERVICE[ポイントサービス]
            AUTHSERVICE[認証サービス]
        end
        
        subgraph "データベース"
            POSTGRES[PostgreSQL]
            MONGODB[MongoDB]
            REDISDB[Redis]
            ELASTICSEARCH[Elasticsearch]
        end
        
        subgraph "インフラサービス"
            KAFKA[Kafka & Zookeeper]
            SCHEMAREGISTRY[Schema Registry]
            KEYCLOAK[Keycloak]
            MAILHOG[MailHog]
            MINIO[MinIO]
        end
        
        subgraph "モニタリング"
            PROMETHEUS[Prometheus]
            GRAFANA[Grafana]
            ZIPKIN[Zipkin]
        end
    end
    
    %% 接続
    NGINX --> WEBCLIENT
    NGINX --> ADMIN
    NGINX --> APIGATEWAY
    
    APIGATEWAY --> USERSERVICE
    APIGATEWAY --> INVENTORYSERVICE
    APIGATEWAY --> SALESSERVICE
    APIGATEWAY --> AISERVICE
    APIGATEWAY --> PAYMENTSERVICE
    APIGATEWAY --> COUPONSERVICE
    APIGATEWAY --> POINTSERVICE
    APIGATEWAY --> AUTHSERVICE
    
    USERSERVICE --> POSTGRES
    SALESSERVICE --> POSTGRES
    PAYMENTSERVICE --> POSTGRES
    
    INVENTORYSERVICE --> MONGODB
    AISERVICE --> MONGODB
    
    COUPONSERVICE --> REDISDB
    POINTSERVICE --> REDISDB
    
    INVENTORYSERVICE --> ELASTICSEARCH
    
    USERSERVICE -.-> KAFKA
    INVENTORYSERVICE -.-> KAFKA
    SALESSERVICE -.-> KAFKA
    PAYMENTSERVICE -.-> KAFKA
    COUPONSERVICE -.-> KAFKA
    POINTSERVICE -.-> KAFKA
    
    AUTHSERVICE --> KEYCLOAK
    
    USERSERVICE --> MAILHOG
    SALESSERVICE --> MAILHOG
    
    INVENTORYSERVICE --> MINIO
    AISERVICE --> MINIO
    
    USERSERVICE -.-> PROMETHEUS
    INVENTORYSERVICE -.-> PROMETHEUS
    SALESSERVICE -.-> PROMETHEUS
    AISERVICE -.-> PROMETHEUS
    PAYMENTSERVICE -.-> PROMETHEUS
    COUPONSERVICE -.-> PROMETHEUS
    POINTSERVICE -.-> PROMETHEUS
    AUTHSERVICE -.-> PROMETHEUS
    
    PROMETHEUS --> GRAFANA
    
    USERSERVICE -.-> ZIPKIN
    INVENTORYSERVICE -.-> ZIPKIN
    SALESSERVICE -.-> ZIPKIN
    AISERVICE -.-> ZIPKIN
    PAYMENTSERVICE -.-> ZIPKIN
    COUPONSERVICE -.-> ZIPKIN
    POINTSERVICE -.-> ZIPKIN
    AUTHSERVICE -.-> ZIPKIN
```

### サンプルDocker Compose設定

主要なサービスの構成を示す`docker-compose.yml`ファイルの例:

```yaml
version: '3.8'

services:
  # インフラサービス
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_USER: skieshop
      POSTGRES_PASSWORD: skieshop
      POSTGRES_DB: skieshop
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data
      - ./docker/postgres/init-scripts:/docker-entrypoint-initdb.d
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U skieshop"]
      interval: 10s
      timeout: 5s
      retries: 5

  mongodb:
    image: mongo:7
    environment:
      MONGO_INITDB_ROOT_USERNAME: skieshop
      MONGO_INITDB_ROOT_PASSWORD: skieshop
    ports:
      - "27017:27017"
    volumes:
      - mongo-data:/data/db
      - ./docker/mongo/init-scripts:/docker-entrypoint-initdb.d

  redis:
    image: redis:7.2-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data
    command: redis-server --appendonly yes

  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
    ports:
      - "2181:2181"

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
      KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"

  # マイクロサービス
  api-gateway:
    build:
      context: ./api-gateway
      dockerfile: Dockerfile.dev
    ports:
      - "8080:8080"
    depends_on:
      - auth-service
    environment:
      SPRING_PROFILES_ACTIVE: dev
      SERVER_PORT: 8080
      SPRING_CLOUD_GATEWAY_ROUTES_0_URI: http://user-service:8081
      SPRING_CLOUD_GATEWAY_ROUTES_0_PREDICATES_0: Path=/api/users/**
      # 他のルーティング設定...

  user-service:
    build:
      context: ./user-service
      dockerfile: Dockerfile.dev
    ports:
      - "8081:8081"
    depends_on:
      - postgres
      - kafka
    environment:
      SPRING_PROFILES_ACTIVE: dev
      SERVER_PORT: 8081
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/skieshop_users
      SPRING_DATASOURCE_USERNAME: skieshop
      SPRING_DATASOURCE_PASSWORD: skieshop
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:29092
      # 他の環境変数...

  inventory-service:
    build:
      context: ./inventory-service
      dockerfile: Dockerfile.dev
    ports:
      - "8082:8082"
    depends_on:
      - mongodb
      - kafka
    environment:
      SPRING_PROFILES_ACTIVE: dev
      SERVER_PORT: 8082
      SPRING_DATA_MONGODB_URI: mongodb://skieshop:skieshop@mongodb:27017/inventory
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:29092
      # 他の環境変数...

  # フロントエンド
  web-frontend:
    build:
      context: ./web-frontend
      dockerfile: Dockerfile.dev
    ports:
      - "3000:3000"
    volumes:
      - ./web-frontend:/app
      - /app/node_modules
    environment:
      NODE_ENV: development
      NEXT_PUBLIC_API_URL: http://localhost:8080/api

  # モニタリングツール
  prometheus:
    image: prom/prometheus:v2.45.0
    ports:
      - "9090:9090"
    volumes:
      - ./docker/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml
      - prometheus-data:/prometheus

  grafana:
    image: grafana/grafana:10.2.0
    ports:
      - "3100:3000"
    environment:
      - GF_SECURITY_ADMIN_USER=admin
      - GF_SECURITY_ADMIN_PASSWORD=admin
    volumes:
      - ./docker/grafana/provisioning:/etc/grafana/provisioning
      - grafana-data:/var/lib/grafana
    depends_on:
      - prometheus

volumes:
  postgres-data:
  mongo-data:
  redis-data:
  prometheus-data:
  grafana-data:
```

### 環境構成

- **開発環境**:
  - Docker Compose（上記構成）
  - ローカルJDK 21
  - ローカルIDEと開発ツール
  - モックサービス

- **テスト環境**:
  - Azure Container Apps（小規模構成）
  - Azure Database for PostgreSQL
  - Azure Cache for Redis
  - Azure Cosmos DB (MongoDB API)
  - テスト用データセット
  - 自動テスト統合

- **ステージング環境**:
  - 本番環境と同様の構成（小規模）
  - 本番データのサブセットまたは匿名化データ
  - 本番と同一のネットワークポリシー
  - 手動およびカナリアテスト

- **本番環境**:
  - Azure Container Apps（自動スケーリング設定）
  - リージョンレプリケーション
  - マネージドサービス優先
  - 本番データと完全なセキュリティ対策

### Azure Container Appsリソース設定

各マイクロサービスのコンテナアプリ設定例:

```yaml
name: inventory-service
resourceGroup: ski-shop-prod
location: japaneast
environmentId: /subscriptions/your-subscription-id/resourceGroups/ski-shop-prod/providers/Microsoft.App/managedEnvironments/ski-shop-env
configuration:
  activeRevisionsMode: Multiple
  ingress:
    external: true
    targetPort: 8080
    transport: http
    corsPolicy:
      allowedOrigins: ["https://www.skieshop.com"]
      allowedMethods: ["GET", "POST", "PUT", "DELETE", "OPTIONS"]
      allowedHeaders: ["*"]
      maxAge: 3600
  dapr:
    enabled: true
    appId: inventory-service
    appPort: 8080
  registries:
    - server: skieshopacr.azurecr.io
      identity: system
  secrets:
    - name: mongodb-connection-string
      keyVaultUrl: https://ski-shop-kv.vault.azure.net/secrets/mongodb-connection-string
  
template:
  containers:
    - image: skieshopacr.azurecr.io/inventory-service:latest
      name: inventory-service
      env:
        - name: SPRING_PROFILES_ACTIVE
          value: prod
        - name: APPLICATIONINSIGHTS_CONNECTION_STRING
          value: InstrumentationKey=your-instrumentation-key
        - name: SPRING_DATA_MONGODB_URI
          secretRef: mongodb-connection-string
      resources:
        cpu: 1.0
        memory: 2Gi
      probes:
        - type: liveness
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        - type: readiness
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 15
          periodSeconds: 5
  scale:
    minReplicas: 1
    maxReplicas: 10
    rules:
      - name: http-scale-rule
        http:
          metadata:
            concurrentRequests: "100"
      - name: cpu-scale-rule
        custom:
          type: cpu
          metadata:
            type: Utilization
            value: "70"
```

### ディザスタリカバリ戦略

1. **データバックアップ**:
   - フルバックアップ: 日次
   - 増分バックアップ: 時間単位
   - Point-in-Timeリカバリー対応

2. **マルチリージョン戦略**:
   - アクティブ-パッシブ構成
   - 地理的に分散したリージョン（東日本・西日本）
   - データレプリケーション

3. **リカバリー手順**:
   - 自動フェイルオーバー
   - 手動リージョン切り替えプロセス
   - 定期的なDRテスト（四半期ごと）

4. **RPO/RTO目標**:
   - RPO（目標復旧時点）: 1時間
   - RTO（目標復旧時間）: 4時間

## 非機能要件

### パフォーマンス要件

- **レスポンスタイム**:
  - APIエンドポイント: 95%のリクエストが300ms以内
  - ページロード時間: 95%のページが2秒以内
  - 画像読み込み: 1秒以内
  - 検索結果表示: 500ms以内

- **スループット**:
  - ピーク時: 1000 TPS（トランザクション/秒）
  - 通常時: 200 TPS
  - バッチ処理: 1時間あたり10万レコード

- **キャパシティ**:
  - 同時ユーザー: 10,000人
  - 製品データ: 10万SKU
  - 注文データ: 日間5,000件
  - メディアストレージ: 初期5TB、年間成長2TB

### スケーラビリティ要件

- **水平スケーリング**:
  - CPU使用率70%でのオートスケーリング
  - ゼロダウンタイムスケーリング
  - インスタンス起動時間30秒以内

- **データスケーリング**:
  - シャーディング対応データモデル
  - 効率的なインデックス設計
  - コールドデータのアーカイブ戦略

### 可用性要件

- **サービスレベル目標（SLO）**:
  - Webサイト: 99.95%（月間21.6分のダウンタイム許容）
  - API: 99.99%（月間4.3分のダウンタイム許容）
  - バックオフィス機能: 99.9%（月間43.2分のダウンタイム許容）

- **障害対策**:
  - マルチAZ配置
  - サーキットブレーカー実装
  - 自動フェイルオーバー
  - グレースフルデグラデーション

- **バックアップ/リカバリ**:
  - フルバックアップ: 日次
  - 増分バックアップ: 時間単位
  - RPO（目標復旧時点）: 1時間
  - RTO（目標復旧時間）: 4時間

### セキュリティ要件

- **コンプライアンス**:
  - PCI DSS（決済カード業界データセキュリティ基準）
  - GDPR（EU一般データ保護規則）
  - 日本の個人情報保護法

- **データ保護**:
  - 保存データの暗号化（AES-256）
  - 転送データの暗号化（TLS 1.3）
  - 個人情報の仮名化または匿名化
  - データアクセス監査

- **脆弱性管理**:
  - 定期的な脆弱性スキャン（週次）
  - ペネトレーションテスト（四半期ごと）
  - 依存関係の脆弱性監視（OWASP依存関係チェック）
  - セキュリティパッチ適用SLA: 重大な脆弱性は24時間以内

### 運用性要件

- **監視**:
  - リアルタイムパフォーマンスダッシュボード
  - カスタムアラートと通知
  - SLO/SLAモニタリング
  - ユーザー体験監視

- **ロギング**:
  - 構造化ログ（JSON形式）
  - 集中ログ管理
  - ログ保持: 運用ログ30日、セキュリティログ1年
  - ログデータ匿名化

- **障害管理**:
  - インシデント対応プロセス
  - 自動アラート
  - エスカレーションパス
  - 障害分析と再発防止

## 開発・運用プロセス

### 開発ライフサイクル

- **計画・設計**:
  - 要件分析とユーザーストーリー作成
  - アーキテクチャ設計レビュー
  - 技術的な検証（PoC）
  - タスク分解とバックログ作成

- **開発**:
  - トランクベース開発
  - 継続的インテグレーション
  - ペアプログラミング/モブプログラミング
  - コードレビュー

- **テスト**:
  - 自動単体テスト（JUnit, Jest）
  - 統合テスト（Testcontainers, Spring Test）
  - コントラクトテスト（Spring Cloud Contract）
  - E2Eテスト（Cypress, Selenium）
  - パフォーマンステスト（Gatling, JMeter）

- **デプロイ**:
  - 継続的デリバリー
  - 自動化されたカナリアリリース
  - ブルー/グリーンデプロイメント
  - フィーチャーフラグによる機能リリース

### CI/CDパイプライン

```
┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐
│         │    │         │    │         │    │         │    │         │
│  コード  │───►│  ビルド  │───►│  テスト  │───►│ 品質検査 │───►│イメージ作成│
│ コミット │    │         │    │         │    │         │    │         │
└─────────┘    └─────────┘    └─────────┘    └─────────┘    └────┬────┘
                                                                  │
┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐         │
│         │    │         │    │         │    │         │         │
│  本番    │◄───│ステージング│◄───│  テスト  │◄───│  開発   │◄────────┘
│ デプロイ │    │ デプロイ │    │ デプロイ │    │ デプロイ │
└─────────┘    └─────────┘    └─────────┘    └─────────┘
```

### 運用プロセス

- **リリース管理**:
  - リリースカレンダー
  - 変更管理プロセス
  - リリースノート自動生成
  - ロールバック手順

- **インシデント管理**:
  - インシデント検出と分類
  - エスカレーションプロセス
  - 解決と根本原因分析
  - ポストモーテムとナレッジベース更新

- **パフォーマンス管理**:
  - 定期的なパフォーマンステスト
  - ボトルネック分析
  - 最適化サイクル
  - 容量計画

- **変更管理**:
  - リリース計画
  - リスク評価
  - 変更承認プロセス
  - 変更適用とモニタリング

## 移行戦略

### フェーズド実装アプローチ

マイクロサービスアーキテクチャへの移行は、以下の4つのフェーズで実施します。この段階的アプローチにより、リスクを最小化しながら継続的に価値を提供することが可能になります。

#### フェーズ1: 基盤構築（3ヶ月）

- クラウドインフラストラクチャセットアップ（Azure環境構築）
- CI/CDパイプライン確立（GitHub Actions/Azure DevOps）
- コアマイクロサービスの骨格実装（API定義、DB設計）
- 認証基盤の構築（Azure AD B2C、Keycloak）
- 開発環境と基本テスト環境の整備
- 技術検証（PoC）の実施

#### フェーズ2: コア機能開発（4ヶ月）

- 製品カタログと在庫管理サービスの実装
- ユーザー管理とプロファイルサービスの実装
- 検索機能とWeb UIの基本実装
- 注文処理の基本フローの実装
- APIゲートウェイの実装と統合
- 基本的な監視とロギング機能の実装

#### フェーズ3: 拡張機能（3ヶ月）

- 支払い処理の完全統合
- ポイントシステム実装
- クーポン機能実装
- AIレコメンデーション機能実装
- 多言語対応の完成
- 高度な検索とフィルタリング機能

#### フェーズ4: 最適化とスケーリング（2ヶ月）

- パフォーマンス最適化
- セキュリティ強化とペネトレーションテスト
- 高可用性構成の完成
- 本番環境の完全スケーリング
- 運用ツールとダッシュボードの整備
- マルチリージョン対応

### ストラングラーフィグパターンの適用

既存システムが存在する場合、「ストラングラーフィグパターン」を使用して段階的に機能をマイクロサービスに移行します。

```mermaid
graph TB
    subgraph "Phase 1"
        M1[既存モノリス] --- A1[API Gateway]
        A1 --> E1[既存機能]
    end
    
    subgraph "Phase 2"
        M2[既存モノリス] --- A2[API Gateway]
        A2 --> E2[既存機能]
        A2 --> N1[新マイクロサービス: ユーザー管理]
    end
    
    subgraph "Phase 3"
        M3[既存モノリス] --- A3[API Gateway]
        A3 --> E3[既存機能の一部]
        A3 --> N2[マイクロサービス: ユーザー管理]
        A3 --> N3[マイクロサービス: 在庫管理]
        A3 --> N4[マイクロサービス: 注文管理]
    end
    
    subgraph "Phase 4"
        A4[API Gateway]
        A4 --> N5[マイクロサービス: ユーザー管理]
        A4 --> N6[マイクロサービス: 在庫管理]
        A4 --> N7[マイクロサービス: 注文管理]
        A4 --> N8[マイクロサービス: 支払い]
        A4 --> N9[マイクロサービス: クーポン]
        A4 --> N10[マイクロサービス: ポイント]
    end
    
    Phase1 --> Phase2
    Phase2 --> Phase3
    Phase3 --> Phase4
```

### データ移行戦略

1. **データ分析と設計**:
   - 既存データのスキーマと品質の分析
   - 新システムのデータモデル設計
   - マッピングルールの定義

2. **段階的データ移行**:
   - 初期ロード: 基本的なマスターデータの移行
   - 増分ロード: トランザクションデータの段階的移行
   - 双方向同期: 移行期間中の一時的なデータ同期メカニズム

3. **データ整合性確保**:
   - チェックサム検証
   - サンプリング監査
   - データ検証レポート

4. **フォールバック計画**:
   - ポイントインタイムリカバリー
   - ロールバックシナリオのテスト
   - 緊急時の手続きの確立

### 移行検証戦略

1. **機能同等性検証**:
   - 既存システムと新システムの並行実行
   - 同一インプットに対する出力比較
   - 自動化された比較テスト

2. **パフォーマンステスト**:
   - 負荷テスト（想定ピークの2倍の負荷）
   - 持続可能性テスト（24時間連続運用）
   - スケーラビリティテスト

3. **カナリアデプロイメント**:
   - トラフィックの段階的シフト（5% → 20% → 50% → 100%）
   - リアルタイムモニタリングと自動ロールバック
   - ユーザーフィードバックの収集

## リスク管理

### 主要リスク一覧

| リスク | 影響度 | 発生確率 | 緩和策 |
|-------|-------|---------|-------|
| マイクロサービス間の連携複雑化 | 高 | 中 | サービスメッシュの採用、API設計の標準化、契約テスト |
| パフォーマンスボトルネック | 高 | 中 | 早期からのパフォーマンステスト、スケーラビリティを考慮した設計、キャッシング戦略 |
| データ一貫性の課題 | 高 | 高 | イベントソーシングパターン、最終的一貫性モデル、冪等性の確保 |
| セキュリティ脆弱性 | 高 | 低 | セキュリティファーストの設計、継続的な脆弱性スキャン、定期的なペネトレーションテスト |
| 運用複雑性の増大 | 中 | 高 | 自動化の徹底、包括的な監視・ロギング、自己修復メカニズム |
| スキルセットのギャップ | 中 | 中 | チームトレーニング、知識共有セッション、外部専門家の活用 |
| 依存サービスの障害 | 中 | 低 | サーキットブレーカー、フォールバックメカニズム、冗長性 |
| コスト超過 | 中 | 中 | 継続的なコスト監視、リソース最適化、段階的なスケーリング |

### 技術的リスクと緩和策

#### 1. 分散トランザクション管理の複雑さ

- **リスク**: マイクロサービス間でのトランザクション整合性確保の難しさ
- **緩和策**: 
  - Sagaパターンの採用: 分散トランザクションを一連の補償可能なローカルトランザクションに分解
  - イベントソーシング: 状態変更イベントの記録による一貫性の確保
  - 冪等操作: 同じリクエストが複数回実行されても安全なAPI設計

- **実装例（Sagaパターン）**:
  ```java
  // 注文作成Saga
  public class OrderCreationSaga {
    @Autowired private CommandGateway commandGateway;
    @Autowired private QueryGateway queryGateway;
    
    public CompletableFuture<OrderResult> createOrder(CreateOrderCommand command) {
      return commandGateway.send(new ReserveInventoryCommand(command.getItems()))
        .thenCompose(result -> commandGateway.send(new ProcessPaymentCommand(command.getPaymentDetails())))
        .thenCompose(result -> commandGateway.send(new FinalizeOrderCommand(command.getOrderId())))
        .exceptionally(ex -> {
          // 補償トランザクション
          commandGateway.send(new CancelOrderCommand(command.getOrderId()));
          return new OrderResult(OrderStatus.FAILED, ex.getMessage());
        });
    }
  }
  ```

#### 2. サービス間通信の信頼性

- **リスク**: ネットワーク障害によるサービス連携の失敗
- **緩和策**:
  - 非同期通信: Kafkaなどを使用したイベント駆動アーキテクチャ
  - リトライメカニズム: 指数バックオフと最大リトライ回数の設定
  - サーキットブレーカー: 障害の連鎖的波及防止（Resilience4j, Spring Cloud Circuit Breaker）

- **実装例（サーキットブレーカー）**:
  ```java
  @Service
  public class InventoryServiceClient {
    @CircuitBreaker(name = "inventoryService", fallbackMethod = "getInventoryFallback")
    @Retry(name = "inventoryService")
    @Bulkhead(name = "inventoryService")
    public Mono<InventoryResponse> getInventory(String productId) {
      return webClient.get()
        .uri("/inventory/{productId}", productId)
        .retrieve()
        .bodyToMono(InventoryResponse.class);
    }
    
    public Mono<InventoryResponse> getInventoryFallback(String productId, Exception ex) {
      // フォールバック処理（キャッシュからの取得または推定値）
      return cacheService.getCachedInventory(productId)
        .switchIfEmpty(Mono.just(new InventoryResponse(productId, 0, false)));
    }
  }
  ```

#### 3. データの整合性と同期

- **リスク**: 複数データストア間のデータ不整合
- **緩和策**:
  - イベント駆動アーキテクチャ: データ変更を統一イベントストリームで処理
  - 最終的一貫性モデル: 非同期的なデータ同期を許容
  - 変更データキャプチャ(CDC): データベース変更のストリーミング（Debezium）

- **実装例（CDC）**:
  ```yaml
  # Debezium Connector設定
  {
    "name": "inventory-connector",
    "config": {
      "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
      "database.hostname": "postgres",
      "database.port": "5432",
      "database.user": "postgres",
      "database.password": "postgres",
      "database.dbname": "inventory",
      "database.server.name": "inventory-db",
      "table.include.list": "public.products,public.inventory",
      "topic.prefix": "inventory",
      "transforms": "unwrap",
      "transforms.unwrap.type": "io.debezium.transforms.ExtractNewRecordState"
    }
  }
  ```

### 運用リスクと緩和策

#### 1. 複雑な監視と障害診断

- **リスク**: 分散システムでの問題特定の難しさ
- **緩和策**:
  - 分散トレーシング: リクエストフローの可視化（Jaeger, Zipkin）
  - 集中ログ管理: 統一されたログ分析（ELK Stack, Azure Log Analytics）
  - 相関ID: サービス間で一貫したリクエスト追跡

- **実装例（分散トレーシング）**:
  ```java
  @RestController
  public class ProductController {
    @Autowired private ProductService productService;
    @Autowired private Tracer tracer;
    
    @GetMapping("/products/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable String id, @RequestHeader HttpHeaders headers) {
      Span span = tracer.buildSpan("getProduct").start();
      try (Scope scope = tracer.scopeManager().activate(span)) {
        span.setTag("product.id", id);
        // リクエストヘッダーから相関IDを抽出
        String correlationId = headers.getFirst("X-Correlation-ID");
        span.setTag("correlation.id", correlationId);
        
        Product product = productService.findById(id);
        if (product == null) {
          span.setTag("error", true);
          span.log(Map.of("event", "product_not_found", "product.id", id));
          return ResponseEntity.notFound().build();
        }
        
        span.log("product_retrieved_successfully");
        return ResponseEntity.ok(product);
      } finally {
        span.finish();
      }
    }
  }
  ```

#### 2. デプロイメントの複雑化

- **リスク**: 複数サービスの連携デプロイの難しさ
- **緩和策**:
  - CI/CD自動化: パイプラインの完全自動化（GitHub Actions, Azure DevOps）
  - カナリアリリース: 段階的なトラフィック移行
  - ブルー/グリーンデプロイメント: ゼロダウンタイムデプロイ

- **実装例（GitHub Actions CI/CD）**:
  ```yaml
  name: CI/CD Pipeline

  on:
    push:
      branches: [ main ]
    pull_request:
      branches: [ main ]

  jobs:
    build:
      runs-on: ubuntu-latest
      steps:
      - uses: actions/checkout@v3
      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'
      - name: Build with Maven
        run: mvn -B package --file pom.xml
      - name: Run Tests
        run: mvn test
      - name: Build and Push Docker Image
        uses: docker/build-push-action@v2
        with:
          context: .
          push: true
          tags: ${{ secrets.ACR_LOGIN_SERVER }}/inventory-service:${{ github.sha }}
          
    deploy:
      needs: build
      runs-on: ubuntu-latest
      steps:
      - name: Azure Login
        uses: azure/login@v1
        with:
          creds: ${{ secrets.AZURE_CREDENTIALS }}
      - name: Deploy to AKS
        uses: azure/k8s-deploy@v1
        with:
          namespace: ski-shop
          manifests: |
            kubernetes/deployment.yaml
            kubernetes/service.yaml
          images: |
            ${{ secrets.ACR_LOGIN_SERVER }}/inventory-service:${{ github.sha }}
          strategy: canary
          percentage: 20
  ```

### ビジネスリスクと緩和策

#### 1. 開発コストと時間の増加

- **リスク**: マイクロサービスの初期開発コスト増加
- **緩和策**:
  - 段階的な移行: ビジネス価値の高い領域から優先的に実装
  - 共通ライブラリ: 横断的関心事の共有コード化
  - 開発者体験の向上: 内部開発者ポータル、自己サービス開発環境

#### 2. スキルセットとチーム構成

- **リスク**: 新技術習得の学習曲線
- **緩和策**:
  - トレーニングプログラム: 定期的な技術勉強会
  - ペアプログラミング: 知識共有の促進
  - 明確なアーキテクチャガイドライン: 設計原則とパターンのドキュメント化

#### 3. 長期的な保守性

- **リスク**: 多様な技術スタックによる保守の複雑化
- **緩和策**:
  - 技術スタックの標準化: 言語とフレームワークの選択肢を制限
  - 内部開発者ポータル: サービスカタログと文書化
  - 包括的なドキュメント: アーキテクチャ決定記録（ADR）の維持

### インシデント対応計画

1. **インシデント検出**:
   - 自動アラート（可用性、レイテンシ、エラーレート）
   - 異常検出（機械学習ベース）
   - ユーザー報告チャネル

2. **初期対応**:
   - 対応チーム編成（対応マネージャー、技術リード、通信担当）
   - 影響範囲の特定と分類（P1～P4）
   - 初期緩和措置の実施

3. **エスカレーションプロセス**:
   - 明確なエスカレーションパス
   - 事前定義された対応タイムライン
   - 上位管理職への通知基準

4. **復旧プロセス**:
   - 事前定義された復旧手順
   - 変更管理と承認プロセス
   - ロールバック決定基準

5. **事後分析**:
   - 根本原因分析（RCA）
   - 再発防止策の特定と実装
   - ナレッジベースの更新
