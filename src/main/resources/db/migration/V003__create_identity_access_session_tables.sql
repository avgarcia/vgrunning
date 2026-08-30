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

CREATE TABLE identity_access.access_session (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES identity_access.account (id),
    verifier_sha256 BYTEA NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    last_used_at TIMESTAMPTZ NOT NULL,
    absolute_expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    revocation_reason TEXT,
    CONSTRAINT access_session_verifier_sha256_length_check CHECK (octet_length(verifier_sha256) = 32),
    CONSTRAINT access_session_absolute_expiry_check CHECK (absolute_expires_at > created_at),
    CONSTRAINT access_session_revocation_check CHECK (
        (revoked_at IS NULL AND revocation_reason IS NULL)
        OR (revoked_at IS NOT NULL AND revocation_reason IN ('logout', 'expired', 'account_inactive'))
    )
);

CREATE UNIQUE INDEX access_session_verifier_sha256_key
    ON identity_access.access_session (verifier_sha256);

CREATE INDEX access_session_active_account_idx
    ON identity_access.access_session (account_id, last_used_at)
    WHERE revoked_at IS NULL;

CREATE TABLE identity_access.auth_rate_limit_bucket (
    bucket_type TEXT NOT NULL,
    key_hmac_sha256 BYTEA NOT NULL,
    window_started_at TIMESTAMPTZ NOT NULL,
    window_ends_at TIMESTAMPTZ NOT NULL,
    failure_count INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    purge_after TIMESTAMPTZ NOT NULL,
    CONSTRAINT auth_rate_limit_bucket_primary_key PRIMARY KEY (bucket_type, key_hmac_sha256, window_started_at),
    CONSTRAINT auth_rate_limit_bucket_type_check CHECK (bucket_type IN ('account_login_failure', 'ip_login_failure')),
    CONSTRAINT auth_rate_limit_bucket_hmac_length_check CHECK (octet_length(key_hmac_sha256) = 32),
    CONSTRAINT auth_rate_limit_bucket_count_check CHECK (failure_count >= 0),
    CONSTRAINT auth_rate_limit_bucket_window_check CHECK (window_ends_at > window_started_at),
    CONSTRAINT auth_rate_limit_bucket_purge_check CHECK (purge_after >= window_ends_at)
);

CREATE INDEX auth_rate_limit_bucket_purge_after_idx
    ON identity_access.auth_rate_limit_bucket (purge_after);

CREATE TABLE identity_access.security_event (
    id UUID PRIMARY KEY,
    occurred_at TIMESTAMPTZ NOT NULL,
    retention_until TIMESTAMPTZ NOT NULL,
    event_type TEXT NOT NULL,
    outcome TEXT NOT NULL,
    actor_class TEXT NOT NULL,
    actor_account_id UUID REFERENCES identity_access.account (id),
    affected_account_id UUID REFERENCES identity_access.account (id),
    access_session_id UUID,
    correlation_id UUID NOT NULL,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT security_event_type_check CHECK (event_type IN (
        'synthetic_account_provisioned',
        'session_created',
        'session_revoked',
        'session_expired',
        'login_rate_limited',
        'password_rehashed'
    )),
    CONSTRAINT security_event_outcome_check CHECK (outcome IN ('success', 'rejected', 'limited', 'automatic')),
    CONSTRAINT security_event_actor_class_check CHECK (actor_class IN ('anonymous', 'account', 'system')),
    CONSTRAINT security_event_metadata_object_check CHECK (jsonb_typeof(metadata) = 'object'),
    CONSTRAINT security_event_retention_check CHECK (retention_until > occurred_at),
    CONSTRAINT security_event_actor_check CHECK (
        (actor_class = 'account' AND actor_account_id IS NOT NULL)
        OR (actor_class IN ('anonymous', 'system') AND actor_account_id IS NULL)
    )
);

CREATE INDEX security_event_occurred_at_idx
    ON identity_access.security_event (occurred_at);

CREATE INDEX security_event_retention_until_idx
    ON identity_access.security_event (retention_until);
