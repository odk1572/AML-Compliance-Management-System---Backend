
CREATE SCHEMA IF NOT EXISTS common_schema;
SET search_path TO common_schema;

CREATE TABLE geographic_risk_ratings (
                                         id UUID PRIMARY KEY,
                                         country_code VARCHAR(3) UNIQUE NOT NULL,
                                         country_name VARCHAR(255) NOT NULL,
                                         fatf_status VARCHAR(50) NOT NULL DEFAULT 'COMPLIANT',
                                         basel_aml_index_score INT NOT NULL DEFAULT 0,
                                         risk_tier VARCHAR(50) NOT NULL DEFAULT 'LOW',
                                         notes TEXT,
                                         sys_is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
                                         sys_deleted_at TIMESTAMP,
                                         effective_from TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                         sys_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                         sys_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TRIGGER trg_geographic_risk_ratings_updated_at
    BEFORE UPDATE ON geographic_risk_ratings
    FOR EACH ROW
    EXECUTE FUNCTION update_sys_updated_at_column();

CREATE INDEX idx_geo_risk_country_code ON geographic_risk_ratings(country_code);
CREATE INDEX idx_geo_risk_risk_tier ON geographic_risk_ratings(risk_tier);
CREATE INDEX idx_geo_risk_fatf_status ON geographic_risk_ratings(fatf_status);
CREATE INDEX idx_geo_risk_sys_is_deleted ON geographic_risk_ratings(sys_is_deleted);