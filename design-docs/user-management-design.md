# ユーザー管理サービス 詳細設計書

## 1. 概要

ユーザー管理サービスは、ユーザー登録、認証、プロファイル管理、権限管理などの機能を提供するマイクロサービスです。ユーザーのアカウント情報、設定、アクティビティを管理し、他のマイクロサービスがユーザーデータにアクセスするための統一的なインターフェースを提供します。

## 2. 技術スタック

### 開発環境
- **言語**: Java 21 (LTS)
- **フレームワーク**: Spring Boot 3.2.3
- **ビルドツール**: Maven 3.9.x
- **コンテナ化**: Docker 25.x
- **テスト**: JUnit 5.10.1、Spring Boot Test、Testcontainers 1.19.3

### 本番環境
- Azure Container Apps
- Azure Database for PostgreSQL

### 主要ライブラリとバージョン
| ライブラリ | バージョン | 用途 |
|----------|----------|------|
| spring-boot-starter-data-jpa | 3.2.3 | JPA データアクセス |
| spring-boot-starter-web | 3.2.3 | REST API エンドポイント |
| spring-boot-starter-validation | 3.2.3 | 入力バリデーション |
| spring-boot-starter-security | 3.2.3 | セキュリティ設定 |
| spring-boot-starter-actuator | 3.2.3 | ヘルスチェック、メトリクス |
| spring-cloud-starter-stream-kafka | 4.1.0 | イベント発行 |
| spring-boot-starter-cache | 3.2.3 | キャッシュ機能 |
| spring-boot-starter-data-redis | 3.2.3 | Redis キャッシュ |
| hibernate-core | 6.4.1 | ORM マッピング |
| postgresql | 42.7.1 | PostgreSQL JDBC ドライバ |
| querydsl-jpa | 5.0.0 | 動的クエリビルダー |
| flyway-core | 9.22.3 | データベースマイグレーション |
| mapstruct | 1.5.5.Final | オブジェクトマッピング |
| lombok | 1.18.30 | ボイラープレートコード削減 |
| micrometer-registry-prometheus | 1.12.2 | メトリクス収集 |
| springdoc-openapi-starter-webmvc-ui | 2.3.0 | API 文書化 |
| azure-identity | 1.11.1 | Azure 認証 |
| azure-security-keyvault-secrets | 4.6.2 | Azure Key Vault 連携 |
| azure-monitor-opentelemetry | 1.0.0-beta.15 | Azure 監視連携 |
| logback-json-classic | 0.1.5 | JSON 形式ログ出力 |

## 3. システム構成

### コンポーネント構成図

```mermaid
graph TB
    subgraph "ユーザー管理サービス"
        direction TB
        API[REST API Controller] --> SERV[User Service]
        SERV --> REPO[User Repository]
        SERV --> ROLE_SERV[Role Service]
        SERV --> PREF_SERV[Preference Service]
        SERV --> ACT_SERV[Activity Service]
        
        ROLE_SERV --> ROLE_REPO[Role Repository]
        PREF_SERV --> PREF_REPO[Preference Repository]
        ACT_SERV --> ACT_REPO[Activity Repository]
        
        REPO --> DB[(PostgreSQL)]
        ROLE_REPO --> DB
        PREF_REPO --> DB
        ACT_REPO --> DB
        
        CACHE[Cache Service] --> REDIS[(Redis)]
        SERV --> CACHE
        
        EVENT[Event Publisher] --> KAFKA[Kafka]
        SERV --> EVENT
        
        VALID[Validation Component]
        SERV --> VALID
    end
    
    %% 外部システムとの接続
    API --> AUTH[認証サービス]
```

### クラス構成

#### 主要クラス

**コントローラー層**
- `UserController`: ユーザー関連の REST API エンドポイント
- `RoleController`: ロール管理 API エンドポイント
- `PreferenceController`: ユーザー設定 API エンドポイント
- `ActivityController`: ユーザーアクティビティ API エンドポイント
- `AdminUserController`: 管理者向けユーザー管理 API

