-- Point Service Database Schema
-- V1: Create initial tables for point management

-- Create tier_definitions table
CREATE TABLE tier_definitions (
    tier_level VARCHAR(20) PRIMARY KEY,
    tier_name VARCHAR(50) NOT NULL,
    min_points_required INTEGER NOT NULL DEFAULT 0,
    point_multiplier DECIMAL(3,2) NOT NULL DEFAULT 1.00,
    benefits JSONB DEFAULT '{}',
    next_tier VARCHAR(20),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tier_next FOREIGN KEY (next_tier) REFERENCES tier_definitions(tier_level)
);

-- Create user_tiers table
CREATE TABLE user_tiers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID UNIQUE NOT NULL,
    tier_level VARCHAR(20) NOT NULL DEFAULT 'bronze',
    total_points_earned INTEGER DEFAULT 0,
    current_points INTEGER DEFAULT 0,
    tier_upgraded_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_usertier_tier FOREIGN KEY (tier_level) REFERENCES tier_definitions(tier_level)
);

-- Create point_transactions table
CREATE TABLE point_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    transaction_type VARCHAR(20) NOT NULL CHECK (transaction_type IN ('earned', 'redeemed', 'expired', 'transferred_in', 'transferred_out')),
    amount INTEGER NOT NULL,
    balance_after INTEGER NOT NULL,
    reason VARCHAR(100) NOT NULL,
    reference_id VARCHAR(100),
    expires_at TIMESTAMP,
    is_expired BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create point_redemptions table
CREATE TABLE point_redemptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    transaction_id UUID NOT NULL,
    redemption_type VARCHAR(50) NOT NULL,
    points_used INTEGER NOT NULL,
    value_redeemed DECIMAL(10,2) NOT NULL,
    status VARCHAR(20) DEFAULT 'completed',
    redeemed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_redemption_transaction FOREIGN KEY (transaction_id) REFERENCES point_transactions(id)
);

-- Create point_redemption_details table for storing redemption details
CREATE TABLE point_redemption_details (
    redemption_id UUID NOT NULL,
    detail_key VARCHAR(50) NOT NULL,
    detail_value VARCHAR(255),
    PRIMARY KEY (redemption_id, detail_key),
    CONSTRAINT fk_redemption_details FOREIGN KEY (redemption_id) REFERENCES point_redemptions(id) ON DELETE CASCADE
);

-- Create point_expiry table
CREATE TABLE point_expiry (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id UUID NOT NULL,
    expired_points INTEGER NOT NULL,
    scheduled_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP,
    status VARCHAR(20) DEFAULT 'scheduled',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_expiry_transaction FOREIGN KEY (transaction_id) REFERENCES point_transactions(id)
);

-- Create indexes for performance
CREATE INDEX idx_user_tiers_user_id ON user_tiers(user_id);
CREATE INDEX idx_point_transactions_user_id ON point_transactions(user_id);
CREATE INDEX idx_point_transactions_created_at ON point_transactions(created_at);
CREATE INDEX idx_point_transactions_user_created ON point_transactions(user_id, created_at);
CREATE INDEX idx_point_transactions_expires_at ON point_transactions(expires_at) WHERE expires_at IS NOT NULL AND is_expired = false;
CREATE INDEX idx_point_transactions_reference ON point_transactions(reference_id) WHERE reference_id IS NOT NULL;
CREATE INDEX idx_point_redemptions_user_id ON point_redemptions(user_id);
CREATE INDEX idx_point_redemptions_redeemed_at ON point_redemptions(redeemed_at);
CREATE INDEX idx_point_expiry_scheduled_at ON point_expiry(scheduled_at);
CREATE INDEX idx_point_expiry_status ON point_expiry(status);

-- Insert default tier definitions
INSERT INTO tier_definitions (tier_level, tier_name, min_points_required, point_multiplier, benefits, next_tier, is_active) VALUES
('bronze', 'Bronze', 0, 1.00, '{"free_shipping_threshold": 100, "birthday_bonus": 500}', 'silver', true),
('silver', 'Silver', 10000, 1.25, '{"free_shipping_threshold": 75, "birthday_bonus": 1000, "early_access": true}', 'gold', true),
('gold', 'Gold', 25000, 1.50, '{"free_shipping_threshold": 50, "birthday_bonus": 2000, "early_access": true, "exclusive_products": true}', 'platinum', true),
('platinum', 'Platinum', 50000, 2.00, '{"free_shipping_threshold": 0, "birthday_bonus": 5000, "early_access": true, "exclusive_products": true, "personal_shopper": true}', null, true);

-- Create trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_user_tiers_updated_at BEFORE UPDATE ON user_tiers FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
