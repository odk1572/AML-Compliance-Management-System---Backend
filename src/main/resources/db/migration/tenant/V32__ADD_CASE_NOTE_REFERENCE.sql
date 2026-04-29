-- Add the reference column
ALTER TABLE case_notes
    ADD COLUMN note_reference VARCHAR(50);

-- Ensure uniqueness and add an index
ALTER TABLE case_notes
    ADD CONSTRAINT uk_note_reference UNIQUE (note_reference);

CREATE INDEX idx_note_ref_lookup ON case_notes (note_reference);