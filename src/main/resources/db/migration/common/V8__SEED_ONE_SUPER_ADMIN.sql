
SET search_path TO common_schema;

INSERT INTO platform_users (
    id,
    email,
    password_hash,
    full_name,
    role,
    failed_login_attempts,
    is_locked,
    sys_is_deleted,
    sys_created_at,
    sys_updated_at
)
VALUES (
           '00000000-0000-0000-0000-000000000001',
           'superadmin@amlsystem.internal',
           -- BCrypt hash of 'Admin@AML2024!' (strength 12)
           -- ROTATE THIS BEFORE PRODUCTION DEPLOYMENT
           '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQyCkGP4CzU9c7TSBpGmHjL6G',
           'AML System Administrator',
           'SUPER_ADMIN',
           0,
           FALSE,
           FALSE,
           NOW(),
           NOW()
       )
    ON CONFLICT (email) DO NOTHING;

-- Log the seeding as a system action
INSERT INTO platform_audit_log (
    actor_id,
    actor_role,
    action_category,
    action_performed,
    target_entity_type,
    target_entity_id,
    new_state,
    ip_address,
    sys_created_at
)
VALUES (
           NULL,
           'SYSTEM',
           'SYSTEM',
           'SUPER_ADMIN_SEEDED',
           'PLATFORM_USER',
           '00000000-0000-0000-0000-000000000001',
           jsonb_build_object(
                   'email', 'superadmin@amlsystem.internal',
                   'note',  'Pre-seeded via V8 Flyway migration. Password must be rotated on first login.'
           ),
           '127.0.0.1',
           NOW()
       );

COMMENT ON TABLE platform_users IS 'Super Admin seeded by V8 migration. ID: 00000000-0000-0000-0000-000000000001. Rotate password before production.';