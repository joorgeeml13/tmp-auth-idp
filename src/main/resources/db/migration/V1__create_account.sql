CREATE TABLE account (
    version        BIGINT       NOT NULL DEFAULT 0,
    id             UUID         PRIMARY KEY,
    email          VARCHAR(255) NOT NULL,
    email_verified BOOLEAN      NOT NULL DEFAULT false,
    phone_number   VARCHAR(20)  UNIQUE,
    phone_verified BOOLEAN      NOT NULL DEFAULT false,
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    locked_until   TIMESTAMPTZ,
    token_version  INTEGER      NOT NULL DEFAULT 1 CHECK (token_version >= 1),
    mfa_enabled    BOOLEAN      NOT NULL DEFAULT false,
    locale         VARCHAR(10)  NOT NULL DEFAULT 'es-ES',
    zoneinfo       VARCHAR(50)  NOT NULL DEFAULT 'Europe/Madrid',
    last_login     TIMESTAMPTZ,
    last_login_ip  INET,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at     TIMESTAMPTZ,
    CONSTRAINT account_email_lowercase CHECK (email = lower(email))
);

CREATE UNIQUE INDEX account_email_key ON account (lower(email));
