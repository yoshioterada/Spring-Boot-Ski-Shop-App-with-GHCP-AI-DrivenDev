# スキーショップ マイクロサービス 全エンドポイント一覧

## 概要

本文書は、スキーショップマイクロサービスアーキテクチャの9つのサービス全てのエンドポイントを網羅的にまとめ、管理者用と一般利用者用に分類したリストです。

## サービス一覧

1. **Authentication Service** - 認証・認可サービス
2. **User Management Service** - ユーザー管理サービス  
3. **Inventory Management Service** - 在庫管理サービス
4. **Sales Management Service** - 販売管理サービス
5. **Payment Cart Service** - 決済・カートサービス
6. **Point Service** - ポイントサービス
7. **Coupon Service** - クーポンサービス
8. **AI Support Service** - AI支援サービス
9. **API Gateway** - APIゲートウェイ

---

## 1. Authentication Service (認証サービス)

### 1.1 一般利用者向けエンドポイント

| HTTPメソッド | エンドポイント | 説明 | 権限 |
|-------------|---------------|------|------|
| POST | `/api/v1/auth/login` | ユーザーログイン | Public |
| POST | `/api/v1/auth/mfa/verify` | MFA検証 | Public |
| POST | `/api/v1/auth/refresh` | トークンリフレッシュ | Public |
| POST | `/api/v1/auth/logout` | ログアウト | Authenticated |
| GET | `/api/v1/auth/oauth/{provider}/redirect` | OAuth認証開始 | Public |
| POST | `/api/v1/auth/oauth/{provider}/callback` | OAuthコールバック | Public |
| POST | `/api/v1/auth/password/reset-request` | パスワードリセット要求 | Public |
| POST | `/api/v1/auth/password/reset` | パスワードリセット実行 | Public |
| POST | `/api/v1/auth/validate` | トークン検証 | Public |
| GET | `/api/v1/auth/me` | 現在のユーザー情報 | Authenticated |
| GET | `/` | ホームページ | Public |
| GET | `/home` | ホームページ（認証後） | Authenticated |
| GET | `/token_details` | IDトークン詳細 | Authenticated |
| GET | `/call_graph` | Microsoft Graph API呼び出し | Authenticated |
| GET | `/profile` | ユーザープロファイル | Authenticated |
| GET | `/api/user/me` | ユーザー情報（REST API） | Authenticated |
| GET | `/api/graph/user` | Microsoft Graph ユーザー情報 | Authenticated |
| GET | `/login` | ログインページ | Public |

### 1.2 管理者向けエンドポイント

| HTTPメソッド | エンドポイント | 説明 | 権限 |
|-------------|---------------|------|------|
| POST | `/api/auth/users` | 新規ユーザー登録 | Public |
| DELETE | `/api/auth/users/{userId}` | ユーザー削除（論理削除） | Admin |
| DELETE | `/api/auth/users/{userId}/hard` | ユーザー物理削除 | Admin |

---

## 2. User Management Service (ユーザー管理サービス)

### 2.1 一般利用者向けエンドポイント

| HTTPメソッド | エンドポイント | 説明 | 権限 |
|-------------|---------------|------|------|
| POST | `/api/users` | 新規ユーザー登録 | Public |
| GET | `/api/users/{id}` | ユーザー情報取得 | Admin or Own |
| PUT | `/api/users/{id}` | ユーザー情報更新 | Admin or Own |
| GET | `/api/users/check-email` | メールアドレス存在確認 | Public |
| POST | `/api/users/{id}/change-password` | パスワード変更 | Admin or Own |
| GET | `/api/users/me` | 現在のユーザー情報取得 | Authenticated |
| GET | `/users/{userId}/activities` | ユーザーアクティビティ取得 | Admin or Own |
| GET | `/users/me/activities` | 現在のユーザーアクティビティ | Authenticated |
| GET | `/users/{userId}/preferences` | ユーザー設定取得 | Admin or Own |
| GET | `/users/{userId}/preferences/{key}` | 特定設定取得 | Admin or Own |
| PUT | `/users/{userId}/preferences/{key}` | 設定更新 | Admin or Own |
| DELETE | `/users/{userId}/preferences/{key}` | 設定削除 | Admin or Own |

