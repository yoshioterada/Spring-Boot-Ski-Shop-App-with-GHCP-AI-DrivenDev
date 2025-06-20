# 在庫管理サービス 詳細設計書

## 1. 概要

在庫管理サービスは、商品カタログ管理、在庫状況の追跡と更新、商品属性とカテゴリ管理、価格設定と割引管理、入荷・出荷処理、商品画像・メディア管理の機能を提供するマイクロサービスです。商品情報を一元的に管理し、他のマイクロサービスが商品データにアクセスするためのAPIを提供します。

## 2. 技術スタック

### 開発環境

- **言語**: Java 21 (LTS)
- **フレームワーク**: Spring Boot 3.2.3
- **ビルドツール**: Maven 3.9.x
- **コンテナ化**: Docker 25.x
- **テスト**: JUnit 5.10.1、Spring Boot Test、Testcontainers 1.19.3

### 本番環境

- Azure Container Apps
- Azure Cosmos DB (MongoDB API)
- Azure Database for PostgreSQL
- Azure Blob Storage

### 主要ライブラリとバージョン

| ライブラリ | バージョン | 用途 |
|----------|----------|------|
| spring-boot-starter-data-mongodb | 3.2.3 | MongoDB データアクセス |
| spring-boot-starter-data-jpa | 3.2.3 | JPA データアクセス |
| spring-boot-starter-web | 3.2.3 | REST API エンドポイント |
| spring-boot-starter-validation | 3.2.3 | 入力バリデーション |
| spring-boot-starter-security | 3.2.3 | セキュリティ設定 |
| spring-boot-starter-actuator | 3.2.3 | ヘルスチェック、メトリクス |
| spring-cloud-starter-stream-kafka | 4.1.0 | イベント発行 |
| spring-boot-starter-cache | 3.2.3 | キャッシュ機能 |
| hibernate-core | 6.4.1 | ORM マッピング |
| postgresql | 42.7.1 | PostgreSQL JDBC ドライバ |
| mongodb-driver-sync | 4.11.1 | MongoDB ドライバ |
| azure-storage-blob | 12.24.0 | Azure Blob Storage クライアント |
| azure-cosmos | 4.51.0 | Azure Cosmos DB クライアント |
| mapstruct | 1.5.5.Final | オブジェクトマッピング |
| lombok | 1.18.30 | ボイラープレートコード削減 |
| micrometer-registry-prometheus | 1.12.2 | メトリクス収集 |
| springdoc-openapi-starter-webmvc-ui | 2.3.0 | API 文書化 |
| imageio-core | 3.9.4 | 画像処理 |
| thumbnailator | 0.4.20 | サムネイル生成 |
| azure-identity | 1.11.1 | Azure 認証 |
| azure-monitor-opentelemetry | 1.0.0-beta.15 | Azure 監視連携 |
| logback-json-classic | 0.1.5 | JSON 形式ログ出力 |

## 3. システム構成

### コンポーネント構成図

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
        PROD_SERV --> EVENT
        INV_SERV --> EVENT
        PRICE_SERV --> EVENT
        
        CACHE[Cache Service] --> REDIS[(Redis)]
        PROD_SERV --> CACHE
        CAT_SERV --> CACHE
    end
    
    %% 外部システムとの接続
    API --> GATEWAY[APIゲートウェイ]
    EVENT --> SALES[販売管理サービス]
    EVENT --> AI[AIサポートサービス]
