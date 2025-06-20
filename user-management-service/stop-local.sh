#!/bin/bash

# User Management Service ローカル開発環境停止スクリプト

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

echo
print_info "User Management Service ローカル環境を停止します..."

# 停止オプションを選択
echo
print_info "停止オプションを選択してください："
echo "1) サービスのみ停止（データは保持）"
echo "2) サービス停止 + データ削除（完全クリーンアップ）"
echo

read -p "選択してください [1-2]: " stop_choice

case $stop_choice in
    1)
        print_info "サービスを停止します（データは保持）..."
        docker-compose down
        print_success "サービスを停止しました"
        ;;
    2)
        print_warning "サービスを停止し、全てのデータを削除します..."
        read -p "本当にデータを削除しますか？ [y/N]: " confirm
        if [[ $confirm =~ ^[Yy]$ ]]; then
            docker-compose down -v --remove-orphans
            docker system prune -f
            print_success "サービスを停止し、データを削除しました"
        else
            print_info "キャンセルしました"
            exit 0
        fi
        ;;
    *)
        print_info "デフォルト：サービスのみ停止します"
        docker-compose down
        print_success "サービスを停止しました"
        ;;
esac

echo
print_success "==================================="
print_success "User Management Service 停止完了"
print_success "==================================="
echo
print_info "再起動する場合は以下を実行してください："
echo "  ./start-local.sh"