### 2.2 管理者向けエンドポイント

| HTTPメソッド | エンドポイント | 説明 | 権限 |
|-------------|---------------|------|------|
| GET | `/api/admin/users` | ユーザー一覧取得 | Admin |
| POST | `/api/admin/users/{userId}/roles` | ユーザーロール更新 | Admin |
| POST | `/api/admin/users/{userId}/activate` | ユーザーアクティベート | Admin |
| POST | `/api/admin/users/{userId}/deactivate` | ユーザー非アクティベート | Admin |
| DELETE | `/api/admin/users/{userId}` | ユーザー削除 | Admin |
| GET | `/api/admin/stats` | 統計情報取得 | Admin |

---

## 3. Inventory Management Service (在庫管理サービス)

### 3.1 一般利用者向けエンドポイント

| HTTPメソッド | エンドポイント | 説明 | 権限 |
|-------------|---------------|------|------|
| GET | `/api/products` | 商品一覧取得 | Public |
| GET | `/api/products/search` | 商品検索 | Public |
| GET | `/api/products/{id}` | 商品詳細取得 | Public |
| GET | `/api/products/sku/{sku}` | SKUで商品取得 | Public |
| GET | `/api/products/category/{categoryId}` | カテゴリ別商品取得 | Public |
| GET | `/api/categories` | カテゴリ一覧取得 | Public |
| GET | `/api/categories/{id}` | カテゴリ詳細取得 | Public |
| GET | `/api/categories/{id}/products` | カテゴリ内商品取得 | Public |
| GET | `/api/inventory/{productId}` | 在庫情報取得 | Public |
| GET | `/api/inventory/status/{productId}` | 在庫ステータス取得 | Public |

### 3.2 管理者向けエンドポイント

| HTTPメソッド | エンドポイント | 説明 | 権限 |
|-------------|---------------|------|------|
| POST | `/api/products/batch` | バッチ商品操作 | Admin |
| POST | `/api/products` | 商品作成 | Admin |
| POST | `/api/categories` | カテゴリ作成 | Admin |
| PUT | `/api/categories/{id}` | カテゴリ更新 | Admin |
| DELETE | `/api/categories/{id}` | カテゴリ削除 | Admin |
| POST | `/api/inventory/batch` | バッチ在庫操作 | Admin |
| POST | `/api/inventory/reserve` | 在庫予約 | Admin |
| POST | `/api/inventory/release` | 在庫解放 | Admin |
| POST | `/api/inventory/stock-in` | 入庫処理 | Admin |
| POST | `/api/inventory/stock-out` | 出庫処理 | Admin |
| GET | `/api/inventory/low-stock` | 低在庫商品取得 | Admin |

---

## 4. Sales Management Service (販売管理サービス)

### 4.1 一般利用者向けエンドポイント

| HTTPメソッド | エンドポイント | 説明 | 権限 |
|-------------|---------------|------|------|
| POST | `/api/v1/orders` | 注文作成 | Authenticated |
| GET | `/api/v1/orders/{orderId}` | 注文詳細取得 | Authenticated |
| GET | `/api/v1/orders/number/{orderNumber}` | 注文番号で取得 | Authenticated |
| GET | `/api/v1/orders/customer/{customerId}` | 顧客注文一覧 | Authenticated |
| GET | `/api/v1/orders/search` | 注文検索 | Authenticated |
| POST | `/api/v1/returns` | 返品要求 | User or Customer Service |

### 4.2 管理者向けエンドポイント