**サービス層**
- `UserService`: ユーザー関連のビジネスロジック
- `RoleService`: ロール管理ロジック
- `PreferenceService`: ユーザー設定管理ロジック
- `ActivityService`: ユーザーアクティビティ記録ロジック
- `CacheService`: キャッシュ管理
- `EventPublisherService`: イベント発行

**リポジトリ層**
- `UserRepository`: ユーザーデータアクセス
- `RoleRepository`: ロールデータアクセス
- `PreferenceRepository`: 設定データアクセス
- `ActivityRepository`: アクティビティデータアクセス

**モデル**
- `User`: ユーザーエンティティ
- `Role`: ロールエンティティ
- `Permission`: 権限エンティティ
- `UserPreference`: ユーザー設定エンティティ
- `UserActivity`: ユーザーアクティビティエンティティ

**DTO**
- `UserDTO`: ユーザーデータ転送オブジェクト
- `RoleDTO`: ロールデータ転送オブジェクト
- `PreferenceDTO`: 設定データ転送オブジェクト
- `ActivityDTO`: アクティビティデータ転送オブジェクト

**マッパー**
- `UserMapper`: エンティティと DTO 間のマッピング
- `RoleMapper`: ロールエンティティと DTO 間のマッピング
- `PreferenceMapper`: 設定エンティティと DTO 間のマッピング
- `ActivityMapper`: アクティビティエンティティと DTO 間のマッピング

**設定**
- `SecurityConfig`: セキュリティ設定
- `CacheConfig`: キャッシュ設定
- `KafkaConfig`: Kafka 設定
- `WebConfig`: Web 関連設定

### データベース設計

#### ER 図

```mermaid
erDiagram
    User ||--o{ UserPreference : "has"
    User ||--o{ UserActivity : "generates"
    User }o--|| Role : "has"
    Role }o--o{ Permission : "has"
    
    User {
        UUID id PK
        String email
        String passwordHash
        String firstName
        String lastName
        String phoneNumber
        Date birthDate
        String gender
        String status
        Timestamp createdAt
        Timestamp updatedAt
        Boolean emailVerified
        Boolean phoneVerified
        UUID roleId FK
    }
    
    Role {
        UUID id PK
        String name
        String description
        Timestamp createdAt
        Timestamp updatedAt
    }
    
    Permission {
        UUID id PK
        String name
        String description
        String resource
        String action
        Timestamp createdAt
        Timestamp updatedAt
    }
    
    RolePermission {
        UUID roleId PK,FK
        UUID permissionId PK,FK
        Timestamp createdAt
    }
    
    UserPreference {
        UUID id PK
        UUID userId FK
        String prefKey
        String prefValue
        String prefType
        Timestamp createdAt
        Timestamp updatedAt
    }
    
    UserActivity {
        UUID id PK
        UUID userId FK
        String activityType
        String description
        JSON metadata
        Timestamp createdAt
        String ipAddress
        String userAgent
    }
```

## 4. API 設計

### RESTful API エンドポイント

#### ユーザー管理 API

| メソッド | パス | 説明 | 認証要件 |
|--------|-----|------|----------|
| POST | /api/users | 新規ユーザー登録 | 不要 |
| GET | /api/users/{id} | ユーザー情報取得 | 要認証 |
| PUT | /api/users/{id} | ユーザー情報更新 | 要認証（本人または管理者） |
| DELETE | /api/users/{id} | ユーザー削除 | 要認証（本人または管理者） |
| GET | /api/users/me | 自分のユーザー情報取得 | 要認証 |
| PUT | /api/users/me/password | パスワード変更 | 要認証 |
| POST | /api/users/verify-email | メールアドレス検証 | 要トークン |
| POST | /api/users/resend-verification | 検証メール再送信 | 不要 |
| GET | /api/users/check-email | メールアドレス存在確認 | 不要 |

#### ユーザー設定 API

| メソッド | パス | 説明 | 認証要件 |
|--------|-----|------|----------|
| GET | /api/users/{id}/preferences | ユーザー設定一覧取得 | 要認証（本人または管理者） |
| GET | /api/users/{id}/preferences/{key} | 特定設定取得 | 要認証（本人または管理者） |
| PUT | /api/users/{id}/preferences/{key} | 設定更新 | 要認証（本人または管理者） |
| DELETE | /api/users/{id}/preferences/{key} | 設定削除 | 要認証（本人または管理者） |

