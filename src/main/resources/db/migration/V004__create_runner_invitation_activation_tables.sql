CREATE TABLE identity_access.access_challenge (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES identity_access.account (id),
    purpose TEXT NOT NULL,
    generation INTEGER NOT NULL,
    verifier_sha256 BYTEA NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    replaced_at TIMESTAMPTZ,
    CONSTRAINT access_challenge_purpose_check
        CHECK (purpose IN ('activation', 'reactivation', 'password_recovery', 'email_change')),
    CONSTRAINT access_challenge_generation_check CHECK (generation > 0),
    CONSTRAINT access_challenge_expiry_check CHECK (expires_at > created_at)
);

CREATE UNIQUE INDEX access_challenge_current_generation_key
    ON identity_access.access_challenge (account_id, purpose)
    WHERE consumed_at IS NULL AND replaced_at IS NULL;

CREATE TABLE identity_access.adult_declaration (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES identity_access.account (id),
    actor_kind TEXT NOT NULL,
    actor_account_id UUID REFERENCES identity_access.account (id),
    origin TEXT NOT NULL,
    declared_at TIMESTAMPTZ NOT NULL,
    text_version TEXT NOT NULL,
    CONSTRAINT adult_declaration_actor_kind_check CHECK (actor_kind IN ('administrator', 'invitee')),
    CONSTRAINT adult_declaration_origin_check
        CHECK (origin IN ('administrative_invitation', 'initial_activation')),
    CONSTRAINT adult_declaration_actor_check CHECK (
        (actor_kind = 'administrator' AND actor_account_id IS NOT NULL)
        OR (actor_kind = 'invitee' AND actor_account_id IS NULL)
    ),
    CONSTRAINT adult_declaration_text_version_not_blank_check CHECK (btrim(text_version) <> ''),
    CONSTRAINT adult_declaration_account_origin_key UNIQUE (account_id, origin)
);

CREATE TABLE identity_access.invitation_acceptance (
    id UUID PRIMARY KEY,
    challenge_id UUID NOT NULL UNIQUE REFERENCES identity_access.access_challenge (id),
    account_id UUID NOT NULL REFERENCES identity_access.account (id),
    accepted_at TIMESTAMPTZ NOT NULL,
    correlation_id UUID NOT NULL
);

CREATE TABLE runner_management.runner (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL UNIQUE REFERENCES identity_access.account (id),
    given_name TEXT NOT NULL,
    family_name TEXT NOT NULL,
    status TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    pending_activation_expires_at TIMESTAMPTZ NOT NULL,
    activated_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT runner_status_check CHECK (status IN ('pending_activation', 'active', 'inactive', 'pending_reactivation', 'cancelled')),
    CONSTRAINT runner_given_name_not_blank_check CHECK (btrim(given_name) <> ''),
    CONSTRAINT runner_family_name_not_blank_check CHECK (btrim(family_name) <> ''),
    CONSTRAINT runner_pending_activation_expiry_check CHECK (pending_activation_expires_at > created_at),
    CONSTRAINT runner_version_check CHECK (version >= 0)
);

CREATE INDEX runner_pending_activation_expiry_ix
    ON runner_management.runner (pending_activation_expires_at)
    WHERE status = 'pending_activation';

CREATE TABLE runner_management.runner_creation_idempotency (
    administrator_account_id UUID NOT NULL REFERENCES identity_access.account (id),
    idempotency_key UUID NOT NULL,
    request_fingerprint BYTEA NOT NULL,
    runner_id UUID REFERENCES runner_management.runner (id),
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (administrator_account_id, idempotency_key),
    CONSTRAINT runner_creation_idempotency_expiry_check CHECK (expires_at > created_at)
);

CREATE INDEX runner_creation_idempotency_expiry_ix
    ON runner_management.runner_creation_idempotency (expires_at);

CREATE TABLE runner_management.runner_lifecycle_audit (
    id UUID PRIMARY KEY,
    runner_id UUID NOT NULL REFERENCES runner_management.runner (id),
    actor_kind TEXT NOT NULL,
    actor_account_id UUID REFERENCES identity_access.account (id),
    transition TEXT NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    correlation_id UUID NOT NULL,
    CONSTRAINT runner_lifecycle_audit_transition_check CHECK (transition IN ('created', 'activated')),
    CONSTRAINT runner_lifecycle_audit_actor_kind_check CHECK (actor_kind IN ('administrator', 'system')),
    CONSTRAINT runner_lifecycle_audit_actor_check CHECK (
        (actor_kind = 'administrator' AND actor_account_id IS NOT NULL)
        OR (actor_kind = 'system' AND actor_account_id IS NULL)
    )
);

CREATE TABLE notification_delivery.notification_request (
    id UUID PRIMARY KEY,
    logical_key TEXT NOT NULL UNIQUE,
    origin_type TEXT NOT NULL,
    origin_id UUID NOT NULL,
    status TEXT NOT NULL,
    destination_key_id TEXT NOT NULL,
    destination_nonce BYTEA NOT NULL,
    destination_ciphertext BYTEA NOT NULL,
    payload_key_id TEXT NOT NULL,
    payload_nonce BYTEA NOT NULL,
    payload_ciphertext BYTEA NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    correlation_id UUID NOT NULL,
    CONSTRAINT notification_request_origin_type_check CHECK (origin_type = 'invitation'),
    CONSTRAINT notification_request_status_check CHECK (status = 'pending'),
    CONSTRAINT notification_request_key_id_not_blank_check
        CHECK (btrim(destination_key_id) <> '' AND btrim(payload_key_id) <> '')
);
