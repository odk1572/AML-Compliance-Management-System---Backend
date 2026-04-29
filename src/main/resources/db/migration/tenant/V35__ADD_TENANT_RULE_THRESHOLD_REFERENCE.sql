-- Add the reference column to the tenant schema
ALTER TABLE tenant_rule_thresholds
    ADD COLUMN trt_reference VARCHAR(50);

-- Ensure uniqueness to prevent collision
ALTER TABLE tenant_rule_thresholds
    ADD CONSTRAINT uk_trt_reference UNIQUE (trt_reference);

-- Index for fast lookup in the "Threshold Management" UI
CREATE INDEX idx_trt_ref_lookup ON tenant_rule_thresholds (trt_reference);