#### ユーザーアクティビティ API

| メソッド | パス | 説明 | 認証要件 |
|--------|-----|------|----------|
| GET | /api/users/{id}/activities | ユーザーアクティビティ一覧取得 | 要認証（本人または管理者） |
| GET | /api/users/me/activities | 自分のアクティビティ一覧取得 | 要認証 |

#### 管理者向け API

| メソッド | パス | 説明 | 認証要件 |
|--------|-----|------|----------|
| GET | /api/admin/users | ユーザー一覧取得 | 要認証（管理者） |
| POST | /api/admin/users/{id}/roles | ユーザーロール更新 | 要認証（管理者） |
| PUT | /api/admin/users/{id}/status | ユーザーステータス更新 | 要認証（管理者） |
| GET | /api/admin/roles | ロール一覧取得 | 要認証（管理者） |
| POST | /api/admin/roles | 新規ロール作成 | 要認証（管理者） |
| PUT | /api/admin/roles/{id} | ロール更新 | 要認証（管理者） |
| DELETE | /api/admin/roles/{id} | ロール削除 | 要認証（管理者） |

### API リクエスト・レスポンス例

#### ユーザー登録

**リクエスト**

```http
POST /api/users HTTP/1.1
Content-Type: application/json

{
  "email": "yamada.taro@example.com",
  "password": "P@ssw0rd123",
  "firstName": "太郎",
  "lastName": "山田",
  "phoneNumber": "090-1234-5678",
  "birthDate": "1990-01-01",
  "gender": "MALE"
}
```

**レスポンス**

```http
HTTP/1.1 201 Created
Content-Type: application/json

{
  "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "email": "yamada.taro@example.com",
  "firstName": "太郎",
  "lastName": "山田",
  "phoneNumber": "090-1234-5678",
  "birthDate": "1990-01-01",
  "gender": "MALE",
  "status": "PENDING_VERIFICATION",
  "emailVerified": false,
  "phoneVerified": false,
  "createdAt": "2025-06-19T10:30:00Z"
}
```

#### ユーザー情報取得

**リクエスト**

```http
GET /api/users/me HTTP/1.1
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**レスポンス**

```http
HTTP/1.1 200 OK
Content-Type: application/json

