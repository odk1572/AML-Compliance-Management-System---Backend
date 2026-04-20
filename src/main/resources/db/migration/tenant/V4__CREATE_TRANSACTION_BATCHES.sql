CREATE TABLE transaction_batches (
                                     id UUID PRIMARY KEY,
                                     batch_reference VARCHAR(50) UNIQUE NOT NULL, -- Format: BATCH-YYYYMMDD-XXXX
                                     uploaded_by UUID NOT NULL REFERENCES tenant_users(id),
                                     file_name VARCHAR(255) NOT NULL,
                                     file_hash_sha256 VARCHAR(64) NOT NULL, -- Used as a deduplication guard
                                     file_size_bytes BIGINT NOT NULL,
                                     cloudinary_public_id VARCHAR(100),
                                     cloudinary_secure_url TEXT,
                                     total_records INT DEFAULT 0,
                                     batch_status VARCHAR(50) NOT NULL DEFAULT 'PENDING', -- PENDING / VALIDATING / PROCESSING / PROCESSED / FAILED
                                     failure_details JSONB, -- Stores validation error messages or stack traces
                                     spring_batch_job_id VARCHAR(100),
                                     processed_at TIMESTAMP,

                                     sys_is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
                                     sys_deleted_at TIMESTAMP,
                                     sys_created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     sys_updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TRIGGER trg_transaction_batches_updated_at
    BEFORE UPDATE ON transaction_batches
    FOR EACH ROW
    EXECUTE FUNCTION update_sys_updated_at_column();

CREATE INDEX idx_batches_status ON transaction_batches(batch_status);
CREATE INDEX idx_batches_file_hash ON transaction_batches(file_hash_sha256); -- Crucial for fast dedup check
CREATE INDEX idx_batches_uploaded_by ON transaction_batches(uploaded_by);
CREATE INDEX idx_batches_sys_is_deleted ON transaction_batches(sys_is_deleted);
CREATE INDEX idx_batches_created_at ON transaction_batches(sys_created_at);