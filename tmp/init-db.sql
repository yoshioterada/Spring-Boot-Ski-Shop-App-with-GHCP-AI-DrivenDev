-- スキーショップマイクロサービス用データベース初期化スクリプト

-- 認証サービス用データベース
CREATE DATABASE skishop_auth;
GRANT ALL PRIVILEGES ON DATABASE skishop_auth TO skishop_user;

-- ユーザー管理サービス用データベース
CREATE DATABASE skishop_user;
GRANT ALL PRIVILEGES ON DATABASE skishop_user TO skishop_user;

-- 在庫管理サービス用データベース
CREATE DATABASE skishop_inventory;
GRANT ALL PRIVILEGES ON DATABASE skishop_inventory TO skishop_user;

-- 販売管理サービス用データベース
CREATE DATABASE skishop_sales;
GRANT ALL PRIVILEGES ON DATABASE skishop_sales TO skishop_user;

-- 支払い・カートサービス用データベース
CREATE DATABASE skishop_payment;
GRANT ALL PRIVILEGES ON DATABASE skishop_payment TO skishop_user;

-- AIサポートサービス用データベース
CREATE DATABASE skishop_ai;
GRANT ALL PRIVILEGES ON DATABASE skishop_ai TO skishop_user;

-- 拡張機能の有効化
\c skishop_auth;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

\c skishop_user;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

\c skishop_inventory;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

\c skishop_sales;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

\c skishop_payment;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

\c skishop_ai;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
