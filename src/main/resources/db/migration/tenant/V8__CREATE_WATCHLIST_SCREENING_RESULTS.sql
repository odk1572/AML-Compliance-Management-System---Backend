
CREATE TABLE watchlist_screening_results (
                                             id                      UUID            NOT NULL DEFAULT gen_random_uuid(),

    -- Transaction that was screened
                                             transaction_id          UUID            NOT NULL,

    -- Cross-schema ref: common_schema.global_watchlists.id
    -- The specific watchlist entry that produced this match
    -- No FK constraint (cross-schema) — enforced at service layer
                                             global_watchlist_id     UUID            NOT NULL,

    -- The name string that was submitted for matching
    -- Either originator_name or beneficiary_name from TRANSACTIONS
                                             screened_name           VARCHAR(255)    NOT NULL,

    -- Which party in the transaction triggered this match
                                             match_type              VARCHAR(15)     NOT NULL,

    -- Algorithm used by FuzzyNameMatcher
                                             match_algorithm         VARCHAR(20)     NOT NULL DEFAULT 'JARO_WINKLER',

    -- Similarity score: 4 decimal places for precision
    -- 0.0000 (no match) → 1.0000 (identical)
                                             match_score             NUMERIC(5, 4)   NOT NULL
                                                 CHECK (match_score BETWEEN 0 AND 1),

    -- Source list that contained the matched entry
                                             list_source             VARCHAR(10)     NOT NULL,

    -- Final determination after score thresholding
                                             screening_result        VARCHAR(15)     NOT NULL,

    -- Single immutable timestamp — no updates ever
                                             sys_created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    -- ── Constraints ──────────────────────────────────────────
                                             CONSTRAINT pk_watchlist_screening_results
                                                 PRIMARY KEY (id),

                                             CONSTRAINT chk_wsr_match_type
                                                 CHECK (match_type IN ('ORIGINATOR', 'BENEFICIARY')),

                                             CONSTRAINT chk_wsr_algorithm
                                                 CHECK (match_algorithm IN (
                                                                            'JARO_WINKLER',
                                                                            'EXACT',
                                                                            'LEVENSHTEIN',
                                                                            'SOUNDEX'
                                                     )),

                                             CONSTRAINT chk_wsr_list_source
                                                 CHECK (list_source IN ('OFAC', 'UN', 'FATF', 'EU', 'LOCAL')),

                                             CONSTRAINT chk_wsr_screening_result
                                                 CHECK (screening_result IN ('HIT', 'POTENTIAL_HIT', 'CLEAR')),

    -- Exact match must score 1.0
                                             CONSTRAINT chk_wsr_exact_score
                                                 CHECK (
                                                     match_algorithm != 'EXACT' OR match_score = 1.0
),

    CONSTRAINT fk_wsr_transaction
        FOREIGN KEY (transaction_id)
        REFERENCES transactions (id)
);

-- ── Indexes ───────────────────────────────────────────────────
-- CO investigation view: screening results for a transaction
CREATE INDEX idx_wsr_transaction
    ON watchlist_screening_results (transaction_id);

-- Alert detail view: only actionable results shown to CO
CREATE INDEX idx_wsr_hit_results
    ON watchlist_screening_results (transaction_id, screening_result)
    WHERE screening_result IN ('HIT', 'POTENTIAL_HIT');

-- Watchlist entry lookup: "how many transactions matched this watchlist entry?"
CREATE INDEX idx_wsr_watchlist_entry
    ON watchlist_screening_results (global_watchlist_id, screening_result);

