package com.vgrunning.runnermanagement.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vgrunning.identityaccess.api.actor.ActorContext;
import com.vgrunning.identityaccess.api.provisioning.AccountProvisioningApi;
import com.vgrunning.identityaccess.api.provisioning.ProvisionRunnerAccount;
import com.vgrunning.identityaccess.api.provisioning.ProvisionedRunnerAccount;
import com.vgrunning.runnermanagement.application.exception.IdempotencyKeyReusedException;
import com.vgrunning.runnermanagement.application.exception.InvalidRunnerCreationException;
import com.vgrunning.runnermanagement.application.exception.RunnerCreationForbiddenException;
import com.vgrunning.runnermanagement.application.port.in.CreateRunnerUseCase.CreateRunner;
import com.vgrunning.runnermanagement.application.port.out.DigestPort;
import com.vgrunning.runnermanagement.application.port.out.RunnerCreationRepository;
import com.vgrunning.runnermanagement.domain.Runner;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Prueba el alta administrativa y la reserva idempotente antes de persistir perfiles. */
class CreateRunnerServiceTest {
    private static final UUID ADMIN_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
    private static final UUID KEY = UUID.fromString("30000000-0000-0000-0000-000000000002");

    @Test
    void createsAPendingRunnerForAnAdministrator() {
        AccountsFake accounts = new AccountsFake();
        RunnersFake runners = new RunnersFake();
        CreateRunnerService service = new CreateRunnerService(accounts, runners, digest());

        Runner result =
                service.create(
                        new ActorContext(ADMIN_ID, "administrador"),
                        KEY,
                        new CreateRunner(" Lucía ", " Martín ", " LUCIA@example.invalid ", true));

        assertThat(result.status()).isEqualTo("pending_activation");
        assertThat(accounts.command.email()).isEqualTo("lucia@example.invalid");
        assertThat(accounts.command.actor().accountId()).isEqualTo(ADMIN_ID);
        assertThat(runners.completed).isTrue();
    }

    @Test
    void refusesNonAdministratorsBeforeReservingAnything() {
        AccountsFake accounts = new AccountsFake();
        RunnersFake runners = new RunnersFake();
        CreateRunnerService service = new CreateRunnerService(accounts, runners, digest());

        assertThatThrownBy(
                        () ->
                                service.create(
                                        new ActorContext(ADMIN_ID, "corredor"),
                                        KEY,
                                        new CreateRunner(
                                                "Lucía", "Martín", "lucia@example.invalid", true)))
                .isInstanceOf(RunnerCreationForbiddenException.class);
        assertThat(runners.reserved).isFalse();
        assertThat(accounts.command).isNull();
    }

    @Test
    void replaysTheSameRequestWithoutProvisioningAnotherAccount() {
        AccountsFake accounts = new AccountsFake();
        RunnersFake runners = new RunnersFake(ReservationMode.REPLAY);
        CreateRunnerService service = new CreateRunnerService(accounts, runners, digest());

        Runner result =
                service.create(
                        new ActorContext(ADMIN_ID, "administrador"),
                        KEY,
                        new CreateRunner("Lucía", "Martín", "lucia@example.invalid", true));

        assertThat(result.id()).isEqualTo(RunnersFake.EXISTING.id());
        assertThat(accounts.command).isNull();
        assertThat(runners.completed).isFalse();
    }

    @Test
    void rejectsAnIdempotencyKeyUsedForAnotherRequest() {
        AccountsFake accounts = new AccountsFake();
        RunnersFake runners = new RunnersFake(ReservationMode.CONFLICT);
        CreateRunnerService service = new CreateRunnerService(accounts, runners, digest());

        assertThatThrownBy(
                        () ->
                                service.create(
                                        new ActorContext(ADMIN_ID, "administrador"),
                                        KEY,
                                        new CreateRunner(
                                                "Lucía", "Martín", "lucia@example.invalid", true)))
                .isInstanceOfSatisfying(
                        IdempotencyKeyReusedException.class,
                        exception ->
                                assertThat(exception.code()).isEqualTo("idempotency_key_reused"));
        assertThat(accounts.command).isNull();
    }

    @Test
    void rejectsInvalidRequestsBeforeReservingAnything() {
        AccountsFake accounts = new AccountsFake();
        RunnersFake runners = new RunnersFake();
        CreateRunnerService service = new CreateRunnerService(accounts, runners, digest());

        assertThatThrownBy(
                        () ->
                                service.create(
                                        new ActorContext(ADMIN_ID, "administrador"),
                                        KEY,
                                        new CreateRunner(
                                                "Lucía", "Martín", "lucia@example.invalid", false)))
                .isInstanceOf(InvalidRunnerCreationException.class);
        assertThatThrownBy(
                        () ->
                                service.create(
                                        new ActorContext(ADMIN_ID, "administrador"),
                                        KEY,
                                        new CreateRunner(
                                                " ", "Martín", "lucia@example.invalid", true)))
                .isInstanceOf(InvalidRunnerCreationException.class);
        assertThatThrownBy(
                        () ->
                                service.create(
                                        new ActorContext(ADMIN_ID, "administrador"),
                                        KEY,
                                        new CreateRunner(
                                                "Lucía", null, "lucia@example.invalid", true)))
                .isInstanceOfSatisfying(
                        InvalidRunnerCreationException.class,
                        exception -> assertThat(exception.code()).isEqualTo("invalid_request"));
        assertThat(runners.reserved).isFalse();
    }

    private static DigestPort digest() {
        return value -> {
            try {
                return MessageDigest.getInstance("SHA-256")
                        .digest(value.getBytes(StandardCharsets.UTF_8));
            } catch (java.security.NoSuchAlgorithmException exception) {
                throw new IllegalStateException(exception);
            }
        };
    }

    private static final class AccountsFake implements AccountProvisioningApi {
        private ProvisionRunnerAccount command;

        @Override
        public ProvisionedRunnerAccount provision(ProvisionRunnerAccount value) {
            command = value;
            return new ProvisionedRunnerAccount(
                    UUID.fromString("30000000-0000-0000-0000-000000000003"),
                    UUID.fromString("30000000-0000-0000-0000-000000000004"),
                    OffsetDateTime.now(ZoneOffset.UTC).plusDays(30));
        }
    }

    private enum ReservationMode {
        NEW,
        REPLAY,
        CONFLICT
    }

    private static final class RunnersFake implements RunnerCreationRepository {
        private static final Runner EXISTING =
                new Runner(
                        UUID.fromString("30000000-0000-0000-0000-000000000005"),
                        "Lucía",
                        "Martín",
                        "pending_activation");

        private final ReservationMode mode;
        private boolean reserved;
        private boolean completed;

        private RunnersFake() {
            this(ReservationMode.NEW);
        }

        private RunnersFake(ReservationMode mode) {
            this.mode = mode;
        }

        @Override
        public Optional<StoredCreation> reserve(
                UUID administratorId, UUID idempotencyKey, byte[] fingerprint) {
            reserved = true;
            return switch (mode) {
                case NEW -> Optional.empty();
                case REPLAY -> Optional.of(new StoredCreation(fingerprint, EXISTING));
                case CONFLICT -> Optional.of(new StoredCreation(new byte[] {0}, EXISTING));
            };
        }

        @Override
        public Runner create(NewRunner runner) {
            return new Runner(
                    runner.runnerId(),
                    runner.givenName(),
                    runner.familyName(),
                    "pending_activation");
        }

        @Override
        public void complete(UUID administratorId, UUID idempotencyKey, Runner runner) {
            completed = true;
        }
    }
}
