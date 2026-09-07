package com.vgrunning.runnermanagement.application.service;

import com.vgrunning.identityaccess.api.actor.ActorContext;
import com.vgrunning.identityaccess.api.provisioning.AccountProvisioningApi;
import com.vgrunning.identityaccess.api.provisioning.ProvisionRunnerAccount;
import com.vgrunning.identityaccess.api.provisioning.ProvisionedRunnerAccount;
import com.vgrunning.runnermanagement.application.exception.IdempotencyKeyReusedException;
import com.vgrunning.runnermanagement.application.exception.InvalidRunnerCreationException;
import com.vgrunning.runnermanagement.application.exception.RunnerCreationForbiddenException;
import com.vgrunning.runnermanagement.application.mapper.RunnerCreationMapper;
import com.vgrunning.runnermanagement.application.port.in.CreateRunnerUseCase;
import com.vgrunning.runnermanagement.application.port.out.RunnerCreationRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

/** Coordina el alta atómica sin filtrar HTTP o SQL a la aplicación. */
@RequiredArgsConstructor
public class CreateRunnerService implements CreateRunnerUseCase {
    private final AccountProvisioningApi accounts;
    private final RunnerCreationRepository runners;
    private final RunnerCreationMapper mapper;

    @Override
    @Transactional
    public CreatedRunner create(ActorContext actor, UUID idempotencyKey, CreateRunner command) {
        if (!actor.isAdministrator()) {
            throw new RunnerCreationForbiddenException();
        }
        CreateRunner normalized = normalize(command);
        byte[] fingerprint = fingerprint(normalized);
        RunnerCreationRepository.Reservation reservation =
                runners.reserve(actor.accountId(), idempotencyKey, fingerprint);
        return reservation
                .existing()
                .map(existing -> replay(existing, fingerprint))
                .orElseGet(() -> createNew(actor, idempotencyKey, normalized));
    }

    private CreatedRunner createNew(ActorContext actor, UUID idempotencyKey, CreateRunner command) {
        UUID runnerId = UUID.randomUUID();
        UUID correlationId = UUID.randomUUID();
        ProvisionedRunnerAccount account =
                accounts.provision(
                        new ProvisionRunnerAccount(command.email(), actor, correlationId));
        var stored =
                runners.create(
                        new RunnerCreationRepository.NewRunner(
                                runnerId,
                                account.accountId(),
                                command.givenName(),
                                command.familyName(),
                                actor.accountId(),
                                correlationId,
                                account.activationExpiresAt()));
        runners.complete(actor.accountId(), idempotencyKey, stored);
        return mapper.toCreatedRunner(stored);
    }

    private CreatedRunner replay(
            RunnerCreationRepository.StoredCreation existing, byte[] fingerprint) {
        if (!MessageDigest.isEqual(existing.fingerprint(), fingerprint)) {
            throw new IdempotencyKeyReusedException();
        }
        return mapper.toCreatedRunner(existing.runner());
    }

    private static CreateRunner normalize(CreateRunner command) {
        if (!Boolean.TRUE.equals(command.adultDeclarationConfirmed())) {
            throw new InvalidRunnerCreationException();
        }
        return new CreateRunner(
                normalizedText(command.givenName()),
                normalizedText(command.familyName()),
                normalizedText(command.email()).toLowerCase(java.util.Locale.ROOT),
                true);
    }

    private static String normalizedText(String value) {
        if (value == null) {
            throw new InvalidRunnerCreationException();
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFC).trim();
        if (normalized.isEmpty()) {
            throw new InvalidRunnerCreationException();
        }
        return normalized;
    }

    private static byte[] fingerprint(CreateRunner command) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(
                            (command.givenName()
                                            + "\n"
                                            + command.familyName()
                                            + "\n"
                                            + command.email())
                                    .getBytes(StandardCharsets.UTF_8));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
