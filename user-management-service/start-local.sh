#!/bin/bash

# User Management Service ローカル開発環境起動スクリプト

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

# 環境設定ファイルの確認
if [ ! -f .env ]; then
    print_warning ".env ファイルが見つかりません。テンプレートからコピーします。"
    cp .env.example .env
    print_info ".env ファイルを作成しました。必要に応じて設定を変更してください。"
fi

# 認証機能の有効/無効を選択
echo
print_info "認証機能の設定を選択してください："
echo "1) 認証機能を無効にする（開発用・推奨）"
echo "2) 認証機能を有効にする（本番相当）"
echo

read -p "選択してください [1-2]: " auth_choice

case $auth_choice in
    1)
        AUTH_ENABLE="false"
        print_info "認証機能を無効にして起動します（開発用）"
        ;;
    2)
        AUTH_ENABLE="true"
        print_warning "認証機能を有効にして起動します（JWT認証が必要）"
        ;;
    *)
        AUTH_ENABLE="false"
        print_info "デフォルト：認証機能を無効にして起動します"
        ;;
esac

# Docker Composeの起動オプションを選択
echo
print_info "起動オプションを選択してください："
echo "1) 基本サービスのみ起動（PostgreSQL + Redis + User Service）"
echo "2) 管理ツール込みで起動（+ pgAdmin）"
echo

read -p "選択してください [1-2]: " startup_choice

case $startup_choice in
    1)
        COMPOSE_PROFILES=""
        print_info "基本サービスのみで起動します"
        ;;
    2)
        COMPOSE_PROFILES="--profile admin-tools"
        print_info "管理ツール込みで起動します"
        ;;
    *)
        COMPOSE_PROFILES=""
        print_info "デフォルト：基本サービスのみで起動します"
        ;;
esac

# Maven ビルド
print_info "Mavenビルドを実行します..."
if ! mvn clean package -DskipTests; then
    print_error "Mavenビルドに失敗しました"
    exit 1
fi

# 既存のコンテナを停止・削除
print_info "既存のコンテナを停止・削除します..."
docker-compose down -v

# 環境変数を設定してDocker Compose起動
print_info "Docker Composeでサービスを起動します..."
SKISHOP_AUTH_ENABLE=$AUTH_ENABLE docker-compose up -d $COMPOSE_PROFILES

# サービスの起動確認
print_info "サービスの起動を確認しています..."
sleep 10

# ヘルスチェック
print_info "ヘルスチェックを実行します..."

# PostgreSQL確認
if docker-compose exec -T postgres pg_isready -U skishop_user -d skishop_user > /dev/null 2>&1; then
    print_success "PostgreSQL: 正常稼働中"
else
    print_error "PostgreSQL: 起動していません"
fi

# Redis確認
if docker-compose exec -T redis redis-cli ping > /dev/null 2>&1; then
    print_success "Redis: 正常稼働中"
else
    print_error "Redis: 起動していません"
fi

# User Management Service確認
sleep 30  # アプリケーション起動待ち
if curl -f http://localhost:8081/api/actuator/health > /dev/null 2>&1; then
    print_success "User Management Service: 正常稼働中"
else
    print_warning "User Management Service: まだ起動中かもしれません"
    print_info "ログを確認してください: docker-compose logs -f user-management-service"
fi

# 起動完了メッセージ
echo
print_success "==================================="
print_success "User Management Service 起動完了"
print_success "==================================="
echo
print_info "サービスエンドポイント:"
echo "  • User Management API: http://localhost:8081/api"
echo "  • Swagger UI: http://localhost:8081/api/swagger-ui.html"
echo "  • Health Check: http://localhost:8081/api/actuator/health"
echo "  • PostgreSQL: localhost:5432"
echo "  • Redis: localhost:6379"

if [[ $startup_choice == "2" ]]; then
    echo "  • pgAdmin: http://localhost:5050 (admin@skishop.com / admin)"
fi

echo
print_info "認証機能設定: SKISHOP_AUTH_ENABLE=$AUTH_ENABLE"

if [[ $AUTH_ENABLE == "false" ]]; then
    print_warning "認証機能が無効になっています。全てのAPIエンドポイントに認証なしでアクセスできます。"
else
    print_warning "認証機能が有効になっています。認証が必要なエンドポイントには適切なJWTトークンが必要です。"
fi

echo
print_info "便利なコマンド:"
echo "  • ログ確認: docker-compose logs -f user-management-service"
echo "  • サービス停止: docker-compose down"
echo "  • サービス再起動: docker-compose restart user-management-service"
echo "  • 認証設定変更: .env ファイルの SKISHOP_AUTH_ENABLE を編集後、docker-compose restart user-management-service"
