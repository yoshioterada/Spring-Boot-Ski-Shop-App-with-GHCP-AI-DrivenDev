# User Management Service - ローカル開発環境

User Management Serviceを単体でローカルDocker環境で動作させるための設定とスクリプトです。

## 🎯 特徴

- **フィーチャーフラグによる認証機能切り替え**
  - `SKISHOP_AUTH_ENABLE=true`: 認証機能有効（本番相当）
  - `SKISHOP_AUTH_ENABLE=false`: 認証機能無効（開発用・推奨）

- **完全なローカル環境**
  - PostgreSQL データベース
  - Redis キャッシュ
  - pgAdmin（オプション）

- **簡単起動・停止**
  - ワンクリック起動・停止スクリプト
  - 設定の対話的選択

## 🚀 クイックスタート

### 1. 環境起動

```bash
# 対話的に設定を選択して起動
./start-local.sh
```

### 2. API テスト

```bash
# 基本的なAPIテストを実行
./test-api.sh
```

### 3. 環境停止

```bash
# 対話的に停止オプションを選択
./stop-local.sh
```

## 📋 詳細手順

### 環境設定

1. **環境変数ファイルの準備**
   ```bash
   # テンプレートから .env ファイルを作成
   cp .env.example .env
   
   # 必要に応じて設定を編集
   vim .env
   ```

2. **認証機能の設定**
   ```bash
   # 開発用（認証無効）
   SKISHOP_AUTH_ENABLE=false
   
   # 本番相当（認証有効）
   SKISHOP_AUTH_ENABLE=true
   ```

### 手動起動

```bash
# Maven ビルド
mvn clean package -DskipTests

# 認証無効でDocker Compose起動
SKISHOP_AUTH_ENABLE=false docker-compose up -d

# 管理ツール込みで起動
SKISHOP_AUTH_ENABLE=false docker-compose --profile admin-tools up -d
```

### サービス確認

```bash
# サービス状態確認
docker-compose ps

# ログ確認
docker-compose logs -f user-management-service

# ヘルスチェック
curl http://localhost:8081/api/actuator/health
```

## 🌐 エンドポイント

### アプリケーション

| サービス | URL | 説明 |
|---------|-----|------|
| User Management API | http://localhost:8081/api | メインAPI |
| Swagger UI | http://localhost:8081/api/swagger-ui.html | APIドキュメント |
| Health Check | http://localhost:8081/api/actuator/health | ヘルスチェック |
| Metrics | http://localhost:8081/api/actuator/metrics | メトリクス |

### インフラストラクチャ

| サービス | URL/接続情報 | 認証情報 |
|---------|-------------|---------|
| PostgreSQL | localhost:5432 | user: `skishop_user`, pass: `password` |
| Redis | localhost:6379 | パスワードなし |
| pgAdmin | http://localhost:5050 | `admin@skishop.com` / `admin` |

## 📝 API使用例

### 認証無効時（開発用）

```bash
# ユーザー一覧取得
curl http://localhost:8081/api/users

# ユーザー作成
curl -X POST http://localhost:8081/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "firstName": "Test",
    "lastName": "User",
    "password": "password123"
  }'

# 個別ユーザー取得
curl http://localhost:8081/api/users/{userId}
```

### 認証有効時（本番相当）

```bash
# JWTトークンが必要
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  http://localhost:8081/api/users
```

## 🔧 開発Tips

### 認証機能の切り替え

```bash
# .envファイルを編集
echo "SKISHOP_AUTH_ENABLE=false" > .env

# サービス再起動
docker-compose restart user-management-service
```

### データベース操作

```bash
# PostgreSQLコンソールアクセス
docker-compose exec postgres psql -U skishop_user -d skishop_user

# テーブル確認
\dt

# データ確認
SELECT * FROM users;
```

### ログ確認

```bash
# リアルタイムログ
docker-compose logs -f user-management-service

# エラーログのみ
docker-compose logs user-management-service | grep ERROR

# 特定の文字列を検索
docker-compose logs user-management-service | grep "Authentication"
```

### パフォーマンス確認

```bash
# メトリクス取得
curl http://localhost:8081/api/actuator/metrics | jq

# JVMメトリクス
curl http://localhost:8081/api/actuator/metrics/jvm.memory.used | jq

# HTTPリクエストメトリクス
curl http://localhost:8081/api/actuator/metrics/http.server.requests | jq
```

## 🐛 トラブルシューティング

### よくある問題

1. **ポート衝突**
   ```bash
   # ポート使用状況確認
   lsof -i :8081
   lsof -i :5432
   lsof -i :6379
   
   # 別のポートを使用する場合
   # docker-compose.yml の ports セクションを編集
   ```

2. **データベース接続エラー**
   ```bash
   # PostgreSQL接続確認
   docker-compose exec postgres pg_isready -U skishop_user -d skishop_user
   
   # コンテナ再起動
   docker-compose restart postgres
   ```

3. **Redis接続エラー**
   ```bash
   # Redis接続確認
   docker-compose exec redis redis-cli ping
   
   # Redisコンソールアクセス
   docker-compose exec redis redis-cli
   ```

4. **アプリケーション起動失敗**
   ```bash
   # 詳細ログ確認
   docker-compose logs user-management-service
   
   # JVMメモリ不足の場合
   # .env で JAVA_OPTS を調整
   JAVA_OPTS=-Xmx1g -Xms512m
   ```

### データリセット

```bash
# 全データ削除
docker-compose down -v

# イメージ再ビルド
docker-compose build --no-cache user-management-service

# クリーンスタート
./start-local.sh
```

## 📚 設定ファイル説明

### docker-compose.yml
- PostgreSQL, Redis, User Management Serviceの定義
- ヘルスチェック、依存関係の設定
- ネットワーク、ボリュームの定義

### .env / .env.example
- 環境変数の設定テンプレート
- 認証フィーチャーフラグの設定
- データベース、Redis接続情報

### application-local.yml
- ローカル開発用のSpring設定
- 認証無効時のデフォルト設定
- 開発用のゆるい制約設定

### config/SecurityConfig.java
- フィーチャーフラグに基づくセキュリティ設定
- 認証有効/無効の動的切り替え

## 🔄 CI/CDとの統合

```bash
# テスト実行（認証無効）
SKISHOP_AUTH_ENABLE=false docker-compose up -d
mvn test
docker-compose down

# テスト実行（認証有効）
SKISHOP_AUTH_ENABLE=true docker-compose up -d
mvn test -Dtest.auth.enabled=true
docker-compose down
```

## 📖 関連ドキュメント

- [User Management Service API仕様](./docs/api.md)
- [認証機能詳細](./docs/authentication.md)
- [データベーススキーマ](./docs/database.md)
