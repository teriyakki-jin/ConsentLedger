CREATE TABLE audit_chain_anchor (
    id          INTEGER     PRIMARY KEY,
    last_log_id BIGINT      NOT NULL DEFAULT 0,
    last_hash   VARCHAR(64) NOT NULL,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT audit_chain_anchor_single_row CHECK (id = 1)
);

-- 초기 genesis 행 삽입
INSERT INTO audit_chain_anchor (id, last_log_id, last_hash)
VALUES (1, 0, '0000000000000000000000000000000000000000000000000000000000000000');