```

### クラス構成

#### 主要クラス

**コントローラー層**

- `ProductController`: 商品関連のREST APIエンドポイント
- `CategoryController`: カテゴリ関連のREST APIエンドポイント
- `InventoryController`: 在庫関連のREST APIエンドポイント
- `PricingController`: 価格関連のREST APIエンドポイント
- `MediaController`: 商品画像関連のREST APIエンドポイント
- `AdminProductController`: 管理者向け商品管理API

**サービス層**

- `ProductService`: 商品関連のビジネスロジック
- `CategoryService`: カテゴリ関連のビジネスロジック
- `InventoryService`: 在庫関連のビジネスロジック
- `PricingService`: 価格関連のビジネスロジック
- `MediaService`: 画像関連のビジネスロジック
- `CacheService`: キャッシュ管理
- `EventPublisherService`: イベント発行

**リポジトリ層**

- `ProductRepository`: 商品データアクセス
- `CategoryRepository`: カテゴリデータアクセス
- `InventoryRepository`: 在庫データアクセス
- `PriceRepository`: 価格データアクセス
- `ImageRepository`: 画像データアクセス
- `SupplierRepository`: サプライヤーデータアクセス

**モデル**

- `Product`: 商品エンティティ
- `Category`: カテゴリエンティティ
- `Inventory`: 在庫エンティティ
- `Price`: 価格エンティティ
- `PriceHistory`: 価格履歴エンティティ
- `ProductImage`: 商品画像エンティティ
- `Supplier`: サプライヤーエンティティ

**DTO**

- `ProductDTO`: 商品データ転送オブジェクト
- `CategoryDTO`: カテゴリデータ転送オブジェクト
- `InventoryDTO`: 在庫データ転送オブジェクト
- `PriceDTO`: 価格データ転送オブジェクト
- `ImageDTO`: 画像データ転送オブジェクト

**マッパー**

- `ProductMapper`: 商品エンティティとDTO間のマッピング
- `CategoryMapper`: カテゴリエンティティとDTO間のマッピング
- `InventoryMapper`: 在庫エンティティとDTO間のマッピング
- `PriceMapper`: 価格エンティティとDTO間のマッピング
- `ImageMapper`: 画像エンティティとDTO間のマッピング

**設定**

- `MongoConfig`: MongoDB設定
- `JpaConfig`: JPA設定
- `SecurityConfig`: セキュリティ設定
- `CacheConfig`: キャッシュ設定
- `BlobStorageConfig`: Blob Storage設定
- `KafkaConfig`: Kafka設定

### データベース設計

#### ER図

```mermaid
erDiagram
    Product ||--o{ ProductImage : "has"
    Product ||--o{ Inventory : "has"
    Product }o--|| Category : "belongs to"
    Product ||--o{ Price : "has"
    Product ||--o{ PriceHistory : "has"
    Product }o--o{ Supplier : "supplied by"
    
    Product {
        String id PK "MongoDB ID"
        String sku
        String name
        String description
        String brand
        JSON attributes
        String[] tags
        Boolean active
        Timestamp createdAt
        Timestamp updatedAt
        String categoryId FK
    }
    
    Category {
        String id PK "MongoDB ID"
        String name
        String description
        String parentId FK
        Integer level
        String path
        Boolean active
        Timestamp createdAt
        Timestamp updatedAt
    }
    
    Inventory {
        UUID id PK
        String productId FK
        Integer quantity
        Integer reservedQuantity
        String locationCode
        String status
        Timestamp createdAt
        Timestamp updatedAt
    }
    
    Price {
        UUID id PK
        String productId FK
        BigDecimal regularPrice
        BigDecimal salePrice
        Timestamp saleStartDate
        Timestamp saleEndDate
        String currencyCode
        Boolean isActive
        Timestamp createdAt
        Timestamp updatedAt
    }
    
    PriceHistory {
        UUID id PK
        String productId FK
        BigDecimal price
        String priceType
        Timestamp effectiveDate
        String reason
        String currencyCode
        Timestamp createdAt
    }
    
    ProductImage {
        UUID id PK
        String productId FK
        String url
        String thumbnailUrl
        String type
        Integer sortOrder
        String altText
        Timestamp createdAt
        Timestamp updatedAt
    }
    
    Supplier {
        UUID id PK
        String name
        String contactPerson
        String email
        String phone
        String address
        Boolean active
        Timestamp createdAt
        Timestamp updatedAt
    }
    
    ProductSupplier {
        String productId PK,FK
        UUID supplierId PK,FK
        String supplierProductCode
        BigDecimal supplierPrice
        Integer leadTimeDays
        Timestamp lastOrderDate
        Timestamp createdAt
        Timestamp updatedAt
    }
```

## 4. API設計

### RESTful APIエンドポイント

#### 商品API

| メソッド | パス | 説明 | 認証要件 |
|--------|-----|------|----------|
| GET | /api/products | 商品一覧取得 | 不要 |
| GET | /api/products/{id} | 商品詳細取得 | 不要 |
| POST | /api/products | 商品作成 | 要認証（管理者） |
| PUT | /api/products/{id} | 商品更新 | 要認証（管理者） |
| DELETE | /api/products/{id} | 商品削除 | 要認証（管理者） |
| GET | /api/products/sku/{sku} | SKUによる商品取得 | 不要 |
| GET | /api/products/search | 商品検索 | 不要 |

#### カテゴリAPI

| メソッド | パス | 説明 | 認証要件 |
|--------|-----|------|----------|
| GET | /api/categories | カテゴリ一覧取得 | 不要 |
| GET | /api/categories/{id} | カテゴリ詳細取得 | 不要 |
| GET | /api/categories/{id}/products | カテゴリに属する商品一覧取得 | 不要 |
| POST | /api/categories | カテゴリ作成 | 要認証（管理者） |
| PUT | /api/categories/{id} | カテゴリ更新 | 要認証（管理者） |
| DELETE | /api/categories/{id} | カテゴリ削除 | 要認証（管理者） |

#### 在庫API

| メソッド | パス | 説明 | 認証要件 |
|--------|-----|------|----------|
| GET | /api/inventory/{productId} | 商品の在庫状況取得 | 不要 |
| GET | /api/inventory/status/{productId} | 商品の在庫ステータス取得 | 不要 |
| POST | /api/inventory/reserve | 在庫予約 | 要認証 |
| POST | /api/inventory/release | 在庫予約解除 | 要認証 |
| POST | /api/inventory/stock-in | 入荷処理 | 要認証（管理者） |
| POST | /api/inventory/stock-out | 出荷処理 | 要認証（管理者） |

#### 価格API

| メソッド | パス | 説明 | 認証要件 |
|--------|-----|------|----------|
| GET | /api/prices/{productId} | 商品の現在価格取得 | 不要 |
| GET | /api/prices/{productId}/history | 商品の価格履歴取得 | 要認証（管理者） |
| POST | /api/prices | 価格設定 | 要認証（管理者） |
| PUT | /api/prices/{id} | 価格更新 | 要認証（管理者） |
| POST | /api/prices/sale | セール価格設定 | 要認証（管理者） |

#### 画像API

| メソッド | パス | 説明 | 認証要件 |
|--------|-----|------|----------|
| GET | /api/media/{productId}/images | 商品画像一覧取得 | 不要 |
| POST | /api/media/{productId}/images | 商品画像アップロード | 要認証（管理者） |
| DELETE | /api/media/images/{id} | 商品画像削除 | 要認証（管理者） |
| PUT | /api/media/images/{id}/order | 商品画像順序更新 | 要認証（管理者） |

### APIリクエスト・レスポンス例

#### 商品一覧取得

**リクエスト**

```http
GET /api/products?category=ski&page=0&size=10 HTTP/1.1
```

**レスポンス**

```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "content": [
    {
      "id": "61f7c8a53e5c74a9a2f22b8b",
      "sku": "SKI-ATOMIC-001",
      "name": "Atomic Bent 100 Ski",
      "description": "The Atomic Bent 100 is a versatile all-mountain ski that delivers...",
      "brand": "Atomic",
      "attributes": {
        "length": "180cm",
        "width": "100mm",
        "color": "Red/Black",
        "material": "Wood core with carbon inserts",
        "yearModel": "2025"
      },
      "price": {
        "regularPrice": 65000,
        "salePrice": 58500,
        "currencyCode": "JPY",
        "onSale": true
      },
      "inventory": {
        "status": "IN_STOCK",
        "quantity": 15
      },
      "category": {
        "id": "61f7c8a53e5c74a9a2f22b01",
        "name": "スキー板"
      },
      "imageUrl": "https://storage.skieshop.com/products/atomic-bent-100-main.jpg",
      "active": true
    },
    // 他の商品...
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "sort": {
      "sorted": true,
      "unsorted": false
    }
  },
  "totalElements": 42,
  "totalPages": 5,
  "last": false,
  "first": true,
  "size": 10,
  "number": 0
}
```

#### 商品詳細取得

**リクエスト**

```http
GET /api/products/61f7c8a53e5c74a9a2f22b8b HTTP/1.1
```

**レスポンス**

```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "id": "61f7c8a53e5c74a9a2f22b8b",
  "sku": "SKI-ATOMIC-001",
  "name": "Atomic Bent 100 Ski",
  "description": "The Atomic Bent 100 is a versatile all-mountain ski that delivers exceptional performance across various snow conditions. With a 100mm waist and rockered tip and tail, it floats effortlessly in powder while maintaining stability on groomed runs. The lightweight wood core with carbon inserts provides the perfect balance of strength and flexibility.",
  "brand": "Atomic",
  "attributes": {
    "length": "180cm",
    "width": "100mm",
    "color": "Red/Black",
    "material": "Wood core with carbon inserts",
    "yearModel": "2025",
    "skill": "Intermediate to Advanced",
    "terrain": "All Mountain",
    "flex": "Medium",
    "radius": "19m"
  },
  "price": {
    "regularPrice": 65000,
    "salePrice": 58500,
    "currencyCode": "JPY",
    "onSale": true,
    "saleStartDate": "2025-06-01T00:00:00Z",
    "saleEndDate": "2025-07-31T23:59:59Z"
  },
  "inventory": {
    "status": "IN_STOCK",
    "quantity": 15,
    "availableSizes": ["170cm", "180cm", "190cm"]
  },
  "category": {
    "id": "61f7c8a53e5c74a9a2f22b01",
    "name": "スキー板",
    "path": "スキー用品/スキー板"
  },
  "images": [
    {
      "id": "61f7c8a53e5c74a9a2f33c01",
      "url": "https://storage.skieshop.com/products/atomic-bent-100-main.jpg",
      "thumbnailUrl": "https://storage.skieshop.com/products/thumbnails/atomic-bent-100-main.jpg",
      "altText": "Atomic Bent 100 Side View",
      "sortOrder": 1
    },
    {
      "id": "61f7c8a53e5c74a9a2f33c02",
      "url": "https://storage.skieshop.com/products/atomic-bent-100-bottom.jpg",
      "thumbnailUrl": "https://storage.skieshop.com/products/thumbnails/atomic-bent-100-bottom.jpg",
      "altText": "Atomic Bent 100 Bottom View",
      "sortOrder": 2
    }
  ],
  "tags": ["all-mountain", "powder", "freeride", "2025モデル"],
  "active": true,
  "createdAt": "2025-01-15T09:30:00Z",
  "updatedAt": "2025-06-01T10:15:00Z"
}
```

## 5. イベント設計

### 発行イベント

| イベント名 | 説明 | ペイロード |
|-----------|------|-----------|
| product.created | 商品作成時 | 商品ID、SKU、名前、カテゴリ、作成日時 |
| product.updated | 商品情報更新時 | 商品ID、更新フィールド、更新日時 |
| product.deleted | 商品削除時 | 商品ID、削除日時 |
| inventory.updated | 在庫数更新時 | 商品ID、新在庫数、更新理由、更新日時 |
| inventory.low | 在庫数低下時 | 商品ID、残り在庫数、閾値、日時 |
| inventory.out_of_stock | 在庫切れ時 | 商品ID、日時 |
| inventory.reserved | 在庫予約時 | 商品ID、予約数、予約ID、日時 |
| price.updated | 価格更新時 | 商品ID、旧価格、新価格、更新日時 |
| price.sale_started | セール開始時 | 商品ID、通常価格、セール価格、開始日時、終了日時 |
| price.sale_ended | セール終了時 | 商品ID、通常価格、セール価格、終了日時 |

### 購読イベント

| イベント名 | 説明 | アクション |
|-----------|------|----------|
| order.created | 注文作成時 | 在庫予約 |
| order.completed | 注文完了時 | 在庫確定減少 |
| order.cancelled | 注文キャンセル時 | 在庫予約解除 |
| shipment.completed | 出荷完了時 | 在庫実数更新 |

### イベントスキーマ例

**inventory.updated イベント**

```json
{
  "eventId": "3e7f8c9a-2d56-4e78-9b12-f67abc890de1",
  "eventType": "inventory.updated",
  "timestamp": "2025-06-19T14:30:00Z",
  "producer": "inventory-service",
  "payload": {
    "productId": "61f7c8a53e5c74a9a2f22b8b",
    "sku": "SKI-ATOMIC-001",
    "previousQuantity": 10,
    "newQuantity": 15,
    "reason": "STOCK_IN",
    "locationCode": "TOKYO_WH",
    "updatedAt": "2025-06-19T14:30:00Z",
    "referenceId": "PO-2025-0619-001"
  }
}
```

## 6. セキュリティ設計

### 認証・認可

- JWT トークンベース認証
- ロールベースアクセス制御（RBAC）
- Spring Security による認可フィルター
- 公開/非公開APIの分離

### データ保護

- 転送中のデータは TLS 1.3 で暗号化
- Azure Key Vault で管理された鍵による機密データの暗号化
- データベースへのアクセスは最小権限の原則に基づく
- 機密性の高い価格情報や在庫情報へのアクセス制限

### API セキュリティ

- レート制限の実装
- CORS設定の適切な構成
- 入力値のバリデーションとサニタイゼーション
- SQLインジェクション対策（JPA/MongoDBリポジトリの使用）
- クロスサイトスクリプティング（XSS）対策

### 画像セキュリティ

- 安全なファイルアップロード検証
- ファイルタイプとサイズの制限
- 画像のメタデータ削除
- Azure Blob Storage のSASトークンを使用したアクセス制御

## 7. エラー処理

### エラーコード設計

| エラーコード | HTTP ステータス | 説明 |
|------------|----------------|------|
| PROD_001 | 400 | 不正なリクエスト形式 |
| PROD_002 | 400 | バリデーションエラー |
| PROD_003 | 409 | SKU重複 |
| PROD_004 | 404 | 商品が見つからない |
| PROD_005 | 404 | カテゴリが見つからない |
| INV_001 | 400 | 在庫数不足 |
| INV_002 | 400 | 不正な在庫操作 |
| INV_003 | 409 | 在庫が既に予約済み |
| PRICE_001 | 400 | 不正な価格設定 |
| PRICE_002 | 404 | 価格情報が見つからない |
| MEDIA_001 | 400 | 不正なファイル形式 |
| MEDIA_002 | 400 | ファイルサイズ超過 |
| MEDIA_003 | 500 | ファイルアップロード失敗 |

### エラーレスポンス形式

```json
{
  "timestamp": "2025-06-19T14:35:00Z",
  "status": 400,
  "error": "Bad Request",
  "code": "INV_001",
  "message": "在庫数が不足しています",
  "details": {
    "productId": "61f7c8a53e5c74a9a2f22b8b",
    "requestedQuantity": 10,
    "availableQuantity": 5
  },
  "path": "/api/inventory/reserve"
}
```

### 例外ハンドリング

- `@RestControllerAdvice` による集中的な例外ハンドリング
- カスタム例外クラス階層
- 詳細なログ記録（機密情報を除く）
- 障害時の適切なフォールバック

## 8. パフォーマンス最適化

### キャッシュ戦略

- Redis を使用した分散キャッシュ
- Spring Cache アノテーションによるキャッシュ管理
- 頻繁に読み取られる情報のキャッシュ（商品詳細、カテゴリツリー）
- 多段階キャッシュ（メモリ内、Redis、CDN）
- キャッシュの有効期限設定

### データベース最適化

- インデックス設計
  - MongoDB: 商品のSKU、名前、カテゴリIDにインデックス
  - PostgreSQL: 在庫テーブルの商品ID、価格テーブルの商品ID
- 複合インデックスの活用
- クエリの最適化（MongoDB クエリ分析、PostgreSQL EXPLAIN）
- シャーディング戦略の検討（将来の拡張）

### 画像最適化

- 画像の複数サイズ生成（オリジナル、中サイズ、サムネイル）
- WebP形式の活用
- CDNを使用した画像配信
- レスポンシブ画像の提供

### 負荷テスト基準

- 通常時 1,000 リクエスト/秒の処理
- ピーク時 3,000 リクエスト/秒の処理
- レスポンスタイム 95% が 150ms 以下
- CPU 使用率 75% 以下
- 大量商品データ（100万SKU）での性能検証

## 9. 監視・ロギング

### メトリクス

- API エンドポイントごとのリクエスト数と応答時間
- データベースクエリの実行時間
- 在庫変動率
- キャッシュヒット率
- BLOB ストレージアクセス頻度
- CPU/メモリ使用率

### ログ

- 構造化ログ（JSON 形式）
- ログレベル: INFO（本番）、DEBUG（開発/テスト）
- 重要な操作の監査ログ（価格変更、在庫調整）
- 画像アップロード・削除の記録
- データベース操作の記録

### アラート

- 在庫閾値アラート
- エラーレート閾値超過
- レスポンスタイム閾値超過
- ディスク使用率閾値超過
- 大量在庫変動の検知

## 10. テスト戦略

### 単体テスト

- サービス層のビジネスロジックテスト
- リポジトリ層のデータアクセステスト
- コントローラー層の入力検証テスト
- モックとスタブを使用した依存コンポーネント分離

### 統合テスト

- Testcontainers を使用した実際のデータベース（MongoDB、PostgreSQL）との統合テスト
- Blob Storage エミュレーターとの統合テスト
- Kafka との統合テスト
- 在庫・価格変更の一貫性テスト

### 契約テスト

- Spring Cloud Contract による API 契約テスト
- 他のマイクロサービスとの連携検証（販売管理サービス、AIサポートサービス）

### 負荷テスト

- Gatling を使用した負荷テスト
- 商品検索と一覧表示の性能テスト
- 画像アップロード・ダウンロードの性能テスト
- 同時在庫更新時の競合テスト

### テストカバレッジ目標

- 単体テスト: コード網羅率 85% 以上
- 統合テスト: 主要フロー 100% カバー
- 契約テスト: 全ての公開 API をカバー

## 11. デプロイメント

### Docker コンテナ化

**Dockerfile**

```dockerfile
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY build/libs/inventory-service.jar /app/app.jar

ENV JAVA_OPTS="-Xms512m -Xmx1024m"

EXPOSE 8082

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

### Kubernetes/Azure Container Apps デプロイメント設定

**deployment.yaml**

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: inventory-service
  labels:
    app: inventory-service
spec:
  replicas: 2
  selector:
    matchLabels:
      app: inventory-service
  template:
    metadata:
      labels:
        app: inventory-service
    spec:
      containers:
      - name: inventory-service
        image: ${CONTAINER_REGISTRY}/inventory-service:${IMAGE_TAG}
        ports:
        - containerPort: 8082
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: SPRING_DATA_MONGODB_URI
          valueFrom:
            secretKeyRef:
              name: inventory-service-secrets
              key: mongodb-uri
        - name: SPRING_DATASOURCE_URL
          valueFrom:
            secretKeyRef:
              name: inventory-service-secrets
              key: db-url
        - name: SPRING_DATASOURCE_USERNAME
          valueFrom:
            secretKeyRef:
              name: inventory-service-secrets
              key: db-username
        - name: SPRING_DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: inventory-service-secrets
              key: db-password
        - name: AZURE_STORAGE_CONNECTION_STRING
          valueFrom:
            secretKeyRef:
              name: inventory-service-secrets
              key: storage-connection-string
        - name: SPRING_KAFKA_BOOTSTRAP_SERVERS
          value: "kafka-service:9092"
        resources:
          limits:
            cpu: "1"
            memory: "1.5Gi"
          requests:
            cpu: "0.5"
            memory: "1Gi"
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8082
          initialDelaySeconds: 30
          periodSeconds: 10
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8082
          initialDelaySeconds: 60
          periodSeconds: 20
```

### ローカル開発環境セットアップ

**docker-compose.yml での設定**

```yaml
version: '3.8'

services:
  inventory-service:
    build:
      context: ./inventory-service
      dockerfile: Dockerfile.dev
    ports:
      - "8082:8082"
    depends_on:
      - mongodb
      - postgres
      - redis
      - kafka
    environment:
      SPRING_PROFILES_ACTIVE: dev
      SERVER_PORT: 8082
      SPRING_DATA_MONGODB_URI: mongodb://skieshop:skieshop@mongodb:27017/inventory
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/skieshop_inventory
      SPRING_DATASOURCE_USERNAME: skieshop
      SPRING_DATASOURCE_PASSWORD: skieshop
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:29092
      SPRING_REDIS_HOST: redis
      SPRING_REDIS_PORT: 6379
      AZURE_STORAGE_CONNECTION_STRING: "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;BlobEndpoint=http://azurite:10000/devstoreaccount1;"
    volumes:
      - ./inventory-service:/app
      - maven-repo:/root/.m2
    networks:
      - skieshop-network
```

### 環境変数設定

| 環境変数 | 説明 | デフォルト値 |
|---------|------|------------|
| SERVER_PORT | サービスポート | 8082 |
| SPRING_PROFILES_ACTIVE | アクティブプロファイル | dev |
| SPRING_DATA_MONGODB_URI | MongoDB 接続 URI | mongodb://localhost:27017/inventory |
| SPRING_DATASOURCE_URL | PostgreSQL 接続 URL | jdbc:postgresql://localhost:5432/skieshop_inventory |
| SPRING_DATASOURCE_USERNAME | データベースユーザー | skieshop |
| SPRING_DATASOURCE_PASSWORD | データベースパスワード | (必須) |
| SPRING_KAFKA_BOOTSTRAP_SERVERS | Kafka サーバー | localhost:9092 |
| SPRING_REDIS_HOST | Redis ホスト | localhost |
| SPRING_REDIS_PORT | Redis ポート | 6379 |
| AZURE_STORAGE_CONNECTION_STRING | Azure Storage 接続文字列 | (必須) |
| AZURE_STORAGE_CONTAINER_NAME | Blob コンテナ名 | product-images |
| LOGGING_LEVEL_ROOT | ルートログレベル | INFO |
| LOGGING_LEVEL_COM_SKIESHOP | アプリケーションログレベル | INFO |

## 12. 運用・保守

### バックアップ戦略

- MongoDB: 毎日のフルバックアップと増分バックアップ
- PostgreSQL: 毎日のフルバックアップとWALアーカイブ
- Azure Blob Storage: 地理冗長ストレージ（GRS）の活用
- データのエクスポート機能

### データマイグレーション

- MongoDB: マイグレーションスクリプト
- PostgreSQL: Flyway によるバージョン管理されたマイグレーション
- ゼロダウンタイムマイグレーション手順
- ロールバック計画

### スケーリング戦略

- 水平スケーリング: レプリカ数増加
- 垂直スケーリング: コンテナリソース割り当て増加
- 自動スケーリング設定（CPU 使用率 70% 超過時）
- 読み取り/書き込み分離の検討

### 障害対応

- 障害検知: ヘルスチェック、メトリクス監視
- 復旧手順: 自動リトライ、サーキットブレーカー
- フォールバック戦略: キャッシュからの提供、読み取り専用モード
- 定期的なディザスタリカバリ訓練

### メンテナンス手順

- 計画的メンテナンスウィンドウ
- ローリングアップデート
- カナリアデプロイメント
- データベースインデックス再構築

## 13. 開発ガイドライン

### コーディング規約

- Google Java スタイルガイド準拠
- Checkstyle、SpotBugs による静的解析
- SonarQube によるコード品質チェック
- コードレビュープロセス

### API 開発ガイドライン

- リソース命名規則: 複数形の名詞（例: /products, /categories）
- HTTP メソッド使用規則
- クエリパラメータ形式
- ページネーション実装
- バージョニング戦略

### ドキュメント

- JavaDoc によるコードドキュメント
- OpenAPI/Swagger によるAPI ドキュメント
- README による環境構築手順
- 運用マニュアル

## 14. 実装参考コード

### エンティティ定義例

```java
@Document(collection = "products")
@Getter
@Setter
@NoArgsConstructor
public class Product {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String sku;
    
    @Indexed
    private String name;
    
    private String description;
    
    private String brand;
    
    @Field("attributes")
    private Map<String, Object> attributes = new HashMap<>();
    
    @Indexed
    private List<String> tags = new ArrayList<>();
    
    @Indexed
    private String categoryId;
    
    private boolean active = true;
    
    @CreatedDate
    private Instant createdAt;
    
    @LastModifiedDate
    private Instant updatedAt;
    
    @DBRef(lazy = true)
    @ReadOnlyProperty
    private List<ProductImage> images;
    
    // MongoDB @Version for optimistic locking
    @Version
    private Long version;
}
```

```java
@Entity
@Table(name = "inventory")
@Getter
@Setter
@NoArgsConstructor
public class Inventory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false)
    private String productId;
    
    @Column(nullable = false)
    private Integer quantity;
    
    @Column(nullable = false)
    private Integer reservedQuantity = 0;
    
    private String locationCode;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InventoryStatus status = InventoryStatus.IN_STOCK;
    
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    
    private Instant updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
        
        // 在庫ステータスの自動更新
        if (quantity <= 0) {
            status = InventoryStatus.OUT_OF_STOCK;
        } else if (quantity <= 5) { // 低在庫閾値
            status = InventoryStatus.LOW_STOCK;
        } else {
            status = InventoryStatus.IN_STOCK;
        }
    }
    
    // 有効在庫数の計算
    public int getAvailableQuantity() {
        return Math.max(0, quantity - reservedQuantity);
    }
    
    public enum InventoryStatus {
        IN_STOCK, LOW_STOCK, OUT_OF_STOCK, DISCONTINUED
    }
}
```

### コントローラー実装例

```java
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Product Management", description = "Product management APIs")
public class ProductController {
    