{
  "id": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "email": "yamada.taro@example.com",
  "firstName": "太郎",
  "lastName": "山田",
  "phoneNumber": "090-1234-5678",
  "birthDate": "1990-01-01",
  "gender": "MALE",
  "status": "ACTIVE",
  "emailVerified": true,
  "phoneVerified": true,
  "role": {
    "id": "e47ac10b-58cc-4372-a567-0e02b2c3d123",
    "name": "CUSTOMER"
  },
  "createdAt": "2025-06-19T10:30:00Z",
  "updatedAt": "2025-06-19T11:15:00Z"
}
```

## 5. イベント設計

### 発行イベント

| イベント名 | 説明 | ペイロード |
|-----------|------|-----------|
| user.created | ユーザー作成時 | ユーザーID、メール、名前、作成日時 |
| user.updated | ユーザー情報更新時 | ユーザーID、更新フィールド、更新日時 |
| user.deleted | ユーザー削除時 | ユーザーID、削除日時 |
| user.verified | メール検証完了時 | ユーザーID、メール、検証日時 |
| user.login | ユーザーログイン時 | ユーザーID、ログイン日時、IPアドレス |
| user.password_changed | パスワード変更時 | ユーザーID、変更日時 |
| user.role_changed | ロール変更時 | ユーザーID、旧ロール、新ロール、変更日時 |

### 購読イベント

| イベント名 | 説明 | アクション |
|-----------|------|----------|
| order.completed | 注文完了時 | ユーザーアクティビティ記録 |
| payment.completed | 支払い完了時 | ユーザーアクティビティ記録 |
| product.viewed | 商品閲覧時 | ユーザーアクティビティ記録 |

### イベントスキーマ例

**user.created イベント**

```json
{
  "eventId": "2b5fed9a-3ef0-4c82-bffd-f6cf591d7520",
  "eventType": "user.created",
  "timestamp": "2025-06-19T10:30:00Z",
  "producer": "user-service",
  "payload": {
    "userId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
    "email": "yamada.taro@example.com",
    "firstName": "太郎",
    "lastName": "山田",
    "createdAt": "2025-06-19T10:30:00Z",
    "status": "PENDING_VERIFICATION"
  }
}
```

## 6. セキュリティ設計

### 認証・認可

- JWT トークンベース認証
- ロールベースアクセス制御（RBAC）
- Spring Security による認可フィルター

### パスワード管理

- BCrypt によるパスワードハッシュ化（コスト係数 12）
- パスワード強度ポリシー（最小 8 文字、大文字、小文字、数字、特殊文字を含む）
- パスワードリセットフロー（期限付きトークン）

### データ保護

- 転送中のデータは TLS 1.3 で暗号化
- 個人情報は Azure Key Vault で管理された鍵で暗号化
- データベースへのアクセスは最小権限の原則に基づく

### GDPR 対応

- 個人データ削除リクエスト対応機能
- データエクスポート機能
- 監査ログ記録

## 7. エラー処理

### エラーコード設計

| エラーコード | HTTP ステータス | 説明 |
|------------|----------------|------|
| USER_001 | 400 | 不正なリクエスト形式 |
| USER_002 | 400 | バリデーションエラー |
| USER_003 | 409 | メールアドレス重複 |
| USER_004 | 404 | ユーザーが見つからない |
| USER_005 | 401 | 認証エラー |
| USER_006 | 403 | 権限不足 |
| USER_007 | 400 | パスワード強度不足 |
| USER_008 | 400 | 検証コード無効または期限切れ |
| USER_009 | 429 | リクエスト頻度制限超過 |
| USER_010 | 500 | 内部サーバーエラー |

### エラーレスポンス形式

```json
{
  "timestamp": "2025-06-19T10:35:00Z",
  "status": 400,
  "error": "Bad Request",
  "code": "USER_002",
  "message": "Validation failed",
  "details": [
    {
      "field": "email",
      "message": "有効なメールアドレスを入力してください"
    },
    {
      "field": "password",
      "message": "パスワードは8文字以上で、大文字、小文字、数字を含む必要があります"
    }
  ],
  "path": "/api/users"
}
```

### 例外ハンドリング

- `@RestControllerAdvice` による集中的な例外ハンドリング
- カスタム例外クラス階層
- 詳細なログ記録（機密情報を除く）

## 8. パフォーマンス最適化

### キャッシュ戦略

- Redis を使用した分散キャッシュ
- Spring Cache アノテーションによるキャッシュ管理
- 頻繁に読み取られる情報のキャッシュ（ユーザープロファイル、ロール、権限）
- キャッシュの有効期限設定

### データベース最適化

- インデックス設計
  - ユーザーテーブル: email, status
  - アクティビティテーブル: userId, createdAt
  - 設定テーブル: userId, prefKey
- 適切なページネーション実装
- クエリの最適化（Explain 分析）

### 負荷テスト基準

- 通常時 500 リクエスト/秒の処理
- ピーク時 2,000 リクエスト/秒の処理
- レスポンスタイム 95% が 200ms 以下
- CPU 使用率 80% 以下

## 9. 監視・ロギング

### メトリクス

- アクティブユーザー数
- リクエスト処理時間
- エラーレート
- キャッシュヒット率
- API エンドポイント使用率

### ログ

- 構造化ログ（JSON 形式）
- ログレベル: INFO（本番）、DEBUG（開発/テスト）
- 重要な操作の監査ログ
- PII（個人識別情報）のマスキング

### アラート

- エラーレート閾値超過
- レスポンスタイム閾値超過
- リソース使用率閾値超過
- 連続認証失敗

## 10. テスト戦略

### 単体テスト

- サービス層のビジネスロジックテスト
- リポジトリ層のデータアクセステスト
- コントローラー層の入力検証テスト
- モックとスタブを使用した依存コンポーネント分離

### 統合テスト

- Testcontainers を使用した実際のデータベースとの統合テスト
- Kafka との統合テスト
- Redis との統合テスト
- 認証サービスとの統合テスト

### 契約テスト

- Spring Cloud Contract による API 契約テスト
- 他のマイクロサービスとの連携検証

### 負荷テスト

- Gatling を使用した負荷テスト
- エンドポイントごとのパフォーマンス検証
- キャッシュ効果の検証

### テストカバレッジ目標

- 単体テスト: コード網羅率 85% 以上
- 統合テスト: 主要フロー 100% カバー
- 契約テスト: 全ての公開 API をカバー

## 11. デプロイメント

### Docker コンテナ化

**Dockerfile**

```dockerfile
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY build/libs/user-service.jar /app/app.jar

