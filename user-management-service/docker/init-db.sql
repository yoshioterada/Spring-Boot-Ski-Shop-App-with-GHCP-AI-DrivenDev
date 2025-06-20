-- User Management Service用データベース初期化スクリプト
-- PostgreSQL 15対応

-- 必要なextensionを有効化
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "citext";

-- データベース設定を最適化
ALTER DATABASE skishop_user SET timezone TO 'UTC';

-- 開発用の設定
-- 本番環境では削除すること
ALTER SYSTEM SET log_statement = 'none';
ALTER SYSTEM SET log_min_duration_statement = -1;

-- 初期設定完了ログ
DO $$
BEGIN
    RAISE NOTICE 'Database initialized for User Management Service at %', now();
    RAISE NOTICE 'Extensions: uuid-ossp, citext enabled';
    RAISE NOTICE 'Timezone set to UTC';
END $$;
