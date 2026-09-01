package com.vgrunning.identityaccess.adapter.out.persistence.jooq;

import static org.vgrunning.generated.jooq.identity_access.tables.AuthRateLimitBucket.AUTH_RATE_LIMIT_BUCKET;

import com.vgrunning.identityaccess.application.port.out.LoginRateLimitRepository;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

/** Adaptador jOOQ de los dos contadores atómicos de acceso. */
@Repository
@RequiredArgsConstructor
public class JooqLoginRateLimitRepository implements LoginRateLimitRepository {
    private static final String ACCOUNT_BUCKET = "account_login_failure";
    private static final String IP_BUCKET = "ip_login_failure";

    private final DSLContext jooq;

    @Override
    public FailureCounts currentFailures(
            byte[] accountKey, byte[] ipKey, OffsetDateTime windowStart) {
        return new FailureCounts(
                count(ACCOUNT_BUCKET, accountKey, windowStart),
                count(IP_BUCKET, ipKey, windowStart));
    }

    @Override
    public FailureCounts incrementFailures(
            byte[] accountKey,
            byte[] ipKey,
            OffsetDateTime windowStart,
            OffsetDateTime windowEnd,
            OffsetDateTime now) {
        return new FailureCounts(
                increment(ACCOUNT_BUCKET, accountKey, windowStart, windowEnd, now),
                increment(IP_BUCKET, ipKey, windowStart, windowEnd, now));
    }

    private int count(String bucketType, byte[] key, OffsetDateTime windowStart) {
        return jooq.select(AUTH_RATE_LIMIT_BUCKET.FAILURE_COUNT)
                .from(AUTH_RATE_LIMIT_BUCKET)
                .where(AUTH_RATE_LIMIT_BUCKET.BUCKET_TYPE.eq(bucketType))
                .and(AUTH_RATE_LIMIT_BUCKET.KEY_HMAC_SHA256.eq(key))
                .and(AUTH_RATE_LIMIT_BUCKET.WINDOW_STARTED_AT.eq(windowStart))
                .fetchOptional(AUTH_RATE_LIMIT_BUCKET.FAILURE_COUNT)
                .orElse(0);
    }

    private int increment(
            String bucketType,
            byte[] key,
            OffsetDateTime windowStart,
            OffsetDateTime windowEnd,
            OffsetDateTime now) {
        Integer failures =
                jooq.insertInto(AUTH_RATE_LIMIT_BUCKET)
                        .set(AUTH_RATE_LIMIT_BUCKET.BUCKET_TYPE, bucketType)
                        .set(AUTH_RATE_LIMIT_BUCKET.KEY_HMAC_SHA256, key)
                        .set(AUTH_RATE_LIMIT_BUCKET.WINDOW_STARTED_AT, windowStart)
                        .set(AUTH_RATE_LIMIT_BUCKET.WINDOW_ENDS_AT, windowEnd)
                        .set(AUTH_RATE_LIMIT_BUCKET.FAILURE_COUNT, 1)
                        .set(AUTH_RATE_LIMIT_BUCKET.CREATED_AT, now)
                        .set(AUTH_RATE_LIMIT_BUCKET.UPDATED_AT, now)
                        .set(AUTH_RATE_LIMIT_BUCKET.PURGE_AFTER, windowEnd.plusDays(1))
                        .onConflict(
                                AUTH_RATE_LIMIT_BUCKET.BUCKET_TYPE,
                                AUTH_RATE_LIMIT_BUCKET.KEY_HMAC_SHA256,
                                AUTH_RATE_LIMIT_BUCKET.WINDOW_STARTED_AT)
                        .doUpdate()
                        .set(
                                AUTH_RATE_LIMIT_BUCKET.FAILURE_COUNT,
                                AUTH_RATE_LIMIT_BUCKET.FAILURE_COUNT.plus(1))
                        .set(AUTH_RATE_LIMIT_BUCKET.UPDATED_AT, now)
                        .returningResult(AUTH_RATE_LIMIT_BUCKET.FAILURE_COUNT)
                        .fetchOne(AUTH_RATE_LIMIT_BUCKET.FAILURE_COUNT);
        if (failures == null) {
            throw new IllegalStateException("El contador de acceso no devolvió un valor.");
        }
        return failures;
    }
}