ENV JAVA_OPTS="-Xms512m -Xmx1024m"

EXPOSE 8081

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

### Kubernetes/Azure Container Apps デプロイメント設定

**deployment.yaml**

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: user-service
  labels:
    app: user-service
spec:
  replicas: 2
  selector:
    matchLabels:
      app: user-service
  template:
    metadata:
      labels:
        app: user-service
    spec:
      containers:
      - name: user-service
        image: ${CONTAINER_REGISTRY}/user-service:${IMAGE_TAG}
        ports:
        - containerPort: 8081
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: SPRING_DATASOURCE_URL
          valueFrom:
            secretKeyRef:
              name: user-service-secrets
              key: db-url
        - name: SPRING_DATASOURCE_USERNAME
          valueFrom:
            secretKeyRef:
              name: user-service-secrets
              key: db-username
        - name: SPRING_DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: user-service-secrets
              key: db-password
        - name: SPRING_KAFKA_BOOTSTRAP_SERVERS
          value: "kafka-service:9092"
        - name: SPRING_REDIS_HOST
          value: "redis-service"
        resources:
          limits:
            cpu: "1"
            memory: "1Gi"
          requests:
            cpu: "0.5"
            memory: "512Mi"
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8081
          initialDelaySeconds: 30
          periodSeconds: 10
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8081
          initialDelaySeconds: 60
          periodSeconds: 20
```

### ローカル開発環境セットアップ

**docker-compose.yml での設定**

```yaml
version: '3.8'

services:
  user-service:
    build:
      context: ./user-service
      dockerfile: Dockerfile.dev
    ports:
      - "8081:8081"
    depends_on:
      - postgres
      - redis
      - kafka
    environment:
      SPRING_PROFILES_ACTIVE: dev
      SERVER_PORT: 8081
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/skieshop_users
      SPRING_DATASOURCE_USERNAME: skieshop
      SPRING_DATASOURCE_PASSWORD: skieshop
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:29092
      SPRING_REDIS_HOST: redis
      SPRING_REDIS_PORT: 6379
    volumes:
      - ./user-service:/app
      - maven-repo:/root/.m2
    networks:
      - skieshop-network
