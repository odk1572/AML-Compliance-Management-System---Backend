SET search_path TO common_schema;

-- 1. Create the user
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

-- 2. Log the action using the valid User ID as the actor
INSERT INTO common_schema.platform_audit_log (
    id,
    actor_id,           -- This must exist in platform_users
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
           gen_random_uuid(), -- ID for the log itself is fine to be random
           '00000000-0000-0000-0000-000000000001', -- FIX: Match the Admin ID above
           'SYSTEM',
           'SYSTEM',
           'SUPER_ADMIN_SEEDED',
           'PLATFORM_USER',
           '00000000-0000-0000-0000-000000000001',
           jsonb_build_object(
                   'email', 'superadmin@amlsystem.internal',
                   'note',  'Pre-seeded via Flyway migration.'
           ),
           '127.0.0.1',
           NOW()
       );

--Admin@AML2024!