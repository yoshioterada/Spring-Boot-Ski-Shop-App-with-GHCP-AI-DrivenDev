#!/bin/bash

# User Management Service 全エンドポイント検証スクリプト

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

print_test() {
    echo -e "\033[35m[TEST]\033[0m $1"
}

# API ベースURL
BASE_URL="http://localhost:8081/api"

# テスト結果
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# テスト実行関数
run_test() {
    local test_name="$1"
    local method="$2"
    local endpoint="$3"
    local data="$4"
    local expected_status="$5"
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    print_test "$test_name"
    echo "  $method $endpoint"
    
    if [ -n "$data" ]; then
        echo "  Data: $data"
        response=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
          -X "$method" \
          -H "Content-Type: application/json" \
          -d "$data" \
          "$BASE_URL$endpoint" 2>/dev/null || echo "ERROR")
    else
        response=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
          -X "$method" \
          "$BASE_URL$endpoint" 2>/dev/null || echo "ERROR")
    fi
    
    if [ "$response" = "ERROR" ]; then
        print_error "  ❌ Connection failed"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        echo
        return 1
    fi
    
    http_code=$(echo "$response" | tail -n1 | cut -d: -f2)
    content=$(echo "$response" | sed '$d')
    
    if [ "$http_code" = "$expected_status" ]; then
        print_success "  ✅ HTTP $http_code (Expected: $expected_status)"
        PASSED_TESTS=$((PASSED_TESTS + 1))
        if [ -n "$content" ] && [ "$content" != "" ]; then
            echo "$content" | jq '.' 2>/dev/null | head -5 || echo "  Response: $content" | head -5
        fi
    else
        print_error "  ❌ HTTP $http_code (Expected: $expected_status)"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        if [ -n "$content" ] && [ "$content" != "" ]; then
            echo "  Error: $content"
        fi
    fi
    echo
}

# グローバル変数（テスト間で共有）
USER_ID=""
ROLE_ID=""

echo
print_info "=============================================="
print_info "User Management Service 全エンドポイント検証"
print_info "=============================================="
echo

# 1. ヘルスチェック・管理エンドポイント
print_info "=== 1. ヘルスチェック・管理エンドポイント ==="
run_test "ヘルスチェック" "GET" "/actuator/health" "" "200"
run_test "アプリケーション情報" "GET" "/actuator/info" "" "200"
run_test "メトリクス" "GET" "/actuator/metrics" "" "200"

# 2. ユーザー管理エンドポイント
print_info "=== 2. ユーザー管理エンドポイント ==="

# メールアドレス重複チェック
run_test "メールアドレス重複チェック（存在しない）" "GET" "/users/check-email?email=test@example.com" "" "200"

# ユーザー作成
user_data='{
  "username": "testuser",
  "email": "test@example.com",
  "firstName": "Test",
  "lastName": "User",
  "password": "password123"
}'
print_test "ユーザー作成"
echo "  POST /users"
echo "  Data: $user_data"
response=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
  -X POST \
  -H "Content-Type: application/json" \
  -d "$user_data" \
  "$BASE_URL/users" 2>/dev/null || echo "ERROR")

if [ "$response" != "ERROR" ]; then
    http_code=$(echo "$response" | tail -n1 | cut -d: -f2)
    content=$(echo "$response" | sed '$d')
    
    if [ "$http_code" = "201" ] || [ "$http_code" = "200" ]; then
        print_success "  ✅ HTTP $http_code"
        PASSED_TESTS=$((PASSED_TESTS + 1))
        USER_ID=$(echo "$content" | jq -r '.id' 2>/dev/null || echo "")
        echo "$content" | jq '.' 2>/dev/null | head -5 || echo "  Response: $content" | head -5
        print_info "  Created User ID: $USER_ID"
    else
        print_error "  ❌ HTTP $http_code"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        echo "  Error: $content"
    fi
else
    print_error "  ❌ Connection failed"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi
