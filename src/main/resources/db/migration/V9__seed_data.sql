-- 테스트용 ADMIN 사용자 (password: admin1234)
INSERT INTO users (id, email, password_hash, role)
VALUES (
    'a0000000-0000-0000-0000-000000000001',
    'admin@consentledger.com',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'ADMIN'
);

-- 테스트용 일반 사용자 (password: user1234)
INSERT INTO users (id, email, password_hash, role)
VALUES (
    'a0000000-0000-0000-0000-000000000002',
    'user@consentledger.com',
    '$2a$10$8K1p/a0dclxJDHR3JhbpmeOwHqDaEi1JMcHoNDHXqGgp1XGzKREYG',
    'USER'
);

-- 테스트용 Agent
INSERT INTO agents (id, name, status, client_id, api_key_hash)
VALUES (
    'b0000000-0000-0000-0000-000000000001',
    'MyData Agent A',
    'ACTIVE',
    'agent-client-001',
    -- api_key: 'test-api-key-agent-a' -> SHA-256 hash
    'c2d2b1a73f9e5e8be6ef07bd0c7e64d24b0f6e3e7a0c5e7a9f8b5d3c1e2f4a6b'
);

-- Agent 협약
INSERT INTO agent_agreements (agent_id, allowed_methods, valid_from)
VALUES (
    'b0000000-0000-0000-0000-000000000001',
    '["PUSH", "PULL"]',
    NOW()
);

-- 테스트용 DataHolder (은행)
INSERT INTO data_holders (id, institution_code, name, supported_methods, endpoint_url)
VALUES
    (
        'c0000000-0000-0000-0000-000000000001',
        'BANK001',
        '한국은행',
        '["PUSH", "PULL"]',
        'https://api.bank001.example.com/transfer'
    ),
    (
        'c0000000-0000-0000-0000-000000000002',
        'BANK002',
        '서울저축은행',
        '["PULL"]',
        'https://api.bank002.example.com/transfer'
    );
