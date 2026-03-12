-- ============================================================
-- AdLex V1: Initial Schema
-- ============================================================

CREATE TABLE tenants (
    id            BIGSERIAL    PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    company_name  VARCHAR(100),
    plan          VARCHAR(20)  NOT NULL DEFAULT 'FREE',
    monthly_quota INTEGER      NOT NULL DEFAULT 100,
    monthly_used  INTEGER      NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE api_keys (
    id           BIGSERIAL    PRIMARY KEY,
    tenant_id    BIGINT       NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name         VARCHAR(50)  NOT NULL,
    key_hash     VARCHAR(64)  NOT NULL UNIQUE,
    key_prefix   VARCHAR(12)  NOT NULL,
    status       VARCHAR(10)  NOT NULL DEFAULT 'ACTIVE',
    last_used_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE rules (
    id          BIGSERIAL    PRIMARY KEY,
    code        VARCHAR(50)  NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    type        VARCHAR(20)  NOT NULL,
    channel     VARCHAR(50)  NOT NULL, -- CSV: 'SMS', 'SMS,KAKAO' 등
    severity    VARCHAR(10)  NOT NULL,
    pattern     TEXT,
    config      JSONB,       -- {"matchMode": "REQUIRE"} 등
    legal_basis TEXT,
    active      BOOLEAN      NOT NULL DEFAULT true,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- TODO: 데이터 증가 시 PARTITION BY RANGE(created_at) 월별 파티셔닝 추가 예정
CREATE TABLE check_logs (
    id              BIGSERIAL   PRIMARY KEY,
    tenant_id       BIGINT      NOT NULL REFERENCES tenants(id),
    message         TEXT        NOT NULL,
    channel         VARCHAR(10) NOT NULL,
    compliant       BOOLEAN     NOT NULL,
    violation_count INTEGER     NOT NULL DEFAULT 0,
    violations      JSONB       NOT NULL DEFAULT '[]',
    processing_ms   BIGINT      NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Indexes
CREATE INDEX idx_api_keys_hash ON api_keys(key_hash);
CREATE INDEX idx_api_keys_tenant ON api_keys(tenant_id);
CREATE INDEX idx_check_logs_tenant_created ON check_logs(tenant_id, created_at);
CREATE INDEX idx_rules_active ON rules(active) WHERE active = true;
