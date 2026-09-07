CREATE TABLE identity_access.account (
    id UUID PRIMARY KEY,
    role TEXT NOT NULL,
    status TEXT NOT NULL,
    password_hash TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    status_changed_at TIMESTAMPTZ NOT NULL,
    password_changed_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT account_role_check CHECK (role IN ('administrador', 'entrenador', 'corredor')),
    CONSTRAINT account_status_check CHECK (status IN ('pending_activation', 'active', 'disabled', 'pending_reactivation', 'cancelled')),
    CONSTRAINT account_version_check CHECK (version >= 0)
);

CREATE TABLE identity_access.account_email (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES identity_access.account (id),
    presentation_email TEXT NOT NULL,
    canonical_email TEXT NOT NULL,
    usage TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    confirmed_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    released_at TIMESTAMPTZ,
    CONSTRAINT account_email_usage_check CHECK (usage IN ('current', 'pending_change')),
    CONSTRAINT account_email_canonical_not_blank_check CHECK (btrim(canonical_email) <> '')
);

CREATE UNIQUE INDEX account_email_live_canonical_usage_key
    ON identity_access.account_email (canonical_email)
    WHERE usage IN ('current', 'pending_change') AND released_at IS NULL;

CREATE UNIQUE INDEX account_email_account_usage_reservation_key
    ON identity_access.account_email (account_id, usage)
    WHERE released_at IS NULL;

CREATE TABLE identity_access.spring_session (
    primary_id CHAR(36) NOT NULL,
    session_id CHAR(36) NOT NULL,
    creation_time BIGINT NOT NULL,
    last_access_time BIGINT NOT NULL,
    max_inactive_interval INTEGER NOT NULL,
    expiry_time BIGINT NOT NULL,
    principal_name VARCHAR(100),
    CONSTRAINT spring_session_pk PRIMARY KEY (primary_id)
);

CREATE UNIQUE INDEX spring_session_ix1 ON identity_access.spring_session (session_id);
CREATE INDEX spring_session_ix2 ON identity_access.spring_session (expiry_time);
CREATE INDEX spring_session_ix3 ON identity_access.spring_session (principal_name);

CREATE TABLE identity_access.spring_session_attributes (
    session_primary_id CHAR(36) NOT NULL,
    attribute_name VARCHAR(200) NOT NULL,
    attribute_bytes BYTEA NOT NULL,
    CONSTRAINT spring_session_attributes_pk PRIMARY KEY (session_primary_id, attribute_name),
    CONSTRAINT spring_session_attributes_fk FOREIGN KEY (session_primary_id)
        REFERENCES identity_access.spring_session (primary_id) ON DELETE CASCADE
);
