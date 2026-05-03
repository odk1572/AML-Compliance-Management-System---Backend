ALTER TABLE tenant_rule_thresholds
    ADD COLUMN trt_reference VARCHAR(50);

ALTER TABLE tenant_rule_thresholds
    ADD CONSTRAINT uk_trt_reference UNIQUE (trt_reference);

CREATE INDEX idx_trt_ref_lookup ON tenant_rule_thresholds (trt_reference);