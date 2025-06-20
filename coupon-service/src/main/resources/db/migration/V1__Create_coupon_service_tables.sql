-- Create coupon service tables

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Campaigns table
CREATE TABLE campaigns (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    campaign_type VARCHAR(50) NOT NULL CHECK (campaign_type IN ('PERCENTAGE', 'FIXED_AMOUNT', 'BOGO', 'FREE_SHIPPING')),
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP NOT NULL,
    is_active BOOLEAN DEFAULT FALSE,
    rules JSONB DEFAULT '{}',
    max_coupons INTEGER,
    generated_coupons INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT check_date_range CHECK (end_date > start_date),
    CONSTRAINT check_max_coupons CHECK (max_coupons IS NULL OR max_coupons > 0),
    CONSTRAINT check_generated_coupons CHECK (generated_coupons >= 0)
);

-- Coupons table
CREATE TABLE coupons (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    campaign_id UUID NOT NULL,
    code VARCHAR(50) UNIQUE NOT NULL,
    coupon_type VARCHAR(50) NOT NULL CHECK (coupon_type IN ('PERCENTAGE', 'FIXED_AMOUNT', 'FREE_SHIPPING')),
    discount_value DECIMAL(10,2) NOT NULL,
    discount_type VARCHAR(20) NOT NULL CHECK (discount_type IN ('PERCENTAGE', 'FIXED')),
    minimum_amount DECIMAL(10,2) DEFAULT 0,
    maximum_discount DECIMAL(10,2),
    usage_limit INTEGER DEFAULT 1,
    used_count INTEGER DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_coupon_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns(id) ON DELETE CASCADE,
    CONSTRAINT check_discount_value CHECK (discount_value > 0),
    CONSTRAINT check_minimum_amount CHECK (minimum_amount >= 0),
    CONSTRAINT check_maximum_discount CHECK (maximum_discount IS NULL OR maximum_discount >= 0),
    CONSTRAINT check_usage_limit CHECK (usage_limit > 0),
    CONSTRAINT check_used_count CHECK (used_count >= 0 AND used_count <= usage_limit)
);

-- Coupon usage table
CREATE TABLE coupon_usage (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    coupon_id UUID NOT NULL,
    user_id UUID NOT NULL,
    order_id UUID NOT NULL,
    discount_applied DECIMAL(10,2) NOT NULL,
    order_amount DECIMAL(10,2) NOT NULL,
    used_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_usage_coupon FOREIGN KEY (coupon_id) REFERENCES coupons(id) ON DELETE CASCADE,
    CONSTRAINT check_discount_applied CHECK (discount_applied >= 0),
    CONSTRAINT check_order_amount CHECK (order_amount > 0),
    UNIQUE(coupon_id, order_id)
);

-- User coupons table (for assigned coupons)
CREATE TABLE user_coupons (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    coupon_id UUID NOT NULL,
    is_redeemed BOOLEAN DEFAULT FALSE,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    redeemed_at TIMESTAMP,
    CONSTRAINT fk_user_coupon FOREIGN KEY (coupon_id) REFERENCES coupons(id) ON DELETE CASCADE,
    UNIQUE(user_id, coupon_id)
);

-- Create indexes for better performance
CREATE INDEX idx_campaigns_active ON campaigns(is_active);
CREATE INDEX idx_campaigns_dates ON campaigns(start_date, end_date);
CREATE INDEX idx_campaigns_type ON campaigns(campaign_type);

CREATE INDEX idx_coupons_code ON coupons(code);
CREATE INDEX idx_coupons_campaign ON coupons(campaign_id);
CREATE INDEX idx_coupons_active ON coupons(is_active);
CREATE INDEX idx_coupons_expires ON coupons(expires_at);
CREATE INDEX idx_coupons_type ON coupons(coupon_type);

CREATE INDEX idx_coupon_usage_coupon ON coupon_usage(coupon_id);
CREATE INDEX idx_coupon_usage_user ON coupon_usage(user_id);
CREATE INDEX idx_coupon_usage_order ON coupon_usage(order_id);
CREATE INDEX idx_coupon_usage_date ON coupon_usage(used_at);

CREATE INDEX idx_user_coupons_user ON user_coupons(user_id);
CREATE INDEX idx_user_coupons_coupon ON user_coupons(coupon_id);
CREATE INDEX idx_user_coupons_redeemed ON user_coupons(is_redeemed);
CREATE INDEX idx_user_coupons_assigned ON user_coupons(assigned_at);

-- Update triggers for updated_at timestamps
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_campaigns_updated_at 
    BEFORE UPDATE ON campaigns 
    FOR EACH ROW EXECUTE PROCEDURE update_updated_at_column();

CREATE TRIGGER update_coupons_updated_at 
    BEFORE UPDATE ON coupons 
    FOR EACH ROW EXECUTE PROCEDURE update_updated_at_column();

-- Insert sample data (optional for development)
INSERT INTO campaigns (id, name, description, campaign_type, start_date, end_date, is_active, rules, max_coupons) VALUES
(uuid_generate_v4(), 'Winter Sale 2024', 'Winter ski equipment sale', 'PERCENTAGE', '2024-01-01 00:00:00', '2024-03-31 23:59:59', true, '{"userSegment": "all", "productCategories": ["ski", "snowboard"], "minOrderAmount": 5000}', 1000),
(uuid_generate_v4(), 'Spring Clearance', 'Spring clearance sale', 'FIXED_AMOUNT', '2024-04-01 00:00:00', '2024-05-31 23:59:59', false, '{"userSegment": "loyalty", "minOrderAmount": 10000}', 500);
