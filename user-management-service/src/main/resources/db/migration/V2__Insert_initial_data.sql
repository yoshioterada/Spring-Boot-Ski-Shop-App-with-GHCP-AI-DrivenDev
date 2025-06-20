-- 初期データ投入
-- V2__Insert_initial_data.sql

-- デフォルトロールの作成
INSERT INTO roles (id, name, description) VALUES
    (uuid_generate_v4(), 'ADMIN', 'システム管理者ロール'),
    (uuid_generate_v4(), 'CUSTOMER', '一般顧客ロール'),
    (uuid_generate_v4(), 'STORE_MANAGER', '店舗管理者ロール'),
    (uuid_generate_v4(), 'SUPPORT', 'サポートスタッフロール');

-- 基本権限の作成
INSERT INTO permissions (id, name, description, resource, action) VALUES
    -- ユーザー関連権限
    (uuid_generate_v4(), 'USER_READ', 'ユーザー情報読み取り', 'USER', 'READ'),
    (uuid_generate_v4(), 'USER_WRITE', 'ユーザー情報書き込み', 'USER', 'WRITE'),
    (uuid_generate_v4(), 'USER_DELETE', 'ユーザー削除', 'USER', 'DELETE'),
    (uuid_generate_v4(), 'USER_ADMIN', 'ユーザー管理', 'USER', 'ADMIN'),
    
    -- 商品関連権限
    (uuid_generate_v4(), 'PRODUCT_READ', '商品情報読み取り', 'PRODUCT', 'READ'),
    (uuid_generate_v4(), 'PRODUCT_WRITE', '商品情報書き込み', 'PRODUCT', 'WRITE'),
    (uuid_generate_v4(), 'PRODUCT_DELETE', '商品削除', 'PRODUCT', 'DELETE'),
    (uuid_generate_v4(), 'PRODUCT_ADMIN', '商品管理', 'PRODUCT', 'ADMIN'),
    
    -- 注文関連権限
    (uuid_generate_v4(), 'ORDER_READ', '注文情報読み取り', 'ORDER', 'READ'),
    (uuid_generate_v4(), 'ORDER_WRITE', '注文情報書き込み', 'ORDER', 'WRITE'),
    (uuid_generate_v4(), 'ORDER_DELETE', '注文削除', 'ORDER', 'DELETE'),
    (uuid_generate_v4(), 'ORDER_ADMIN', '注文管理', 'ORDER', 'ADMIN'),
    
    -- 在庫関連権限
    (uuid_generate_v4(), 'INVENTORY_READ', '在庫情報読み取り', 'INVENTORY', 'READ'),
    (uuid_generate_v4(), 'INVENTORY_WRITE', '在庫情報書き込み', 'INVENTORY', 'WRITE'),
    (uuid_generate_v4(), 'INVENTORY_ADMIN', '在庫管理', 'INVENTORY', 'ADMIN'),
    
    -- システム管理権限
    (uuid_generate_v4(), 'ADMIN_ACCESS', '管理者アクセス', 'SYSTEM', 'ADMIN'),
    (uuid_generate_v4(), 'SUPPORT_ACCESS', 'サポートアクセス', 'SYSTEM', 'SUPPORT');

-- ロール-権限関係の設定
-- ADMINロール：すべての権限
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ADMIN';

-- CUSTOMERロール：基本的な読み取り権限
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'CUSTOMER' 
AND p.name IN ('USER_READ', 'PRODUCT_READ', 'ORDER_READ');

-- STORE_MANAGERロール：商品と在庫管理権限
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'STORE_MANAGER' 
AND p.name IN (
    'USER_READ', 'PRODUCT_READ', 'PRODUCT_WRITE', 'PRODUCT_ADMIN',
    'ORDER_READ', 'ORDER_WRITE', 'ORDER_ADMIN',
    'INVENTORY_READ', 'INVENTORY_WRITE', 'INVENTORY_ADMIN'
);

-- SUPPORTロール：顧客サポート関連権限
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'SUPPORT' 
AND p.name IN (
    'USER_READ', 'USER_WRITE', 'PRODUCT_READ',
    'ORDER_READ', 'ORDER_WRITE', 'SUPPORT_ACCESS'
);

-- 管理者ユーザーの作成（初期パスワード: Admin123!）
INSERT INTO users (id, email, password_hash, first_name, last_name, status, email_verified, role_id)
SELECT 
    uuid_generate_v4(),
    'admin@skishop.com',
    '$2a$10$Z9ZzOoJ8Q8Z8Q8Z8Q8Z8Q.Q8Z8Q8Z8Q8Z8Q8Z8Q8Z8Q8Z8Q8Z8Q8Q8',  -- Admin123!
    'System',
    'Administrator',
    'ACTIVE',
    true,
    r.id
FROM roles r
WHERE r.name = 'ADMIN';
