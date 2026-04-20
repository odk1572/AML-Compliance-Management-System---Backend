CREATE TABLE customer_profiles (
                                   id UUID PRIMARY KEY,
                                   account_number VARCHAR(50) UNIQUE NOT NULL,
                                   customer_name VARCHAR(255) NOT NULL,
                                   customer_type VARCHAR(20) NOT NULL, -- INDIVIDUAL / CORPORATE
                                   id_type VARCHAR(50),
                                   id_number VARCHAR(100),
                                   nationality VARCHAR(3), -- ISO Country Code
                                   country_of_residence VARCHAR(3), -- ISO Country Code

    -- Financial Context (Critical for transactional behavior monitoring)
                                   monthly_income DECIMAL(20, 2) DEFAULT 0.00,
                                   net_worth DECIMAL(20, 2) DEFAULT 0.00,

    -- AML Risk Context
                                   risk_rating VARCHAR(20) NOT NULL DEFAULT 'LOW', -- CRITICAL / HIGH / MEDIUM / LOW
                                   risk_score INT DEFAULT 0,
                                   is_pep BOOLEAN DEFAULT FALSE,
                                   is_dormant BOOLEAN DEFAULT FALSE,

    -- Lifecycle Dates
                                   account_opened_on DATE NOT NULL,
                                   last_activity_date DATE,
                                   kyc_status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- VERIFIED / PENDING / EXPIRED

    -- Metadata & Soft Deletion
                                   sys_is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
                                   sys_deleted_at TIMESTAMP,
                                   sys_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   sys_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TRIGGER trg_customer_profiles_updated_at
    BEFORE UPDATE ON customer_profiles
    FOR EACH ROW
    EXECUTE FUNCTION update_sys_updated_at_column();

CREATE INDEX idx_customer_account_number ON customer_profiles(account_number);
CREATE INDEX idx_customer_risk_rating ON customer_profiles(risk_rating);
CREATE INDEX idx_customer_kyc_status ON customer_profiles(kyc_status);
CREATE INDEX idx_customer_is_pep ON customer_profiles(is_pep);
CREATE INDEX idx_customer_nationality ON customer_profiles(nationality);
CREATE INDEX idx_customer_sys_is_deleted ON customer_profiles(sys_is_deleted);