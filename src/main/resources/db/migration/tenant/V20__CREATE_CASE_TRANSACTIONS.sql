CREATE TABLE case_transactions (
                                   id                UUID PRIMARY KEY,
                                   case_id           UUID NOT NULL,
                                   transaction_id    UUID NOT NULL,
                                   sys_created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                   CONSTRAINT fk_case_txn_case FOREIGN KEY (case_id) REFERENCES cases(id) ON DELETE CASCADE,
                                   CONSTRAINT fk_case_txn_transaction FOREIGN KEY (transaction_id) REFERENCES transactions(id),
                                   CONSTRAINT uk_case_transaction_link UNIQUE (case_id, transaction_id)
);

CREATE INDEX idx_case_transactions_case_id ON case_transactions(case_id);