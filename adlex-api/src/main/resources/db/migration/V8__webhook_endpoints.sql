-- ============================================================
-- AdLex V8: Webhook Endpoints
-- ============================================================

CREATE TABLE webhook_endpoints (
    id         BIGSERIAL     PRIMARY KEY,
    tenant_id  BIGINT        NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    url        VARCHAR(2048) NOT NULL,
    secret     VARCHAR(64)   NOT NULL,
    active     BOOLEAN       NOT NULL DEFAULT true,
    events     VARCHAR(100)  NOT NULL DEFAULT 'check.completed',
    created_at TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_webhook_endpoints_tenant ON webhook_endpoints(tenant_id);
CREATE INDEX idx_webhook_endpoints_active ON webhook_endpoints(tenant_id, active) WHERE active = true;
