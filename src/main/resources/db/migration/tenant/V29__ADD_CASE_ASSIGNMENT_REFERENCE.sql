-- Add the reference column
ALTER TABLE case_assignments
    ADD COLUMN asg_reference VARCHAR(50);

-- Ensure uniqueness and add an index
ALTER TABLE case_assignments
    ADD CONSTRAINT uk_asg_reference UNIQUE (asg_reference);

CREATE INDEX idx_asg_ref_lookup ON case_assignments (asg_reference);