```

### 環境変数設定

| 環境変数 | 説明 | デフォルト値 |
|---------|------|------------|
| SERVER_PORT | サービスポート | 8081 |
| SPRING_PROFILES_ACTIVE | アクティブプロファイル | dev |
| SPRING_DATASOURCE_URL | PostgreSQL 接続 URL | jdbc:postgresql://localhost:5432/skieshop_users |
| SPRING_DATASOURCE_USERNAME | データベースユーザー | skieshop |
| SPRING_DATASOURCE_PASSWORD | データベースパスワード | (必須) |
| SPRING_KAFKA_BOOTSTRAP_SERVERS | Kafka サーバー | localhost:9092 |
| SPRING_REDIS_HOST | Redis ホスト | localhost |
| SPRING_REDIS_PORT | Redis ポート | 6379 |
| LOGGING_LEVEL_ROOT | ルートログレベル | INFO |
| LOGGING_LEVEL_COM_SKIESHOP | アプリケーションログレベル | INFO |
| TOKEN_SECRET | トークン署名キー | (必須) |
| TOKEN_EXPIRATION | トークン有効期間（秒） | 86400 |

## 12. 運用・保守

### バックアップ戦略

- PostgreSQL データベースの日次フルバックアップ
- WAL アーカイブによる Point-in-Time リカバリ対応
- バックアップデータの暗号化と安全な保管

### データマイグレーション

- Flyway によるバージョン管理されたデータベースマイグレーション
- ゼロダウンタイムマイグレーション手順
- ロールバック計画

### スケーリング戦略

- 水平スケーリング: レプリカ数増加
- 垂直スケーリング: コンテナリソース割り当て増加
- 自動スケーリング設定（CPU 使用率 70% 超過時）

### 障害対応

- 障害検知: ヘルスチェック、メトリクス監視
- 復旧手順: リトライメカニズム、サーキットブレーカー
- フォールバック戦略: キャッシュからの提供、静的応答

### メンテナンス手順

- 計画的メンテナンスウィンドウ
- ローリングアップデート
- カナリアデプロイメント

## 13. 開発ガイドライン

### コーディング規約

- Google Java スタイルガイド準拠
- Checkstyle、SpotBugs による静的解析
- SonarQube によるコード品質チェック

### API 開発ガイドライン

- リソース命名規則: 複数形の名詞（例: /users, /roles）
- HTTP メソッド使用規則
- クエリパラメータ形式
- ページネーション実装
- エラーレスポンス形式

### ドキュメント

- JavaDoc によるコードドキュメント
- OpenAPI/Swagger によるAPI ドキュメント
- README による環境構築手順
- 運用マニュアル

## 14. 実装参考コード

### エンティティ定義例

```java
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(nullable = false)
    private String passwordHash;
    
    @Column(nullable = false)
    private String firstName;
    
    @Column(nullable = false)
    private String lastName;
    
    private String phoneNumber;
    
    private LocalDate birthDate;
    
    @Enumerated(EnumType.STRING)
    private Gender gender;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.PENDING_VERIFICATION;
    
    @Column(nullable = false)
    private boolean emailVerified = false;
    
    private boolean phoneVerified = false;
    
    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role role;
    
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    
    private Instant updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
    
    public enum Gender {
        MALE, FEMALE, OTHER, PREFER_NOT_TO_SAY
    }
    
    public enum UserStatus {
        PENDING_VERIFICATION, ACTIVE, SUSPENDED, DEACTIVATED
    }
}
```

### コントローラー実装例

```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "User management APIs")
public class UserController {
    
    private final UserService userService;
    private final UserMapper userMapper;
    
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new user")
    public UserDTO registerUser(@Valid @RequestBody UserRegistrationDTO registrationDTO) {
        User user = userService.registerUser(registrationDTO);
        return userMapper.toDto(user);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get user by ID")
    public UserDTO getUserById(@PathVariable UUID id) {
        User user = userService.getUserById(id);
        return userMapper.toDto(user);
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Update user information")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isCurrentUser(#id)")
    public UserDTO updateUser(@PathVariable UUID id, @Valid @RequestBody UserUpdateDTO updateDTO) {
        User user = userService.updateUser(id, updateDTO);
        return userMapper.toDto(user);
    }
    
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a user")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isCurrentUser(#id)")
    public void deleteUser(@PathVariable UUID id) {
        userService.deleteUser(id);
    }
    
    @GetMapping("/me")
    @Operation(summary = "Get current user information")
    public UserDTO getCurrentUser() {
        User user = userService.getCurrentUser();
        return userMapper.toDto(user);
    }
    
    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Change password")
    public void changePassword(@Valid @RequestBody PasswordChangeDTO passwordChangeDTO) {
        userService.changePassword(passwordChangeDTO);
    }
    
    @PostMapping("/verify-email")
    @Operation(summary = "Verify email address")
    public Map<String, Boolean> verifyEmail(@Valid @RequestBody EmailVerificationDTO verificationDTO) {
        boolean verified = userService.verifyEmail(verificationDTO.getToken());
        return Map.of("verified", verified);
    }
    
