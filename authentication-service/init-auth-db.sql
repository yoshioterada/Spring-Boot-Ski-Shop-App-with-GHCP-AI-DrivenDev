-- Authentication Service Database Initialization Script
-- This script creates the necessary database schema for the authentication service

-- Set timezone
SET timezone = 'UTC';

-- Create extensions if they don't exist
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Grant privileges to the user
GRANT ALL PRIVILEGES ON DATABASE skishop_auth TO auth_user;
GRANT ALL ON SCHEMA public TO auth_user;

-- Set default schema
ALTER USER auth_user SET search_path = public;

-- Create initial schema (basic tables will be created by Hibernate)
-- This file mainly ensures proper permissions and extensions are set up