    private final ProductService productService;
    private final ProductMapper productMapper;
    
    @GetMapping
    @Operation(summary = "Get product list")
    public Page<ProductDTO> getProducts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "name,asc") String sort) {
        
        ProductSearchCriteria criteria = new ProductSearchCriteria();
        criteria.setCategory(category);
        criteria.setBrand(brand);
        criteria.setTags(tags);
        criteria.setMinPrice(minPrice);
        criteria.setMaxPrice(maxPrice);
        
        String[] sortParts = sort.split(",");
        Sort.Direction direction = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("desc") ? 
                Sort.Direction.DESC : Sort.Direction.ASC;
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParts[0]));
        
        return productService.findProducts(criteria, pageable)
                .map(productMapper::toDto);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID")
    public ProductDetailDTO getProductById(@PathVariable String id) {
        Product product = productService.findById(id);
        return productMapper.toDetailDto(product);
    }
    
    @GetMapping("/sku/{sku}")
    @Operation(summary = "Get product by SKU")
    public ProductDetailDTO getProductBySku(@PathVariable String sku) {
        Product product = productService.findBySku(sku);
        return productMapper.toDetailDto(product);
    }
    
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new product")
    @PreAuthorize("hasRole('ADMIN')")
    public ProductDetailDTO createProduct(@Valid @RequestBody ProductCreateDTO productDTO) {
        Product product = productMapper.toEntity(productDTO);
        Product savedProduct = productService.createProduct(product);
        return productMapper.toDetailDto(savedProduct);
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Update product")
    @PreAuthorize("hasRole('ADMIN')")
    public ProductDetailDTO updateProduct(
            @PathVariable String id, 
            @Valid @RequestBody ProductUpdateDTO productDTO) {
        
        Product updatedProduct = productService.updateProduct(id, productDTO);
        return productMapper.toDetailDto(updatedProduct);
    }
    
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete product")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteProduct(@PathVariable String id) {
        productService.deleteProduct(id);
    }
    
    @GetMapping("/search")
    @Operation(summary = "Search products")
    public Page<ProductDTO> searchProducts(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        return productService.searchProducts(query, pageable)
                .map(productMapper::toDto);
    }
}
```

### サービス実装例

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements InventoryService {
    
    private final InventoryRepository inventoryRepository;
    private final EventPublisherService eventPublisher;
    
    @Override
    @Transactional(readOnly = true)
    public InventoryDTO getInventory(String productId) {
        Inventory inventory = findInventoryByProductId(productId);
        return mapToDto(inventory);
    }
    
    @Override
    @Transactional(readOnly = true)
    public InventoryStatus getInventoryStatus(String productId) {
        Inventory inventory = findInventoryByProductId(productId);
        return inventory.getStatus();
    }
    
    @Override
    @Transactional
    public boolean reserveInventory(String productId, int quantity, String reservationId) {
        Inventory inventory = findInventoryByProductId(productId);
        
        // 在庫確認
        if (inventory.getAvailableQuantity() < quantity) {
            log.warn("Not enough inventory for product: {}, requested: {}, available: {}", 
                    productId, quantity, inventory.getAvailableQuantity());
            return false;
        }
        
        // 在庫予約
        inventory.setReservedQuantity(inventory.getReservedQuantity() + quantity);
        inventoryRepository.save(inventory);
        
        // イベント発行
        eventPublisher.publishInventoryReserved(productId, quantity, reservationId);
        
        log.info("Inventory reserved: productId={}, quantity={}, reservationId={}", 
                productId, quantity, reservationId);
        
        return true;
    }
    
    @Override
    @Transactional
    public void releaseInventory(String productId, int quantity, String reservationId) {
        Inventory inventory = findInventoryByProductId(productId);
        
        // 予約解除（予約数を超えないように）
        int newReservedQuantity = Math.max(0, inventory.getReservedQuantity() - quantity);
        inventory.setReservedQuantity(newReservedQuantity);
        
        inventoryRepository.save(inventory);
        
        // イベント発行
        eventPublisher.publishInventoryReleased(productId, quantity, reservationId);
        
        log.info("Inventory released: productId={}, quantity={}, reservationId={}", 
                productId, quantity, reservationId);
    }
    
    @Override
    @Transactional
    public void stockIn(StockInDTO stockInDTO) {
        Inventory inventory = findInventoryByProductId(stockInDTO.getProductId());
        
        int previousQuantity = inventory.getQuantity();
        inventory.setQuantity(inventory.getQuantity() + stockInDTO.getQuantity());
        
        Inventory savedInventory = inventoryRepository.save(inventory);
        
        // イベント発行
        eventPublisher.publishInventoryUpdated(
                stockInDTO.getProductId(),
                previousQuantity,
                savedInventory.getQuantity(),
                "STOCK_IN",
                stockInDTO.getReferenceId()
        );
        
        log.info("Stock in: productId={}, quantity={}, newTotal={}, reference={}", 
                stockInDTO.getProductId(), stockInDTO.getQuantity(), 
                savedInventory.getQuantity(), stockInDTO.getReferenceId());
    }
    
    @Override
    @Transactional
    public boolean stockOut(StockOutDTO stockOutDTO) {
        Inventory inventory = findInventoryByProductId(stockOutDTO.getProductId());
        
        // 在庫確認
        if (inventory.getQuantity() < stockOutDTO.getQuantity()) {
            log.warn("Not enough inventory for stock out: {}, requested: {}, available: {}", 
                    stockOutDTO.getProductId(), stockOutDTO.getQuantity(), inventory.getQuantity());
            return false;
        }
        
        int previousQuantity = inventory.getQuantity();
        inventory.setQuantity(inventory.getQuantity() - stockOutDTO.getQuantity());
        
        // 予約がある場合は予約も減らす
        if (stockOutDTO.isFromReservation() && inventory.getReservedQuantity() > 0) {
            int reductionAmount = Math.min(stockOutDTO.getQuantity(), inventory.getReservedQuantity());
            inventory.setReservedQuantity(inventory.getReservedQuantity() - reductionAmount);
        }
        
        Inventory savedInventory = inventoryRepository.save(inventory);
        
        // イベント発行
        eventPublisher.publishInventoryUpdated(
                stockOutDTO.getProductId(),
                previousQuantity,
                savedInventory.getQuantity(),
                "STOCK_OUT",
                stockOutDTO.getReferenceId()
        );
        
        // 在庫切れチェック
        if (savedInventory.getQuantity() == 0) {
            eventPublisher.publishInventoryOutOfStock(stockOutDTO.getProductId());
        } else if (savedInventory.getStatus() == InventoryStatus.LOW_STOCK) {
            eventPublisher.publishInventoryLow(
                    stockOutDTO.getProductId(), 
                    savedInventory.getQuantity(), 
                    5  // 低在庫閾値
            );
        }
        
        log.info("Stock out: productId={}, quantity={}, newTotal={}, reference={}", 
                stockOutDTO.getProductId(), stockOutDTO.getQuantity(), 
                savedInventory.getQuantity(), stockOutDTO.getReferenceId());
        
        return true;
    }
    
    // その他のメソッド実装...
    
    private Inventory findInventoryByProductId(String productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Inventory not found for product: " + productId));
    }
    
    private InventoryDTO mapToDto(Inventory inventory) {
        InventoryDTO dto = new InventoryDTO();
        dto.setProductId(inventory.getProductId());
        dto.setQuantity(inventory.getQuantity());
        dto.setReservedQuantity(inventory.getReservedQuantity());
        dto.setAvailableQuantity(inventory.getAvailableQuantity());
        dto.setStatus(inventory.getStatus());
        dto.setLocationCode(inventory.getLocationCode());
        return dto;
    }
}
```

