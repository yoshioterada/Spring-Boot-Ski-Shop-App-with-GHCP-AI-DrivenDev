#!/bin/bash

# User Management Service API テストスクリプト

set -e

# 色付きメッセージ用の関数
print_info() {
    echo -e "\033[36m[INFO]\033[0m $1"
}

print_success() {
    echo -e "\033[32m[SUCCESS]\033[0m $1"
}

print_warning() {
    echo -e "\033[33m[WARNING]\033[0m $1"
}

print_error() {
    echo -e "\033[31m[ERROR]\033[0m $1"
}

# API ベースURL
BASE_URL="http://localhost:8081/api"

echo
print_info "==================================="
print_info "User Management Service API テスト"
print_info "==================================="

# ヘルスチェック
print_info "1. ヘルスチェック"
echo "GET $BASE_URL/actuator/health"
response=$(curl -s -w "\nHTTP_CODE:%{http_code}" $BASE_URL/actuator/health)
http_code=$(echo "$response" | tail -n1 | cut -d: -f2)
content=$(echo "$response" | sed '$d')

if [ "$http_code" = "200" ]; then
    print_success "ヘルスチェック成功"
    echo "$content" | jq '.' 2>/dev/null || echo "$content"
else
    print_error "ヘルスチェック失敗 (HTTP $http_code)"
    echo "$content"
    exit 1
fi

echo
print_info "2. ユーザー一覧取得"
echo "GET $BASE_URL/users"
response=$(curl -s -w "\nHTTP_CODE:%{http_code}" $BASE_URL/users)
http_code=$(echo "$response" | tail -n1 | cut -d: -f2)
content=$(echo "$response" | sed '$d')

if [ "$http_code" = "200" ]; then
    print_success "ユーザー一覧取得成功"
    echo "$content" | jq '.' 2>/dev/null || echo "$content"
else
    print_warning "ユーザー一覧取得失敗 (HTTP $http_code)"
    echo "$content"
fi

echo
print_info "3. ユーザー作成テスト"
user_data='{
  "username": "testuser",
  "email": "test@example.com",
  "firstName": "Test",
  "lastName": "User",
  "password": "password123"
}'

echo "POST $BASE_URL/users"
echo "データ: $user_data"
response=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
  -X POST \
  -H "Content-Type: application/json" \
  -d "$user_data" \
  $BASE_URL/users)
http_code=$(echo "$response" | tail -n1 | cut -d: -f2)
content=$(echo "$response" | sed '$d')

if [ "$http_code" = "201" ] || [ "$http_code" = "200" ]; then
    print_success "ユーザー作成成功"
    user_id=$(echo "$content" | jq -r '.id' 2>/dev/null)
    echo "$content" | jq '.' 2>/dev/null || echo "$content"
else
    print_warning "ユーザー作成失敗 (HTTP $http_code)"
    echo "$content"
fi

# ユーザーIDが取得できた場合、個別ユーザー取得をテスト
if [ ! -z "$user_id" ] && [ "$user_id" != "null" ]; then
    echo
    print_info "4. 個別ユーザー取得テスト"
    echo "GET $BASE_URL/users/$user_id"
    response=$(curl -s -w "\nHTTP_CODE:%{http_code}" $BASE_URL/users/$user_id)
    http_code=$(echo "$response" | tail -n1 | cut -d: -f2)
    content=$(echo "$response" | sed '$d')

    if [ "$http_code" = "200" ]; then
        print_success "個別ユーザー取得成功"
        echo "$content" | jq '.' 2>/dev/null || echo "$content"
    else
        print_warning "個別ユーザー取得失敗 (HTTP $http_code)"
        echo "$content"
    fi
fi

echo
print_info "5. Swagger UI アクセステスト"
echo "GET $BASE_URL/swagger-ui.html"
response=$(curl -s -w "\nHTTP_CODE:%{http_code}" $BASE_URL/swagger-ui.html)
http_code=$(echo "$response" | tail -n1 | cut -d: -f2)

if [ "$http_code" = "200" ]; then
    print_success "Swagger UI アクセス成功"
    print_info "ブラウザで http://localhost:8081/api/swagger-ui.html にアクセスしてAPIドキュメントを確認できます"
else
    print_warning "Swagger UI アクセス失敗 (HTTP $http_code)"
fi

echo
print_success "==================================="
print_success "API テスト完了"
print_success "==================================="
echo
print_info "追加のテストコマンド:"
echo "  • 全エンドポイント: curl http://localhost:8081/api/actuator/mappings | jq '.contexts.application.mappings.dispatcherServlets.dispatcherServlet[].predicate'"
echo "  • メトリクス: curl http://localhost:8081/api/actuator/metrics"
echo "  • 設定情報: curl http://localhost:8081/api/actuator/configprops"