    @PostMapping("/resend-verification")
    @Operation(summary = "Resend verification email")
    public Map<String, Boolean> resendVerification(@Valid @RequestBody EmailDTO emailDTO) {
        userService.resendVerification(emailDTO.getEmail());
        return Map.of("sent", true);
    }
    
    @GetMapping("/check-email")
    @Operation(summary = "Check if email exists")
    public Map<String, Boolean> checkEmail(@RequestParam String email) {
        boolean exists = userService.checkEmailExists(email);
        return Map.of("exists", exists);
    }
}
```

### サービス実装例

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EventPublisherService eventPublisher;
    private final EmailService emailService;
    
    @Override
    @Transactional
    public User registerUser(UserRegistrationDTO registrationDTO) {
        // メールアドレス重複チェック
        if (userRepository.existsByEmail(registrationDTO.getEmail())) {
            throw new EmailAlreadyExistsException("Email already registered: " + registrationDTO.getEmail());
        }
        
        // ユーザーエンティティの作成
        User user = new User();
        user.setEmail(registrationDTO.getEmail());
        user.setPasswordHash(passwordEncoder.encode(registrationDTO.getPassword()));
        user.setFirstName(registrationDTO.getFirstName());
        user.setLastName(registrationDTO.getLastName());
        user.setPhoneNumber(registrationDTO.getPhoneNumber());
        user.setBirthDate(registrationDTO.getBirthDate());
        user.setGender(registrationDTO.getGender());
        
        // デフォルトロールの設定
        Role customerRole = roleRepository.findByName("CUSTOMER")
                .orElseThrow(() -> new RoleNotFoundException("Default role not found"));
        user.setRole(customerRole);
        
        // ユーザーの保存
        User savedUser = userRepository.save(user);
        
        // 検証メールの送信
        String verificationToken = generateVerificationToken(savedUser);
        emailService.sendVerificationEmail(savedUser.getEmail(), savedUser.getFirstName(), verificationToken);
        
        // ユーザー作成イベントの発行
        eventPublisher.publishUserCreated(savedUser);
        
        log.info("User registered successfully: {}", savedUser.getEmail());
        
        return savedUser;
    }
    
    @Override
    @Transactional(readOnly = true)
    public User getUserById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
    }
    
    @Override
    @Transactional(readOnly = true)
    public User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("Current user not found"));
    }
    
    @Override
    @Transactional
    public User updateUser(UUID id, UserUpdateDTO updateDTO) {
        User user = getUserById(id);
        
        // 更新可能なフィールドの更新
        if (updateDTO.getFirstName() != null) {
            user.setFirstName(updateDTO.getFirstName());
        }
        if (updateDTO.getLastName() != null) {
            user.setLastName(updateDTO.getLastName());
        }
        if (updateDTO.getPhoneNumber() != null) {
            user.setPhoneNumber(updateDTO.getPhoneNumber());
        }
        if (updateDTO.getBirthDate() != null) {
            user.setBirthDate(updateDTO.getBirthDate());
        }
        if (updateDTO.getGender() != null) {
            user.setGender(updateDTO.getGender());
        }
        
        User updatedUser = userRepository.save(user);
        
        // ユーザー更新イベントの発行
        eventPublisher.publishUserUpdated(updatedUser);
        
        log.info("User updated: {}", updatedUser.getEmail());
        
        return updatedUser;
    }
    
    @Override
    @Transactional
    public void deleteUser(UUID id) {
        User user = getUserById(id);
        
        // 論理削除またはデータ匿名化の実装
        user.setStatus(User.UserStatus.DEACTIVATED);
        user.setEmail(user.getEmail() + ".deleted." + System.currentTimeMillis());
        userRepository.save(user);
        
        // ユーザー削除イベントの発行
        eventPublisher.publishUserDeleted(id, user.getEmail());
        
        log.info("User deleted: {}", user.getEmail());
    }
    
    // 他のメソッド実装...
}
```

## 15. 環境構築・実行手順

### ローカル開発環境構築

1. 前提条件
   - Java 21
   - Docker と Docker Compose
   - Git

2. リポジトリのクローン
   ```bash
   git clone https://github.com/skieshop/user-service.git
   cd user-service
   ```

