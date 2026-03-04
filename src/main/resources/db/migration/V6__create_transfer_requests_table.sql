CREATE TABLE transfer_requests (
    id                   UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    consent_id           UUID        NOT NULL REFERENCES consents (id) ON DELETE RESTRICT,
    method               VARCHAR(50)  NOT NULL,
    status               VARCHAR(20)  NOT NULL DEFAULT 'REQUESTED',
    requester_user_id    UUID        REFERENCES users (id) ON DELETE SET NULL,
    requester_agent_id   UUID        REFERENCES agents (id) ON DELETE SET NULL,
    approved_by_user_id  UUID        REFERENCES users (id) ON DELETE SET NULL,
    idempotency_key      VARCHAR(100) NOT NULL UNIQUE,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT transfer_requests_status_check
        CHECK (status IN ('REQUESTED', 'APPROVED', 'DENIED', 'COMPLETED', 'FAILED')),
    CONSTRAINT transfer_requests_requester_check
        CHECK (requester_user_id IS NOT NULL OR requester_agent_id IS NOT NULL)
);

CREATE INDEX idx_transfer_requests_consent_id ON transfer_requests (consent_id);
CREATE INDEX idx_transfer_requests_status ON transfer_requests (status);
CREATE INDEX idx_transfer_requests_requester_user_id ON transfer_requests (requester_user_id);
CREATE INDEX idx_transfer_requests_requester_agent_id ON transfer_requests (requester_agent_id);
CREATE UNIQUE INDEX idx_transfer_requests_idempotency_key ON transfer_requests (idempotency_key);
