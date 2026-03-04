CREATE TABLE agent_agreements (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    agent_id        UUID        NOT NULL REFERENCES agents (id) ON DELETE CASCADE,
    allowed_methods JSONB       NOT NULL DEFAULT '[]',
    valid_from      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    valid_until     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_agent_agreements_agent_id ON agent_agreements (agent_id);
CREATE INDEX idx_agent_agreements_allowed_methods ON agent_agreements USING GIN (allowed_methods);
