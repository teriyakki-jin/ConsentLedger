CREATE TABLE agents (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    client_id       VARCHAR(100) NOT NULL UNIQUE,
    api_key_hash    VARCHAR(64)  NOT NULL,
    last_rotated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT agents_status_check CHECK (status IN ('ACTIVE', 'SUSPENDED', 'REVOKED'))
);

CREATE INDEX idx_agents_client_id ON agents (client_id);
CREATE INDEX idx_agents_status ON agents (status);
