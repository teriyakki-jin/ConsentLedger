CREATE TABLE consents (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    agent_id       UUID        REFERENCES agents (id) ON DELETE SET NULL,
    data_holder_id UUID        NOT NULL REFERENCES data_holders (id) ON DELETE RESTRICT,
    scopes         JSONB       NOT NULL DEFAULT '[]',
    status         VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    expires_at     TIMESTAMPTZ,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT consents_status_check CHECK (status IN ('ACTIVE', 'REVOKED', 'EXPIRED'))
);

CREATE INDEX idx_consents_user_id ON consents (user_id);
CREATE INDEX idx_consents_agent_id ON consents (agent_id);
CREATE INDEX idx_consents_data_holder_id ON consents (data_holder_id);
CREATE INDEX idx_consents_status ON consents (status);
CREATE INDEX idx_consents_scopes ON consents USING GIN (scopes);
