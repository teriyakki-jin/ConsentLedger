CREATE TABLE data_holders (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_code VARCHAR(20)  NOT NULL UNIQUE,
    name             VARCHAR(255) NOT NULL,
    supported_methods JSONB       NOT NULL DEFAULT '[]',
    endpoint_url     VARCHAR(500),
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_data_holders_institution_code ON data_holders (institution_code);
CREATE INDEX idx_data_holders_supported_methods ON data_holders USING GIN (supported_methods);