## 15. 環境構築・実行手順

### ローカル開発環境構築

1. 前提条件
   - Java 21
   - Docker と Docker Compose
   - Git

2. リポジトリのクローン
   ```bash
   git clone https://github.com/skieshop/inventory-service.git
   cd inventory-service
   ```

3. ローカルでの実行
   ```bash
   # 依存サービスの起動
   docker-compose up -d mongodb postgres redis kafka
   
   # アプリケーションのビルドと実行
   mvn spring-boot:run
   ```

4. Docker Compose での起動
   ```bash
   docker-compose up -d
   ```

### 単体・統合テスト実行

```bash
# 単体テスト実行
mvn test

# 統合テスト実行
mvn verify -P integration-test

# すべてのテスト実行
mvn verify
```

### 本番環境デプロイ手順

1. ビルドとコンテナイメージ作成
   ```bash
   mvn clean package
   docker build -t skieshop/inventory-service:latest .
   ```

2. コンテナレジストリへのプッシュ
   ```bash
   docker tag skieshop/inventory-service:latest acrskileshop.azurecr.io/inventory-service:latest
   docker push acrskileshop.azurecr.io/inventory-service:latest
   ```

3. Azure Container Apps へのデプロイ
   ```bash
   az login
   az acr login --name acrskileshop
   
   az containerapp update \
     --name inventory-service \
     --resource-group skieshop-rg \
     --image acrskileshop.azurecr.io/inventory-service:latest
   ```

