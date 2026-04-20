

SET search_path TO common_schema;

INSERT INTO geographic_risk_ratings
(id, country_code, country_name, fatf_status, basel_aml_index_score, risk_tier, notes)
VALUES
    (gen_random_uuid(), 'PRK', 'North Korea', 'BLACKLIST', 9, 'CRITICAL', 'FATF Call for Action. Severe sanctions apply.'),
    (gen_random_uuid(), 'IRN', 'Iran', 'BLACKLIST', 8, 'CRITICAL', 'FATF Call for Action. High risk of terrorism financing.'),
    (gen_random_uuid(), 'MMR', 'Myanmar', 'BLACKLIST', 8, 'CRITICAL', 'FATF Call for Action. Strategic AML/CFT deficiencies.');

INSERT INTO geographic_risk_ratings
(id, country_code, country_name, fatf_status, basel_aml_index_score, risk_tier, notes)
VALUES
    (gen_random_uuid(), 'SYR', 'Syria', 'GREYLIST', 8, 'HIGH', 'Jurisdiction under increased monitoring.'),
    (gen_random_uuid(), 'YEM', 'Yemen', 'GREYLIST', 8, 'HIGH', 'Jurisdiction under increased monitoring.'),
    (gen_random_uuid(), 'SSD', 'South Sudan', 'GREYLIST', 7, 'HIGH', 'Jurisdiction under increased monitoring.'),
    (gen_random_uuid(), 'HTI', 'Haiti', 'GREYLIST', 7, 'HIGH', 'Jurisdiction under increased monitoring.');

INSERT INTO geographic_risk_ratings
(id, country_code, country_name, fatf_status, basel_aml_index_score, risk_tier, notes)
VALUES
    (gen_random_uuid(), 'USA', 'United States', 'COMPLIANT', 4, 'LOW', 'Baseline compliant jurisdiction.'),
    (gen_random_uuid(), 'GBR', 'United Kingdom', 'COMPLIANT', 4, 'LOW', 'Baseline compliant jurisdiction.'),
    (gen_random_uuid(), 'IND', 'India', 'COMPLIANT', 4, 'LOW', 'Baseline compliant jurisdiction.');