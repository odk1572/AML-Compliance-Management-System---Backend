-- Add the user_code column to the common_schema table
ALTER TABLE common_schema.platform_users
    ADD COLUMN user_code VARCHAR(50);

-- Ensure uniqueness so no two admins have the same code
ALTER TABLE common_schema.platform_users
    ADD CONSTRAINT uk_platform_user_code UNIQUE (user_code);

-- Index for fast login and admin lookups
CREATE INDEX idx_platform_user_code_lookup ON common_schema.platform_users (user_code);