### サンプルデータ投入

初期テストデータを投入するためのスクリプト:

```bash
# MongoDB 初期データ投入
mongoimport --uri "mongodb://skieshop:skieshop@localhost:27017/inventory" \
  --collection products --file sample-data/products.json --jsonArray

# PostgreSQL 初期データ投入
psql -U skieshop -d skieshop_inventory -h localhost -f sample-data/inventory.sql
```

### 動作確認手順

1. ヘルスチェックエンドポイント確認
   ```bash
   curl http://localhost:8082/actuator/health
   ```

2. Swagger UI アクセス
   ```
   http://localhost:8082/swagger-ui.html
   ```

3. 基本 API 動作確認
   ```bash
   # 商品一覧取得
   curl -X GET "http://localhost:8082/api/products?page=0&size=10"
   
   # 商品詳細取得
   curl -X GET "http://localhost:8082/api/products/61f7c8a53e5c74a9a2f22b8b"
   
   # 在庫状況確認
   curl -X GET "http://localhost:8082/api/inventory/61f7c8a53e5c74a9a2f22b8b"
   ```

## 16. 障害対応ガイド

### 一般的な問題のトラブルシューティング

| 問題 | 考えられる原因 | 解決策 |
|-----|--------------|--------|
| MongoDB 接続エラー | 接続文字列の問題またはサーバーダウン | 接続文字列の確認、MongoDB サーバーの状態確認 |
| 画像アップロード失敗 | Blob Storage 接続の問題または権限不足 | ストレージ接続文字列と権限の確認 |
| 在庫の不整合 | 競合する更新または障害からの部分的な回復 | トランザクションログの確認、手動調整 |
| API タイムアウト | 大量データ処理または依存サービスの遅延 | クエリの最適化、キャッシュの活用 |
| イベント発行失敗 | Kafka への接続問題 | Kafka サーバーの状態確認、メッセージ再送メカニズム |

