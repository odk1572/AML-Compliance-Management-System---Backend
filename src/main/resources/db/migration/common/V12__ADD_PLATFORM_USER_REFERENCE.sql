ALTER TABLE common_schema.platform_users
    ADD COLUMN user_code VARCHAR(50);

ALTER TABLE common_schema.platform_users
    ADD CONSTRAINT uk_platform_user_code UNIQUE (user_code);

CREATE INDEX idx_platform_user_code_lookup ON common_schema.platform_users (user_code);