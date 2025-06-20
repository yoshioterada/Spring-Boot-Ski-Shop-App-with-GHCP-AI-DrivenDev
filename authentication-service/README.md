# Authentication Service

スキーショップ電子商取引プラットフォームの認証・認可マイクロサービス

## 概要

このサービスは以下の機能を提供します：

- ユーザー認証（ログイン/ログアウト）
- JWTトークン生成と検証
- OAuth統合（Azure Entra ID、Google、Facebook、LINE）
- 多要素認証（MFA）
- セッション管理とセキュリティ
- パスワード管理とリセット

## 技術スタック

- **言語**: Java 21
- **フレームワーク**: Spring Boot 3.2.3
- **データベース**: PostgreSQL 15
- **キャッシュ**: Redis 7
- **認証**: JWT, OAuth2, Azure Entra ID
- **ビルドツール**: Maven
- **コンテナ**: Docker

## クイックスタート

### ローカル開発環境

1. **リポジトリのクローン**
```bash
git clone <repository-url>
cd authentication-service
```

2. **Docker Composeで起動**
```bash
docker compose up -d
```

3. **ヘルスチェック**
```bash
curl http://localhost:8080/api/actuator/health
```

### 開発モード

```bash
# Maven依存関係のインストール
mvn clean install

# 開発サーバーの起動
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

## 設定

### 環境変数

| 変数名 | 説明 | デフォルト値 |
|--------|------|-------------|
| `DB_URL` | データベース接続URL | `jdbc:postgresql://localhost:5432/skishop_auth` |
| `DB_USERNAME` | データベースユーザー名 | `auth_user` |
| `DB_PASSWORD` | データベースパスワード | `auth_password` |
| `REDIS_HOST` | Redisホスト | `localhost` |
| `REDIS_PORT` | Redisポート | `6379` |
| `JWT_SECRET` | JWT署名キー | 自動生成 |
| `AZURE_TENANT_ID` | Azure テナントID | - |
| `AZURE_CLIENT_ID` | Azure クライアントID | - |
| `AZURE_CLIENT_SECRET` | Azure クライアントシークレット | - |

### プロファイル

- `local`: ローカル開発環境（Azure AD無効）
- `dev`: 開発環境
- `staging`: ステージング環境
- `prod`: 本番環境

## API仕様

### エンドポイント一覧

| エンドポイント | メソッド | 説明 |
|---------------|----------|------|
| `/api/auth/login` | POST | ユーザーログイン |
| `/api/auth/logout` | POST | ユーザーログアウト |
| `/api/auth/refresh` | POST | トークンリフレッシュ |
| `/api/auth/validate` | GET | トークン検証 |
| `/api/auth/password/reset-request` | POST | パスワードリセット要求 |
| `/api/auth/password/reset` | POST | パスワードリセット実行 |
| `/api/auth/mfa/setup` | POST | MFA設定 |
| `/api/auth/mfa/verify` | POST | MFA検証 |
| `/api/auth/oauth2/authorize/{provider}` | GET | OAuth2認証開始 |
| `/api/auth/oauth2/callback/{provider}` | POST | OAuth2コールバック |

### API文書

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8080/api-docs

## アーキテクチャ

```
authentication-service/
├── src/main/java/com/skishop/auth/
│   ├── config/          # 設定クラス
│   ├── controller/      # REST コントローラー
│   ├── dto/            # データ転送オブジェクト
│   ├── entity/         # JPA エンティティ
│   ├── exception/      # 例外処理
│   ├── repository/     # データアクセス層
│   ├── service/        # ビジネスロジック
│   └── util/           # ユーティリティ
├── src/main/resources/
│   ├── application.yml
│   ├── application-local.yml
│   └── db/migration/   # Flyway マイグレーション
└── src/test/           # テストコード
```

## セキュリティ

### 認証方式

1. **JWT認証**: アクセストークンとリフレッシュトークン
2. **OAuth2**: Azure Entra ID、Google、Facebook、LINE
3. **MFA**: TOTP（Time-based One-Time Password）

### セキュリティ機能

- パスワードハッシュ化（BCrypt）
- アカウントロック機能
- レート制限
- セッション管理
- セキュリティヘッダー

## テスト

```bash
# 単体テストの実行
mvn test

# 統合テストの実行
mvn verify

# カバレッジレポートの生成
mvn jacoco:report
```

## 監視とログ

### ヘルスチェック

```bash
curl http://localhost:8080/api/actuator/health
```

### メトリクス

- Prometheus: http://localhost:8080/api/actuator/prometheus
- Metrics: http://localhost:8080/api/actuator/metrics

### ログレベル

```yaml
logging:
  level:
    com.skishop.auth: DEBUG
    org.springframework.security: INFO
```

## デプロイメント

### Docker

```bash
# イメージのビルド
docker build -t authentication-service .

# コンテナの実行
docker run -p 8080:8080 authentication-service
```

### Docker Compose

```bash
# 起動
docker compose up -d

# 停止
docker compose down

# ログ確認
docker compose logs -f
```

## 開発ガイドライン

### コーディング規約

- Google Java Style Guide
- Spring Boot Best Practices
- Security First Approach

### Git ワークフロー

1. feature ブランチで開発
2. PR作成時に自動テスト実行
3. レビュー後にmainブランチにマージ

## トラブルシューティング

### よくある問題

1. **データベース接続エラー**
   - PostgreSQLが起動しているか確認
   - 接続情報が正しいか確認

2. **Redis接続エラー**
   - Redisサーバーが起動しているか確認
   - ネットワーク設定を確認

3. **Azure AD認証エラー**
   - テナントIDとクライアントIDを確認
   - 権限設定を確認

### ログの確認

```bash
# アプリケーションログ
tail -f logs/application.log

# エラーログ
tail -f logs/error.log

# Dockerログ
docker compose logs -f authentication-service
```

## ライセンス

このプロジェクトは商用利用のために開発されています。

## 貢献

プルリクエストや課題報告を歓迎します。詳細は CONTRIBUTING.md を参照してください。

## サポート

技術的な質問やサポートが必要な場合は、開発チームにお問い合わせください。