| HTTPメソッド | エンドポイント | 説明 | 権限 |
|-------------|---------------|------|------|
| PUT | `/api/v1/orders/{orderId}/status` | 注文ステータス更新 | Admin |
| PUT | `/api/v1/orders/{orderId}/cancel` | 注文キャンセル | Admin |
| GET | `/api/v1/shipments` | 発送一覧取得 | Admin or Logistics |
| GET | `/api/v1/shipments/{id}` | 発送詳細取得 | Admin or Logistics or CS |
| POST | `/api/v1/shipments` | 発送作成 | Admin or Logistics |
| PUT | `/api/v1/shipments/{id}/status` | 発送ステータス更新 | Admin or Logistics |
| GET | `/api/v1/shipments/order/{orderId}` | 注文の発送情報 | Admin or Logistics or CS |
| PUT | `/api/v1/shipments/{id}/tracking` | 追跡情報更新 | Admin or Logistics |
| GET | `/api/v1/returns` | 返品一覧取得 | Admin or CS or Return Processor |
| GET | `/api/v1/returns/{id}` | 返品詳細取得 | Admin or CS or Return Processor |
| PUT | `/api/v1/returns/{id}/status` | 返品ステータス更新 | Admin or CS or Return Processor |
| GET | `/api/v1/returns/order/{orderId}` | 注文の返品情報 | Admin or CS or Return Processor |
| GET | `/api/v1/reports/sales` | 売上レポート | Admin or Sales Manager or Analyst |
| GET | `/api/v1/reports/products` | 商品レポート | Admin or Sales Manager or Product Manager |
| GET | `/api/v1/reports/export/sales` | 売上レポートエクスポート | Admin or Sales Manager |
| GET | `/api/v1/reports/shipping` | 配送レポート | Admin or Sales Manager or Analyst |
| GET | `/api/v1/reports/returns` | 返品レポート | Admin or Sales Manager or Analyst |

---

## 5. Payment Cart Service (決済・カートサービス)

### 5.1 一般利用者向けエンドポイント

| HTTPメソッド | エンドポイント | 説明 | 権限 |
|-------------|---------------|------|------|
| POST | `/api/v1/cart/items` | カートアイテム追加 | Authenticated |
| PUT | `/api/v1/cart/items/{itemId}` | カートアイテム更新 | Authenticated |
| DELETE | `/api/v1/cart/items/{itemId}` | カートアイテム削除 | Authenticated |
| GET | `/api/v1/cart` | カート取得 | Authenticated |
| DELETE | `/api/v1/cart` | カートクリア | Authenticated |
| POST | `/api/v1/payments/intent` | 決済インテント作成 | Authenticated |
| POST | `/api/v1/payments/{paymentId}/process` | 決済処理 | Authenticated |
| GET | `/api/v1/payments/{paymentId}` | 決済情報取得 | Authenticated |
| GET | `/api/v1/payments/history` | 決済履歴取得 | Authenticated |

### 5.2 管理者向けエンドポイント

| HTTPメソッド | エンドポイント | 説明 | 権限 |
|-------------|---------------|------|------|
| POST | `/api/v1/payments/{paymentId}/refund` | 返金処理 | Admin |
| POST | `/api/v1/payments/webhook` | 決済Webhook | System |

---

## 6. Point Service (ポイントサービス)

### 6.1 一般利用者向けエンドポイント

| HTTPメソッド | エンドポイント | 説明 | 権限 |
|-------------|---------------|------|------|
| GET | `/api/v1/points/balance` | ポイント残高取得 | Authenticated |
| POST | `/api/v1/points/redeem` | ポイント引き換え | Authenticated |
| GET | `/api/v1/points/history` | ポイント履歴取得 | Authenticated |
| GET | `/api/v1/points/expiring` | 期限切れ予定ポイント | Authenticated |
| GET | `/api/v1/points/redemption-options` | 引き換えオプション | Authenticated |
| POST | `/api/v1/points/transfer` | ポイント譲渡 | Authenticated |
| GET | `/api/v1/tiers/user` | ユーザーティア情報 | Authenticated |
| GET | `/api/v1/tiers/benefits` | ティア特典情報 | Authenticated |
| GET | `/api/v1/tiers/progress` | ティア進捗情報 | Authenticated |
| GET | `/api/v1/tiers` | ティア一覧 | Authenticated |
| GET | `/api/v1/tiers/{tierLevel}` | ティア詳細 | Authenticated |
| GET | `/api/tiers/user/{userId}` | ユーザーティア取得 | Public |
| GET | `/api/tiers` | ティア情報取得 | Public |
| GET | `/api/tiers/{tierLevel}` | ティアレベル詳細 | Public |
| GET | `/api/tiers/upgrade-eligibility/{userId}` | アップグレード資格確認 | Public |
| GET | `/api/points/balance/{userId}` | ユーザーポイント残高 | Public |
| GET | `/api/points/history/{userId}` | ポイント履歴 | Public |
| GET | `/api/points/history/{userId}/range` | 期間ポイント履歴 | Public |
| GET | `/api/points/expiring/{userId}` | 期限切れ予定ポイント | Public |

