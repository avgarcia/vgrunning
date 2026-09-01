package com.vgrunning.identityaccess.application.securityevent;

import com.vgrunning.identityaccess.application.model.SessionIdentity;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Evento semántico de acceso decidido antes de alcanzar la persistencia. */
public record SecurityEvent(
        UUID id,
        OffsetDateTime occurredAt,
        Type type,
        Outcome outcome,
        ActorClass actorClass,
        @Nullable UUID actorAccountId,
        @Nullable UUID affectedAccountId,
        @Nullable UUID sessionId,
        UUID correlationId) {

    public static SecurityEvent sessionCreated(
            SessionIdentity session, OffsetDateTime now, UUID correlationId) {
        return accountEvent(Type.SESSION_CREATED, Outcome.SUCCESS, session, now, correlationId);
    }

    public static SecurityEvent passwordRehashed(
            UUID accountId, OffsetDateTime now, UUID correlationId) {
        return new SecurityEvent(
                UUID.randomUUID(),
                now,
                Type.PASSWORD_REHASHED,
                Outcome.SUCCESS,
                ActorClass.ACCOUNT,
                accountId,
                accountId,
                null,
                correlationId);
    }

    public static SecurityEvent sessionRevoked(
            SessionIdentity session, OffsetDateTime now, UUID correlationId) {
        return accountEvent(Type.SESSION_REVOKED, Outcome.SUCCESS, session, now, correlationId);
    }

    public static SecurityEvent sessionExpired(
            SessionIdentity session, OffsetDateTime now, UUID correlationId) {
        return new SecurityEvent(
                UUID.randomUUID(),
                now,
                Type.SESSION_EXPIRED,
                Outcome.AUTOMATIC,
                ActorClass.SYSTEM,
                null,
                session.accountId(),
                session.sessionId(),
                correlationId);
    }

    public static SecurityEvent sessionRevokedBySystem(
            SessionIdentity session, OffsetDateTime now, UUID correlationId) {
        return new SecurityEvent(
                UUID.randomUUID(),
                now,
                Type.SESSION_REVOKED,
                Outcome.AUTOMATIC,
                ActorClass.SYSTEM,
                null,
                session.accountId(),
                session.sessionId(),
                correlationId);
    }

    public static SecurityEvent loginRateLimited(OffsetDateTime now, UUID correlationId) {
        return new SecurityEvent(
                UUID.randomUUID(),
                now,
                Type.LOGIN_RATE_LIMITED,
                Outcome.LIMITED,
                ActorClass.ANONYMOUS,
                null,
                null,
                null,
                correlationId);
    }

    public static SecurityEvent syntheticAccountProvisioned(
            UUID accountId, OffsetDateTime now, UUID correlationId) {
        return new SecurityEvent(
                UUID.randomUUID(),
                now,
                Type.SYNTHETIC_ACCOUNT_PROVISIONED,
                Outcome.SUCCESS,
                ActorClass.SYSTEM,
                null,
                accountId,
                null,
                correlationId);
    }

    private static SecurityEvent accountEvent(
            Type type,
            Outcome outcome,
            SessionIdentity session,
            OffsetDateTime now,
            UUID correlationId) {
        return new SecurityEvent(
                UUID.randomUUID(),
                now,
                type,
                outcome,
                ActorClass.ACCOUNT,
                session.accountId(),
                session.accountId(),
                session.sessionId(),
                correlationId);
    }

    public enum Type {
        SYNTHETIC_ACCOUNT_PROVISIONED("synthetic_account_provisioned"),
        SESSION_CREATED("session_created"),
        SESSION_REVOKED("session_revoked"),
        SESSION_EXPIRED("session_expired"),
        LOGIN_RATE_LIMITED("login_rate_limited"),
        PASSWORD_REHASHED("password_rehashed");

        private final String value;

        Type(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    public enum Outcome {
        SUCCESS("success"),
        LIMITED("limited"),
        AUTOMATIC("automatic");

        private final String value;

        Outcome(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    public enum ActorClass {
        ANONYMOUS("anonymous"),
        ACCOUNT("account"),
        SYSTEM("system");

        private final String value;

        ActorClass(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }
}
