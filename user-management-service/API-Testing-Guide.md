# User Management Service - API Testing Commands

このファイルは、Local-Evaluation.mdで検証したエンドポイントをOpenAPI形式で体系化したものです。

## 概要

- **サービス**: User Management Service
- **ベースURL**: http://localhost:8081/api
- **認証**: 無効化（Development Mode）
- **検証済み**: 全11エンドポイント

## Quick Start Testing Script

以下のコマンドを順番に実行することで、全エンドポイントの動作確認ができます：

```bash
#!/bin/bash

# Base URL
BASE_URL="http://localhost:8081/api"

echo "=== User Management Service API Testing ==="
echo

# 1. Health Check
echo "1. Health Check"
curl -s "$BASE_URL/actuator/health" | jq .
echo

# 2. Create User
echo "2. Create User"
USER_ID=$(curl -s -X POST "$BASE_URL/users" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser1",
    "email": "test1@example.com",
    "password": "test123456",
    "firstName": "Test1",
    "lastName": "User1"
  }' | jq -r '.id')
echo "Created User ID: $USER_ID"
echo

# 3. Get User by ID
echo "3. Get User by ID"
curl -s "$BASE_URL/users/$USER_ID" | jq .
echo

# 4. Check Email
echo "4. Check Email"
curl -s "$BASE_URL/users/check-email?email=test1@example.com" | jq .
echo

# 5. Update User
echo "5. Update User"
curl -s -X PUT "$BASE_URL/users/$USER_ID" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "updated1@example.com",
    "firstName": "Updated1",
    "lastName": "User1"
  }' | jq .
echo

# 6. Get Users (Admin)
echo "6. Get Users (Admin)"
curl -s "$BASE_URL/admin/users" | jq .
echo

# 7. Verify Email
echo "7. Verify Email"
curl -s -X POST "$BASE_URL/users/verify-email" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test1@example.com",
    "token": "sample-token"
  }'
echo "Email verification completed"
echo

# 8. Get User Activities
echo "8. Get User Activities"
curl -s "$BASE_URL/users/$USER_ID/activities" | jq .
echo

# 9. Get User Preferences
echo "9. Get User Preferences"
curl -s "$BASE_URL/users/$USER_ID/preferences" | jq .
echo

# 10. Get Roles (Admin)
echo "10. Get Roles (Admin)"
curl -s "$BASE_URL/admin/roles" | jq .
echo

# 11. Delete User
echo "11. Delete User"
curl -s -X DELETE "$BASE_URL/users/$USER_ID"
echo "User deleted"
echo

echo "=== All API tests completed ==="
```

## 個別コマンド一覧

### Health Check
```bash
curl -s http://localhost:8081/api/actuator/health | jq .
```

### User Management

#### Create User
```bash
curl -X POST http://localhost:8081/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser1",
    "email": "test1@example.com",
    "password": "test123456",
    "firstName": "Test1",
    "lastName": "User1"
  }' | jq .
```

#### Get User by ID
```bash
curl -s http://localhost:8081/api/users/{USER_ID} | jq .
```

#### Update User
```bash
curl -X PUT http://localhost:8081/api/users/{USER_ID} \
  -H "Content-Type: application/json" \
  -d '{
    "email": "updated1@example.com",
    "firstName": "Updated1",
    "lastName": "User1"
  }' | jq .
```

#### Delete User
```bash
curl -X DELETE http://localhost:8081/api/users/{USER_ID}
```

#### Check Email Existence
```bash
curl "http://localhost:8081/api/users/check-email?email=test1@example.com" | jq .
```

#### Verify Email
```bash
curl -X POST http://localhost:8081/api/users/verify-email \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test1@example.com",
    "token": "sample-token"
  }'
```

### User Data

#### Get User Activities
```bash
curl -s http://localhost:8081/api/users/{USER_ID}/activities | jq .
```

#### Get User Preferences
```bash
curl -s http://localhost:8081/api/users/{USER_ID}/preferences | jq .
```

### Admin Functions

#### Get All Users
```bash
curl http://localhost:8081/api/admin/users | jq .
```

#### Get All Roles
```bash
curl -s http://localhost:8081/api/admin/roles | jq .
```

## 使用方法

1. 上記のスクリプトを `test-api.sh` として保存
2. 実行権限を付与: `chmod +x test-api.sh`
3. 実行: `./test-api.sh`

または、個別のコマンドをコピーして実行してください。

## 注意事項

- `{USER_ID}` は実際のUser IDに置き換えてください
- `jq` がインストールされていない場合は、`| jq .` の部分を削除してください
- 認証機能がOFFのため、認証ヘッダーは不要です

## OpenAPI仕様書

詳細なAPI仕様は `openapi.yaml` ファイルを参照してください。このファイルは：
- Swagger UI での表示
- Postman への import
- API クライアントコードの自動生成

などに利用できます。