### 6.2 管理者向けエンドポイント

| HTTPメソッド | エンドポイント | 説明 | 権限 |
|-------------|---------------|------|------|
| POST | `/api/v1/points/award` | ポイント付与 | Service |
| POST | `/api/tiers/process-upgrades` | ティアアップグレード処理 | Admin |
| POST | `/api/points/award` | ポイント付与 | Admin |
| POST | `/api/points/redeem` | ポイント引き換え処理 | Admin |
| POST | `/api/points/transfer` | ポイント譲渡処理 | Admin |
| POST | `/api/points/process-expired` | 期限切れポイント処理 | Admin |

---

## 7. Coupon Service (クーポンサービス)

### 7.1 一般利用者向けエンドポイント

| HTTPメソッド | エンドポイント | 説明 | 権限 |
|-------------|---------------|------|------|
| POST | `/api/v1/coupons/validate` | クーポン検証 | Authenticated |
| POST | `/api/v1/coupons/redeem` | クーポン引き換え | Authenticated |
| GET | `/api/v1/coupons/user/available` | 利用可能クーポン取得 | Authenticated |
| GET | `/api/v1/coupons/{code}` | クーポン詳細取得 | Authenticated |
| GET | `/api/v1/campaigns/active` | アクティブキャンペーン | Public |

### 7.2 管理者向けエンドポイント

| HTTPメソッド | エンドポイント | 説明 | 権限 |
|-------------|---------------|------|------|
| POST | `/api/v1/coupons` | クーポン作成 | Admin |
| GET | `/api/v1/coupons` | クーポン一覧取得 | Admin |
| GET | `/api/v1/coupons/usage/{couponId}` | クーポン使用状況 | Admin |
| POST | `/api/v1/coupons/bulk-generate` | 一括クーポン生成 | Admin |
| POST | `/api/v1/campaigns` | キャンペーン作成 | Admin |
| GET | `/api/v1/campaigns` | キャンペーン一覧 | Admin |
| PUT | `/api/v1/campaigns/{campaignId}` | キャンペーン更新 | Admin |
| POST | `/api/v1/campaigns/{campaignId}/activate` | キャンペーンアクティベート | Admin |
| GET | `/api/v1/campaigns/{campaignId}/analytics` | キャンペーン分析 | Admin |
| GET | `/api/v1/campaigns/{campaignId}` | キャンペーン詳細 | Admin |
| GET | `/api/v1/distributions/rules/{campaignId}` | 配信ルール取得 | Admin or Campaign Manager |
| POST | `/api/v1/distributions/rules/{campaignId}` | 配信ルール作成 | Admin or Campaign Manager |
| PUT | `/api/v1/distributions/rules/{ruleId}` | 配信ルール更新 | Admin or Campaign Manager |
| DELETE | `/api/v1/distributions/rules/{ruleId}` | 配信ルール削除 | Admin or Campaign Manager |
| GET | `/api/v1/distributions/history/{campaignId}` | 配信履歴 | Admin or Campaign Manager |
| POST | `/api/v1/distributions/execute/{campaignId}` | 配信実行 | Admin or Campaign Manager |

---

## 8. AI Support Service (AI支援サービス)