TOTAL_TESTS=$((TOTAL_TESTS + 1))
echo

# メールアドレス重複チェック（存在する）
run_test "メールアドレス重複チェック（存在する）" "GET" "/users/check-email?email=test@example.com" "" "200"

# ユーザー一覧取得
run_test "ユーザー一覧取得" "GET" "/users" "" "200"

# 個別ユーザー取得
if [ -n "$USER_ID" ] && [ "$USER_ID" != "null" ] && [ "$USER_ID" != "" ]; then
    run_test "個別ユーザー取得" "GET" "/users/$USER_ID" "" "200"
    
    # ユーザー更新
    update_data='{
      "firstName": "Updated",
      "lastName": "User",
      "email": "test@example.com"
    }'
    run_test "ユーザー更新" "PUT" "/users/$USER_ID" "$update_data" "200"
else
    print_warning "ユーザーIDが取得できないため、個別ユーザー関連のテストをスキップします"
fi

# 現在のユーザー情報取得（認証無効時はモックデータ）
run_test "現在のユーザー情報取得" "GET" "/users/me" "" "200"

# パスワード変更
password_data='{
  "currentPassword": "password123",
  "newPassword": "newpassword123"
}'
run_test "パスワード変更" "PUT" "/users/me/password" "$password_data" "200"

# メール認証関連
verification_data='{"token": "dummy-token"}'
run_test "メール認証" "POST" "/users/verify-email" "$verification_data" "400"

resend_data='{"email": "test@example.com"}'
run_test "認証メール再送" "POST" "/users/resend-verification" "$resend_data" "200"

# 3. ユーザー設定エンドポイント
print_info "=== 3. ユーザー設定エンドポイント ==="

if [ -n "$USER_ID" ] && [ "$USER_ID" != "null" ] && [ "$USER_ID" != "" ]; then
    # ユーザー設定一覧取得
    run_test "ユーザー設定一覧取得" "GET" "/users/$USER_ID/preferences" "" "200"
    
    # 個別設定取得
    run_test "個別設定取得" "GET" "/users/$USER_ID/preferences/language" "" "200"
    
    # 設定更新
    pref_data='{"value": "ja"}'
    run_test "設定更新" "PUT" "/users/$USER_ID/preferences/language" "$pref_data" "200"
    
    # 設定削除
    run_test "設定削除" "DELETE" "/users/$USER_ID/preferences/language" "" "200"
else
    print_warning "ユーザーIDが取得できないため、ユーザー設定関連のテストをスキップします"
fi

# 4. ユーザーアクティビティエンドポイント
print_info "=== 4. ユーザーアクティビティエンドポイント ==="

if [ -n "$USER_ID" ] && [ "$USER_ID" != "null" ] && [ "$USER_ID" != "" ]; then
    # ユーザーアクティビティ取得
    run_test "ユーザーアクティビティ取得" "GET" "/users/$USER_ID/activities" "" "200"
else
    print_warning "ユーザーIDが取得できないため、ユーザーアクティビティ関連のテストをスキップします"
fi

# 現在のユーザーアクティビティ取得
run_test "現在のユーザーアクティビティ取得" "GET" "/users/me/activities" "" "200"

# 5. 管理者エンドポイント
print_info "=== 5. 管理者エンドポイント ==="

# 管理者用ユーザー一覧
run_test "管理者用ユーザー一覧" "GET" "/admin/users" "" "200"

# ロール一覧取得
run_test "ロール一覧取得" "GET" "/admin/roles" "" "200"

# ロール作成
role_data='{
  "name": "TEST_ROLE",
  "description": "Test role for validation"
}'
print_test "ロール作成"
echo "  POST /admin/roles"
echo "  Data: $role_data"
response=$(curl -s -w "\nHTTP_CODE:%{http_code}" \
  -X POST \
  -H "Content-Type: application/json" \
  -d "$role_data" \
  "$BASE_URL/admin/roles" 2>/dev/null || echo "ERROR")