### ログ解析ガイド

主要なログパターンと対応方法:

- `ERROR c.s.i.exception.GlobalExceptionHandler`: 未処理例外の発生、スタックトレースを確認
- `ERROR c.s.i.service.ProductServiceImpl`: 商品サービスでの重大エラー
- `ERROR c.s.i.service.InventoryServiceImpl`: 在庫サービスでの重大エラー
- `WARN c.s.i.service.InventoryServiceImpl`: 在庫不足や予約失敗などの警告
- `INFO c.s.i.service.MediaServiceImpl`: 画像アップロード/削除の成功ログ

### 障害復旧手順

1. **データベース障害時**:
   - MongoDB: レプリカセットからの自動フェイルオーバー
   - PostgreSQL: スタンバイサーバーへのフェイルオーバー
   - 必要に応じてバックアップからリストア

2. **ストレージ障害時**:
   - 読み取り専用モードへの切り替え
   - CDN からの画像提供継続
   - Blob Storage の地理冗長レプリカへのフェイルオーバー

3. **在庫不整合時**:
   - 在庫確認スクリプトの実行
   - 注文データとの整合性検証
   - 必要に応じた手動調整

4. **性能低下時**:
   - スケールアウト/スケールアップ
   - キャッシュヒット率の確認と最適化
   - 不要なインデックスの確認と最適化

## 17. 将来拡張計画

### 短期的な拡張計画

- 動的な商品属性管理の強化
- サプライヤー管理機能の拡充
- 商品バリエーション（サイズ、カラー等）の体系化
- 商品レコメンデーションAPIの提供

### 中長期的な拡張計画

- リアルタイム在庫更新のためのWebSocketサポート
- 地理分散型の在庫管理（複数倉庫対応）
- バーコード/QRコードスキャン連携API
- 高度な商品検索（セマンティック検索、ファセット検索）
- 3Dモデルや拡張現実（AR）コンテンツのサポート
