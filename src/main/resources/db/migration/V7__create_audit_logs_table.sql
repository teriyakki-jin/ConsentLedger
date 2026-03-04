CREATE TABLE audit_logs (
    id             BIGSERIAL    PRIMARY KEY,
    ts             TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    action         VARCHAR(50)  NOT NULL,
    object_type    VARCHAR(50)  NOT NULL,
    object_id      UUID,
    actor_user_id  UUID         REFERENCES users (id) ON DELETE SET NULL,
    actor_agent_id UUID         REFERENCES agents (id) ON DELETE SET NULL,
    ip_address     VARCHAR(45),
    outcome        VARCHAR(20)  NOT NULL DEFAULT 'SUCCESS',
    payload_json   JSONB,
    payload_hash   VARCHAR(64)  NOT NULL,
    prev_hash      VARCHAR(64)  NOT NULL,
    hash           VARCHAR(64)  NOT NULL UNIQUE,
    schema_version INTEGER      NOT NULL DEFAULT 1,

    CONSTRAINT audit_logs_outcome_check CHECK (outcome IN ('SUCCESS', 'FAILURE'))
);

CREATE INDEX idx_audit_logs_ts ON audit_logs (ts);
CREATE INDEX idx_audit_logs_action ON audit_logs (action);
CREATE INDEX idx_audit_logs_object ON audit_logs (object_type, object_id);
CREATE INDEX idx_audit_logs_actor_user_id ON audit_logs (actor_user_id);
CREATE INDEX idx_audit_logs_actor_agent_id ON audit_logs (actor_agent_id);

-- append-only: UPDATE/DELETE 차단 트리거
CREATE OR REPLACE FUNCTION prevent_audit_modification()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'Audit logs are immutable. Modification is not allowed.';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER audit_logs_immutable
    BEFORE UPDATE OR DELETE ON audit_logs
    FOR EACH ROW EXECUTE FUNCTION prevent_audit_modification();
