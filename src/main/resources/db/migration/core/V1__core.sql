-- Flyway runs this with search_path = core (quarkus.flyway.schemas), so names are unqualified.

CREATE TABLE project (
    id          UUID         PRIMARY KEY,
    slug        VARCHAR(40)  NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    -- Derived from the id so it can never come from user input (see docs/multitenancy.md).
    schema_name VARCHAR(34)  GENERATED ALWAYS AS ('p_' || replace(id::text, '-', '')) STORED UNIQUE,
    status      VARCHAR(20)  NOT NULL DEFAULT 'PROVISIONING',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT project_slug_format CHECK (slug ~ '^[a-z][a-z0-9-]{2,39}$'),
    CONSTRAINT project_status_valid CHECK (status IN ('PROVISIONING', 'ACTIVE', 'SUSPENDED'))
);

CREATE TABLE signing_key (
    id         UUID        PRIMARY KEY,
    project_id UUID        NOT NULL REFERENCES project (id),
    kid        VARCHAR(64) NOT NULL,
    algorithm  VARCHAR(10) NOT NULL,
    public_key TEXT        NOT NULL,
    status     VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    retire_at  TIMESTAMPTZ,
    CONSTRAINT signing_key_kid_key UNIQUE (project_id, kid),
    CONSTRAINT signing_key_status_valid CHECK (status IN ('ACTIVE', 'RETIRED', 'REVOKED'))
);

-- One signing key per project at a time; rotating means retiring the old one first.
CREATE UNIQUE INDEX signing_key_one_active ON signing_key (project_id) WHERE status = 'ACTIVE';