if [ "$response" != "ERROR" ]; then
    http_code=$(echo "$response" | tail -n1 | cut -d: -f2)
    content=$(echo "$response" | sed '$d')
    
    if [ "$http_code" = "201" ] || [ "$http_code" = "200" ]; then
        print_success "  ✅ HTTP $http_code"
        PASSED_TESTS=$((PASSED_TESTS + 1))
        ROLE_ID=$(echo "$content" | jq -r '.id' 2>/dev/null || echo "")
        echo "$content" | jq '.' 2>/dev/null | head -5 || echo "  Response: $content" | head -5
        print_info "  Created Role ID: $ROLE_ID"
    else
        print_error "  ❌ HTTP $http_code"
        FAILED_TESTS=$((FAILED_TESTS + 1))
        echo "  Error: $content"
    fi
else
    print_error "  ❌ Connection failed"
    FAILED_TESTS=$((FAILED_TESTS + 1))
fi
TOTAL_TESTS=$((TOTAL_TESTS + 1))
echo

# ロール更新・削除
if [ -n "$ROLE_ID" ] && [ "$ROLE_ID" != "null" ] && [ "$ROLE_ID" != "" ]; then
    # ロール更新
    role_update_data='{
      "name": "UPDATED_TEST_ROLE",
      "description": "Updated test role"
    }'
    run_test "ロール更新" "PUT" "/admin/roles/$ROLE_ID" "$role_update_data" "200"
    
    # ロール削除
    run_test "ロール削除" "DELETE" "/admin/roles/$ROLE_ID" "" "200"
else
    print_warning "ロールIDが取得できないため、ロール更新・削除のテストをスキップします"
fi

# ユーザーへのロール付与
if [ -n "$USER_ID" ] && [ "$USER_ID" != "null" ] && [ "$USER_ID" != "" ]; then
    user_role_data='{"roleNames": ["USER"]}'
    run_test "ユーザーへのロール付与" "POST" "/admin/users/$USER_ID/roles" "$user_role_data" "200"
    
    # ユーザーステータス更新
    status_data='{"status": "ACTIVE"}'
    run_test "ユーザーステータス更新" "PUT" "/admin/users/$USER_ID/status" "$status_data" "200"
else
    print_warning "ユーザーIDが取得できないため、ユーザーロール・ステータス関連のテストをスキップします"
fi

# 6. 存在しないリソースのテスト
print_info "=== 6. エラーケーステスト ==="
run_test "存在しないユーザー取得" "GET" "/users/00000000-0000-0000-0000-000000000000" "" "404"
run_test "不正なUUID形式" "GET" "/users/invalid-uuid" "" "400"
run_test "存在しないエンドポイント" "GET" "/nonexistent" "" "404"

# 7. Swagger UI テスト
print_info "=== 7. API ドキュメントテスト ==="
run_test "Swagger UI" "GET" "/swagger-ui.html" "" "200"
run_test "OpenAPI JSON" "GET" "/api-docs" "" "200"

# ユーザー削除（クリーンアップ）
if [ -n "$USER_ID" ] && [ "$USER_ID" != "null" ] && [ "$USER_ID" != "" ]; then
    print_info "=== クリーンアップ ==="
    run_test "ユーザー削除" "DELETE" "/users/$USER_ID" "" "200"
fi

# 結果サマリー
echo
print_info "=============================================="
print_info "検証結果サマリー"
print_info "=============================================="
echo "総テスト数: $TOTAL_TESTS"
echo "成功: $PASSED_TESTS"
echo "失敗: $FAILED_TESTS"
echo

if [ $FAILED_TESTS -eq 0 ]; then
    print_success "🎉 全てのテストが成功しました！"
    exit 0
else
    print_warning "⚠️  $FAILED_TESTS 個のテストが失敗しました"
    echo "成功率: $(( PASSED_TESTS * 100 / TOTAL_TESTS ))%"
    exit 1
fi
