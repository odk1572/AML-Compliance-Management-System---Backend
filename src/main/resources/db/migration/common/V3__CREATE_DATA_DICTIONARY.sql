
SET search_path TO common_schema;

CREATE TABLE data_dictionary (
                                 id              UUID            NOT NULL DEFAULT gen_random_uuid(),
                                 entity_name     VARCHAR(50)     NOT NULL,   -- e.g. TRANSACTION, CUSTOMER
                                 attribute_name  VARCHAR(100)    NOT NULL,   -- e.g. amount, originator_country
                                 data_type       VARCHAR(20)     NOT NULL,   -- NUMERIC | STRING | BOOLEAN | DATE
                                 value_cast_hint VARCHAR(50),                -- Runtime hint: DECIMAL, INTEGER, ISO_DATE etc.
                                 description     TEXT,
                                 is_active       BOOLEAN         NOT NULL DEFAULT TRUE,

    -- Lightweight audit — cache invalidation only
                                 sys_updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

                                 CONSTRAINT pk_data_dictionary           PRIMARY KEY (id),
                                 CONSTRAINT uq_data_dictionary_attr      UNIQUE (entity_name, attribute_name),
                                 CONSTRAINT chk_data_dictionary_type     CHECK (data_type IN ('NUMERIC','STRING','BOOLEAN','DATE'))
);

CREATE INDEX idx_data_dictionary_entity
    ON data_dictionary (entity_name)
    WHERE is_active = TRUE;

-- Seed the core transaction attributes used by the rule engine
INSERT INTO data_dictionary (entity_name, attribute_name, data_type, value_cast_hint, description) VALUES
                                                                                                       ('TRANSACTION', 'amount',                'NUMERIC',  'DECIMAL',  'Transaction amount in original currency'),
                                                                                                       ('TRANSACTION', 'currency_code',         'STRING',   NULL,       'ISO 4217 currency code'),
                                                                                                       ('TRANSACTION', 'originator_country',    'STRING',   NULL,       'Originator country ISO 3166-1 alpha-2'),
                                                                                                       ('TRANSACTION', 'beneficiary_country',   'STRING',   NULL,       'Beneficiary country ISO 3166-1 alpha-2'),
                                                                                                       ('TRANSACTION', 'transaction_type',      'STRING',   NULL,       'WIRE | SWIFT | ACH | INTERNAL | CASH'),
                                                                                                       ('TRANSACTION', 'channel',               'STRING',   NULL,       'ONLINE | BRANCH | ATM | API'),
                                                                                                       ('TRANSACTION', 'transaction_timestamp', 'DATE',     'ISO_DATE', 'Timestamp of the transaction'),
                                                                                                       ('CUSTOMER',    'risk_rating',           'STRING',   NULL,       'CRITICAL | HIGH | MEDIUM | LOW'),
                                                                                                       ('CUSTOMER',    'is_pep',                'BOOLEAN',  NULL,       'Politically Exposed Person flag'),
                                                                                                       ('CUSTOMER',    'is_sanctioned',         'BOOLEAN',  NULL,       'Sanctions list match flag'),
                                                                                                       ('CUSTOMER',    'is_dormant',            'BOOLEAN',  NULL,       'Account dormancy flag'),
                                                                                                       ('CUSTOMER',    'account_age_days',      'NUMERIC',  'INTEGER',  'Days since account_opened_on'),
                                                                                                       ('CUSTOMER',    'kyc_status',            'STRING',   NULL,       'VERIFIED | PENDING | EXPIRED');

COMMENT ON TABLE  data_dictionary             IS 'Registry of all evaluable entity attributes. Drives the rule builder UI and RuleEvaluator runtime.';
COMMENT ON COLUMN data_dictionary.value_cast_hint IS 'Hint for RuleEvaluator to cast threshold_value at runtime. Avoids joining this table per condition evaluation.';