3. ローカルでの実行
   ```bash
   # 依存サービスの起動
   docker-compose up -d postgres redis kafka
   
   # アプリケーションのビルドと実行
   mvn spring-boot:run
   ```

4. Docker Compose での起動
   ```bash
   docker-compose up -d
   ```

### 単体・統合テスト実行

```bash
# 単体テスト実行
mvn test

# 統合テスト実行
mvn verify -P integration-test

# すべてのテスト実行
mvn verify
```

### 本番環境デプロイ手順

1. ビルドとコンテナイメージ作成
   ```bash
   mvn clean package
   docker build -t skieshop/user-service:latest .
   ```

2. コンテナレジストリへのプッシュ
   ```bash
   docker tag skieshop/user-service:latest acrskileshop.azurecr.io/user-service:latest
   docker push acrskileshop.azurecr.io/user-service:latest
   ```

3. Azure Container Apps へのデプロイ
   ```bash
   az login
   az acr login --name acrskileshop
   
   az containerapp update \
     --name user-service \
     --resource-group skieshop-rg \
     --image acrskileshop.azurecr.io/user-service:latest
   ```

### 動作確認手順

1. ヘルスチェックエンドポイント確認
   ```bash
   curl http://localhost:8081/actuator/health
   ```

2. Swagger UI アクセス
   ```
   http://localhost:8081/swagger-ui.html
   ```

3. 基本 API 動作確認
   ```bash
   # ユーザー登録
   curl -X POST http://localhost:8081/api/users \
     -H "Content-Type: application/json" \
     -d '{"email":"test@example.com","password":"Test123!","firstName":"Test","lastName":"User"}'
   
   # メールアドレス確認
   curl -X GET "http://localhost:8081/api/users/check-email?email=test@example.com"
   ```

## 16. 障害対応ガイド

### 一般的な問題のトラブルシューティング

| 問題 | 考えられる原因 | 解決策 |
|-----|--------------|--------|
| サービス起動失敗 | データベース接続エラー | データベース接続情報の確認、PostgreSQL サーバーの状態確認 |
| API タイムアウト | 高負荷または依存サービスの遅延 | リソースのスケーリング、依存サービスの監視 |
| 認証エラー | トークン無効または期限切れ | トークン設定の確認、認証サービスとの連携確認 |
| メール送信失敗 | メールサービス接続エラー | メールサービス設定確認、代替通知方法の検討 |
| キャッシュ不整合 | Redis 接続問題または古いキャッシュ | Redis 接続確認、キャッシュ無効化と再構築 |

### ログ解析ガイド

主要なログパターンと対応方法:

- `ERROR c.s.u.exception.GlobalExceptionHandler`: 未処理例外の発生、スタックトレースを確認
- `ERROR c.s.u.service.UserServiceImpl`: ユーザーサービスでの重大エラー
- `WARN c.s.u.security.JwtTokenProvider`: トークン検証の問題
- `INFO c.s.u.service.UserServiceImpl`: ユーザー操作の成功ログ（監査目的）

### 障害復旧手順

1. **サービスダウン時**:
   - コンテナログの確認
   - 依存サービスの状態確認
   - 必要に応じたサービス再起動
   - 問題が解決しない場合は以前のバージョンへのロールバック

2. **データ破損時**:
   - 読み取り専用モードへの切り替え
   - バックアップからのリストア
   - データ整合性チェックの実行

3. **パフォーマンス低下時**:
   - リソース使用状況の確認
   - 不要なキャッシュのクリア
   - 必要に応じたスケールアウト
   - 長時間実行クエリの特定と最適化

## 17. 将来拡張計画

### 短期的な拡張計画

- 多要素認証（MFA）の実装
- ソーシャルログイン連携の強化
- プロフィール画像管理機能
- GDPR 対応の強化

### 中長期的な拡張計画

- マイクロサービス間認証の OAuth2 への完全移行
- 顧客セグメンテーション機能
- ユーザー行動分析機能の強化
- ユーザー設定の拡張（通知設定、言語設定など）
- グローバル展開に向けた多言語対応
