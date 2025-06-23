# Azure SkiShop Frontend Service

Azure SkiShop のフロントエンドサービスです。顧客向けWebサイトと管理者向け管理画面を提供します。

## 概要

- **Java 21** + **Spring Boot 3.2** + **Thymeleaf** で構築
- 薄い青色ベースの一般利用者向けデザイン
- 薄い赤色ベースの管理者向けデザイン
- レスポンシブデザイン対応
- Azure Entra ID OAuth 2.0 認証対応

## 主な機能

### 一般利用者向け
- 商品閲覧・検索
- カート機能
- 注文機能
- ユーザープロフィール管理
- ポイント・クーポン確認
- AI相談チャット

### 管理者向け
- ダッシュボード
- 商品管理
- 注文管理
- ユーザー管理
- 在庫管理
- レポート機能

## 技術スタック

- **Java**: 21 LTS
- **Spring Boot**: 3.2.x
- **Thymeleaf**: 3.1.x
- **Bootstrap**: 5.3.x
- **WebClient**: バックエンドAPI通信
- **Spring Security**: 認証・認可
- **Caffeine Cache**: キャッシング

## 開発環境セットアップ

### 前提条件
- Java 21+
- Maven 3.9+
- Docker & Docker Compose

### ローカル実行

1. **依存関係のインストール**
```bash
mvn clean install
```

2. **アプリケーション起動**
```bash
mvn spring-boot:run
```

3. **ブラウザでアクセス**
```
http://localhost:8080
```

### Docker Compose での実行

1. **コンテナビルド・起動**
```bash
docker-compose up --build
```

2. **ブラウザでアクセス**
```
http://localhost:8080
```

## 設定

### 環境変数

| 変数名 | 説明 | デフォルト値 |
|--------|------|-------------|
| `API_GATEWAY_URL` | APIゲートウェイのURL | `http://localhost:8080` |
| `SKISHOP_AUTH_ENABLED` | 認証機能の有効/無効 | `true` |
| `AZURE_CLIENT_ID` | Azure Entra ID クライアントID | - |
| `AZURE_CLIENT_SECRET` | Azure Entra ID クライアントシークレット | - |
| `AZURE_TENANT_ID` | Azure Entra ID テナントID | - |

### プロファイル

- **local**: ローカル開発用（認証無効）
- **production**: 本番用（認証有効）

## API連携

本サービスは以下のバックエンドAPIと連携します：

- Authentication Service
- User Management Service  
- Inventory Management Service
- Sales Management Service
- Payment Cart Service
- Point Service
- Coupon Service
- AI Support Service

## 画面構成

### 一般利用者向け画面
- `/` - ホーム画面
- `/products` - 商品一覧
- `/products/{id}` - 商品詳細
- `/search` - 検索結果
- `/cart` - カート
- `/checkout` - 決済
- `/profile` - プロフィール
- `/orders` - 注文履歴
- `/points` - ポイント
- `/coupons` - クーポン

### 管理者向け画面
- `/admin` - ダッシュボード
- `/admin/products` - 商品管理
- `/admin/orders` - 注文管理
- `/admin/users` - ユーザー管理
- `/admin/inventory` - 在庫管理
- `/admin/reports` - レポート

## テスト

### 単体テスト実行
```bash
mvn test
```

### 統合テスト実行
```bash
mvn verify
```

## デプロイメント

### Azure Container Apps

1. **Dockerイメージビルド**
```bash
docker build -t frontend-service:latest .
```

2. **Azure Container Registryにプッシュ**
```bash
az acr login --name your-registry
docker tag frontend-service:latest your-registry.azurecr.io/frontend-service:latest
docker push your-registry.azurecr.io/frontend-service:latest
```

3. **Container Appsデプロイ**
```bash
az containerapp up \
  --name frontend-service \
  --image your-registry.azurecr.io/frontend-service:latest \
  --environment-variables \
    SPRING_PROFILES_ACTIVE=production \
    API_GATEWAY_URL=https://your-api-gateway.azure.com
```

## 監視・ログ

### ヘルスチェック
```
GET /actuator/health
```

### メトリクス
```
GET /actuator/metrics
GET /actuator/prometheus
```

### ログレベル
- 開発環境: DEBUG
- 本番環境: INFO

## トラブルシューティング

### よくある問題

1. **APIとの接続エラー**
   - `API_GATEWAY_URL` の設定を確認
   - ネットワーク接続を確認

2. **認証エラー**
   - Azure Entra ID の設定を確認
   - クライアントID・シークレットを確認

3. **パフォーマンス問題**
   - キャッシュ設定を確認
   - JVMメモリ設定を調整

## 開発者向け情報

### ディレクトリ構造
```
src/
├── main/
│   ├── java/com/skishop/frontend/
│   │   ├── config/          # 設定クラス
│   │   ├── controller/      # コントローラー
│   │   ├── service/         # サービスクラス
│   │   ├── dto/             # データ転送オブジェクト
│   │   └── FrontendServiceApplication.java
│   └── resources/
│       ├── templates/       # Thymeleafテンプレート
│       ├── static/          # 静的ファイル
│       └── application.yml
└── test/                    # テストコード
```

### コーディング規約
- Java 21の最新機能を活用
- Spring Boot ベストプラクティスに従う
- レスポンシブデザインの実装
- アクセシビリティ対応

## ライセンス

このプロジェクトはMITライセンスの下で公開されています。

## 貢献

プルリクエストやイシュー報告を歓迎します。

## 更新履歴

- **v1.0.0** (2025-06-23): 初回リリース
  - 基本的なフロントエンド機能実装
  - 一般利用者向け・管理者向け画面
  - Azure認証対応