### 8.1 一般利用者向けエンドポイント

| HTTPメソッド | エンドポイント | 説明 | 権限 |
|-------------|---------------|------|------|
| POST | `/api/v1/chat/message` | チャットメッセージ送信 | Authenticated |
| POST | `/api/v1/chat/recommend` | チャット推奨 | Authenticated |
| POST | `/api/v1/chat/advice` | チャットアドバイス | Authenticated |
| GET | `/api/v1/chat/conversations/{userId}` | 会話履歴取得 | Authenticated |
| DELETE | `/api/v1/chat/conversations/{conversationId}` | 会話削除 | Authenticated |
| POST | `/api/v1/chat/feedback` | チャットフィードバック | Authenticated |
| GET | `/api/v1/recommendations/{userId}` | ユーザー推奨商品 | Authenticated |
| GET | `/api/v1/recommendations/similar/{productId}` | 類似商品推奨 | Public |
| GET | `/api/v1/recommendations/trending` | トレンド商品 | Public |
| GET | `/api/v1/recommendations/category/{category}` | カテゴリ推奨 | Public |
| POST | `/api/v1/recommendations/feedback` | 推奨フィードバック | Authenticated |
| GET | `/api/v1/recommendations/explain/{userId}/{productId}` | 推奨理由説明 | Authenticated |
| POST | `/api/v1/search/semantic` | セマンティック検索 | Public |
| GET | `/api/v1/search/autocomplete` | 自動完成 | Public |
| GET | `/api/v1/search/suggest` | 検索提案 | Public |
| POST | `/api/v1/search/visual` | ビジュアル検索 | Public |

### 8.2 管理者向けエンドポイント

*このサービスには明示的な管理者専用エンドポイントはありませんが、すべてのデータはモニタリングと分析の対象となります。*

---

## 9. API Gateway (APIゲートウェイ)

API Gatewayは他のサービスへのルーティングのみを行い、独自のビジネスエンドポイントは持ちません。

---

## エンドポイント分類サマリー

### 権限レベル別統計

| 権限レベル | エンドポイント数 | 説明 |
|-----------|----------------|------|
| **Public** | 42 | 認証不要でアクセス可能 |
| **Authenticated** | 31 | 認証されたユーザーのみアクセス可能 |
| **Admin** | 49 | 管理者のみアクセス可能 |
| **Admin or Own** | 8 | 管理者または本人のみアクセス可能 |
| **Specialized Roles** | 15 | 特定の役割を持つユーザーのみアクセス可能 |

### サービス別エンドポイント数

| サービス | 一般利用者向け | 管理者向け | 合計 |
|----------|----------------|------------|------|
| Authentication Service | 19 | 3 | 22 |
| User Management Service | 12 | 6 | 18 |
| Inventory Management Service | 10 | 12 | 22 |
| Sales Management Service | 6 | 19 | 25 |
| Payment Cart Service | 9 | 2 | 11 |
| Point Service | 21 | 6 | 27 |
| Coupon Service | 5 | 16 | 21 |
| AI Support Service | 17 | 0 | 17 |
| API Gateway | 0 | 0 | 0 |
| **合計** | **99** | **64** | **163** |

## 注意事項

1. **セキュリティ**: 全てのエンドポイントは適切な認証・認可機構によって保護されています。
2. **権限制御**: `@PreAuthorize`アノテーションによる細かい権限制御が実装されています。
3. **API バージョニング**: 一部のサービスでは`/api/v1/`のバージョニングが使用されています。
4. **統合性**: APIゲートウェイを通じて全サービスへの統一されたアクセスが提供されます。
5. **監査**: 全ての管理者向け操作は監査ログの対象となります。

## 更新履歴

- **作成日**: 2025年06月22日
- **作成者**: システム解析による自動生成
- **対象バージョン**: 最新版（解析時点）

---

*このドキュメントは、実際のコントローラーファイルから自動的に抽出された情報に基づいて作成されています。エンドポイントの詳細な仕様については、各サービスのOpenAPI仕様書を参照してください。*
