# Authentication Service - ローカル検証結果

## 概要

authentication-serviceをローカルDocker環境で起動し、全エンドポイントの動作検証を実施します。
Azure Entra ID認証機能を無効化（Development Mode）での検証を行います。

## 起動方法

### 前提条件

- Docker Desktop がインストールされていること
- Docker Compose が利用可能であること

### 起動手順

1. **プロジェクトディレクトリに移動**

```bash
cd /path/to/authentication-service
```

2. **既存のコンテナを停止（必要に応じて）**

```bash
docker compose down
```

3. **イメージをビルド**

```bash
docker compose build
```

4. **サービスを起動**

```bash
docker compose up -d
```

5. **起動確認**

```bash
docker compose ps
```

### 認証機能設定

`docker-compose.yml`で以下の環境変数により認証機能を制御：

- `SKISHOP_AUTH_ENABLE: "false"` - 内部認証機能を無効化
- `SKISHOP_MFA_ENABLE: "false"` - 多要素認証を無効化
- Azure Entra ID関連設定を`disabled-for-local`に設定

## サービス構成

### コンテナ構成

| サービス名 | ポート | 説明 |
|-----------|--------|------|
| authentication-service | 8080 | 認証サービス（メイン） |
| auth-postgres | 5433 | PostgreSQLデータベース |
| auth-redis | 6380 | Redisキャッシュ |

### ネットワーク

- **Network Name**: `authentication-service-network`
- **Driver**: bridge

## エンドポイント検証

### 1. ヘルスチェック

**エンドポイント**: `GET /api/actuator/health`

```bash
curl -s http://localhost:8080/api/actuator/health | jq .
```

**期待レスポンス**:
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "redis": { "status": "UP" }
  }
}
```

### 2. 認証API

#### ログイン

**エンドポイント**: `POST /api/auth/login`

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "test@example.com",
    "password": "password123"
  }' | jq .
```

#### ログアウト

**エンドポイント**: `POST /api/auth/logout`

```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -H "Authorization: Bearer YOUR_TOKEN" | jq .
```

#### トークンリフレッシュ

**エンドポイント**: `POST /api/auth/refresh`

```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "YOUR_REFRESH_TOKEN"
  }' | jq .
```

#### トークン検証

**エンドポイント**: `GET /api/auth/validate`

```bash
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8080/api/auth/validate | jq .
```

### 3. パスワード管理

#### パスワードリセット要求

**エンドポイント**: `POST /api/auth/password/reset-request`

```bash
curl -X POST http://localhost:8080/api/auth/password/reset-request \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com"
  }' | jq .
```

#### パスワードリセット実行

**エンドポイント**: `POST /api/auth/password/reset`

```bash
curl -X POST http://localhost:8080/api/auth/password/reset \
  -H "Content-Type: application/json" \
  -d '{
    "token": "RESET_TOKEN",
    "newPassword": "newPassword123"
  }' | jq .
```

### 4. MFA（多要素認証）

#### MFA設定

**エンドポイント**: `POST /api/auth/mfa/setup`

```bash
curl -X POST http://localhost:8080/api/auth/mfa/setup \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" | jq .
```

#### MFA検証

**エンドポイント**: `POST /api/auth/mfa/verify`

```bash
curl -X POST http://localhost:8080/api/auth/mfa/verify \
  -H "Content-Type: application/json" \
  -d '{
    "sessionToken": "SESSION_TOKEN",
    "mfaCode": "123456"
  }' | jq .
```

### 5. OAuth2連携（Azure Entra ID）

#### OAuth2認証開始

**エンドポイント**: `GET /api/auth/oauth2/authorize/{provider}`

```bash
curl -X GET "http://localhost:8080/api/auth/oauth2/authorize/azure" | jq .
```

#### OAuth2コールバック

**エンドポイント**: `POST /api/auth/oauth2/callback/{provider}`

```bash
curl -X POST http://localhost:8080/api/auth/oauth2/callback/azure \
  -H "Content-Type: application/json" \
  -d '{
    "code": "AUTHORIZATION_CODE",
    "state": "STATE_VALUE"
  }' | jq .
```

## API文書

### Swagger UI

サービス起動後、以下のURLでAPI文書を確認できます：

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8080/api-docs

## ログ確認

### コンテナログの確認

```bash
# 認証サービスのログ
docker compose logs -f authentication-service

# PostgreSQLのログ
docker compose logs -f auth-postgres

# Redisのログ
docker compose logs -f auth-redis

# 全サービスのログ
docker compose logs -f
```

### アプリケーションログ

アプリケーションログは`./logs`ディレクトリにマウントされています。

## トラブルシューティング

### よくある問題

1. **データベース接続エラー**
   - PostgreSQLコンテナが正常に起動しているか確認
   - `docker compose logs auth-postgres`でログを確認

2. **Redis接続エラー**
   - Redisコンテナが正常に起動しているか確認
   - `docker compose logs auth-redis`でログを確認

3. **認証サービスが起動しない**
   - JVMヒープサイズの確認
   - 環境変数の設定確認
   - `docker compose logs authentication-service`でログを確認

### 設定の確認

```bash
# 環境変数の確認
docker compose exec authentication-service env | grep -E "(DB_|REDIS_|JWT_|AZURE_)"

# データベース接続テスト
docker compose exec auth-postgres psql -U auth_user -d skishop_auth -c "SELECT version();"

# Redis接続テスト
docker compose exec auth-redis redis-cli ping
```

## 停止方法

サービスを停止する場合：

```bash
docker compose down
```

ボリュームも含めて完全に削除する場合：

```bash
docker compose down -v
```

## 検証チェックリスト

- [ ] すべてのコンテナが正常に起動
- [ ] ヘルスチェックが成功
- [ ] データベース接続が確立
- [ ] Redis接続が確立
- [ ] 基本的な認証APIが動作
- [ ] Swagger UIでAPI文書が表示
- [ ] ログが正常に出力される

## 注意事項

- **ローカル開発専用**: 本設定はローカル開発環境専用です
- **セキュリティ**: Azure Entra ID認証は無効化されています
- **データ永続化**: PostgreSQLとRedisのデータはDockerボリュームに保存されます
- **ポート競合**: 他のサービスとのポート競合に注意してください

---

**最終更新**: 2025-06-20  
**検証環境**: Docker Desktop, macOS
