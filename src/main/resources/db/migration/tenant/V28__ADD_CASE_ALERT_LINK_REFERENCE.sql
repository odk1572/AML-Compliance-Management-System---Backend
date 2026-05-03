ALTER TABLE case_alert_links
    ADD COLUMN cal_reference VARCHAR(50);

ALTER TABLE case_alert_links
    ADD CONSTRAINT uk_cal_reference UNIQUE (cal_reference);

CREATE INDEX idx_cal_ref_lookup ON case_alert_links (cal_reference);